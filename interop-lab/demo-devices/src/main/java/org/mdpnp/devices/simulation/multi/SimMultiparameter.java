/*******************************************************************************
 * Copyright (c) 2014, MD PnP Program
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.mdpnp.devices.simulation.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.Subscriber;
import ice.ConnectionState;
import ice.GlobalSimulationObjective;
import org.java_websocket.client.WebSocketClient;
import org.mdpnp.common.graph.CommonGraphModal;
import org.mdpnp.common.graph.ECGGraphModal;
import org.mdpnp.devices.DeviceClock;
import org.mdpnp.devices.DeviceClock.Reading;
import org.mdpnp.devices.simulation.AbstractSimulatedConnectedDevice;
import org.mdpnp.devices.simulation.GlobalSimulationObjectiveListener;
import org.mdpnp.devices.simulation.co2.SimulatedCapnometer;
import org.mdpnp.devices.simulation.ecg.SimulatedElectroCardioGram;
import org.mdpnp.devices.simulation.ibp.SimulatedInvasiveBloodPressure;
import org.mdpnp.devices.simulation.pulseox.SimulatedPulseOximeter;
import org.mdpnp.rtiapi.data.EventLoop;
import org.mdpnp.websocket.AuthenticateSocket;
import org.mdpnp.websocket.CommandLineModel;
import org.mdpnp.websocket.DeviceInfoUtils;
import org.mdpnp.websocket.WebsocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.*;

/**
 * @author Jeff Plourde
 *
 */
public class SimMultiparameter extends AbstractSimulatedConnectedDevice {
    
    private static final Logger log = LoggerFactory.getLogger(SimMultiparameter.class);

    protected final InstanceHolder<ice.Numeric> pulse, SpO2, respiratoryRate, etCO2, ecgRespiratoryRate, heartRate, systolic, diastolic;
    protected InstanceHolder<ice.SampleArray> pleth, co2, i, ii, iii, pressure;

    private OpenIceJsonModal jsonModal;
    private GraphModal grpModel;
    private String token;
    private Map<String,Object> multiParameterGraph;
    //public WebSocketClient mWs;

    private List<Float> plethUserDataList=new ArrayList<>();
    private List<Float> pressureUserDataList=new ArrayList<>();
    private List<Float> co2UserDataList=new ArrayList<>();
    private List<Float> ecgGaUserDataList=new ArrayList<>();
    private List<Float> ecgGbUserDataList=new ArrayList<>();
    private List<Float> ecgGcUserDataList=new ArrayList<>();

    private class SimulatedPulseOximeterExt extends SimulatedPulseOximeter {

        public SimulatedPulseOximeterExt(DeviceClock referenceClock) {
            super(referenceClock);
        }

        @Override
        protected void receivePulseOx(Reading sampleTime, int heartRate, int SpO2, Number[] plethValues, int frequency) {

            numericSample(pulse, heartRate, sampleTime);
            numericSample(SimMultiparameter.this.SpO2, SpO2, sampleTime);
            pleth = sampleArraySample(pleth, plethValues, rosetta.MDC_PULS_OXIM_PLETH.VALUE, "", 0, 
                    rosetta.MDC_DIM_DIMLESS.VALUE, frequency, sampleTime);


            /********** Updated Code *************/

            plethUserDataList.addAll(pleth.data.values.userData);

          }
    }
    
    private class SimulatedInvasiveBloodPressureExt extends SimulatedInvasiveBloodPressure {
        public SimulatedInvasiveBloodPressureExt(DeviceClock referenceClock) {  
            super(referenceClock);
        }
        
        @Override
        protected void receivePressure(Reading sampleTime, int systolic, int diastolic, Number[] waveValues, int frequency) {
            numericSample(SimMultiparameter.this.systolic, systolic, sampleTime);
            numericSample(SimMultiparameter.this.diastolic, diastolic, sampleTime);
            pressure = sampleArraySample(pressure, waveValues, rosetta.MDC_PRESS_BLD_ART_ABP.VALUE, "", 0,
                    rosetta.MDC_DIM_DIMLESS.VALUE, frequency, sampleTime);

            /********** Updated Code *************/

            pressureUserDataList.addAll(pressure.data.values.userData);

           }
        
    }
    
    private class SimulatedCapnometerExt extends SimulatedCapnometer {

        public SimulatedCapnometerExt(DeviceClock referenceClock) {
            super(referenceClock);
        }

        @Override
        protected void receiveCO2(Reading sampleTime, Number[] co2Values, int respiratoryRateValue, int etCO2Value, int frequency) {

            co2 = sampleArraySample(co2, co2Values, rosetta.MDC_AWAY_CO2.VALUE, "", 0, 
                    rosetta.MDC_DIM_MMHG.VALUE, frequency, sampleTime);
            numericSample(respiratoryRate, respiratoryRateValue, sampleTime);
            numericSample(etCO2, etCO2Value, sampleTime);

            /********** Updated Code *************/

            co2UserDataList.addAll(co2.data.values.userData);

        }
    }
    
    private class SimulatedElectroCardioGramExt extends SimulatedElectroCardioGram {

        public SimulatedElectroCardioGramExt(DeviceClock referenceClock) {
            super(referenceClock);
        }

        @Override
        protected void receiveECG(Reading sampleTime, Number[] iValues, Number[] iiValues, Number[] iiiValues,
                                  int heartRateValue, int respiratoryRateValue, int frequency) {

            try {
                // TODO get better numbers in actual millivolts
                i = sampleArraySample(i, iValues, ice.MDC_ECG_LEAD_I.VALUE, "", 0, 
                        rosetta.MDC_DIM_DIMLESS.VALUE, frequency, sampleTime);
                ii = sampleArraySample(ii, iiValues, ice.MDC_ECG_LEAD_II.VALUE, "", 0, 
                        rosetta.MDC_DIM_DIMLESS.VALUE, frequency, sampleTime);
                iii = sampleArraySample(iii, iiiValues, ice.MDC_ECG_LEAD_III.VALUE, "", 0, 
                        rosetta.MDC_DIM_DIMLESS.VALUE, frequency, sampleTime);


                numericSample(heartRate, (float) heartRateValue, sampleTime);
                numericSample(ecgRespiratoryRate, (float) respiratoryRateValue, sampleTime);


                /***** Updted code *********/

                ecgGaUserDataList.addAll(i.data.values.userData);
                ecgGbUserDataList.addAll(ii.data.values.userData);
                ecgGcUserDataList.addAll(iii.data.values.userData);

             } catch (Throwable t) {
                log.error("Error simulating ECG data", t);
            }
        }
    }

    private final SimulatedElectroCardioGram ecg;
    private final SimulatedPulseOximeter pulseox;
    private final SimulatedCapnometer capnometer;
    private final SimulatedInvasiveBloodPressure ibp;

    @Override
    public boolean connect(String str) {
        pulseox.connect(executor);
        capnometer.connect(executor);
        ecg.connect(executor);
        ibp.connect(executor);
        return super.connect(str);
    }

    @Override
    public void disconnect() {
        pulseox.disconnect();
        capnometer.disconnect();
        ecg.disconnect();
        ibp.disconnect();
        super.disconnect();
    }

    WebsocketUtils wsu;
    WebSocketClient connect;

    String deviceID = null;
    String devicePWD=null;

    public SimMultiparameter(final Subscriber subscriber, final Publisher publisher, EventLoop eventLoop) {
        super(subscriber, publisher, eventLoop);

        DeviceClock referenceClock = getClockProvider();

        ecg = new SimulatedElectroCardioGramExt(referenceClock);
        pulseox = new SimulatedPulseOximeterExt(referenceClock);
        capnometer = new SimulatedCapnometerExt(referenceClock);
        ibp = new SimulatedInvasiveBloodPressureExt(referenceClock);

        pulse = createNumericInstance(rosetta.MDC_PULS_OXIM_PULS_RATE.VALUE, "");
        SpO2 = createNumericInstance(rosetta.MDC_PULS_OXIM_SAT_O2.VALUE, "");
        
        respiratoryRate = createNumericInstance(rosetta.MDC_CO2_RESP_RATE.VALUE, "");
        etCO2 = createNumericInstance(rosetta.MDC_AWAY_CO2_ET.VALUE, "");

        ecgRespiratoryRate = createNumericInstance(rosetta.MDC_TTHOR_RESP_RATE.VALUE, "");
        heartRate = createNumericInstance(rosetta.MDC_ECG_HEART_RATE.VALUE, "");
        
        systolic = createNumericInstance(rosetta.MDC_PRESS_BLD_ART_ABP_SYS.VALUE, "");
        diastolic = createNumericInstance(rosetta.MDC_PRESS_BLD_ART_ABP_DIA.VALUE, "");

        deviceIdentity.model = "Multiparameter (Simulated)";
        writeDeviceIdentity();




        if(!CommandLineModel.getDeviceId().isEmpty()&&!CommandLineModel.getDevicePassword().isEmpty()) {

            /************ CommandLine input **************/

            deviceID = CommandLineModel.getDeviceId();
            devicePWD = CommandLineModel.getDevicePassword();

        }else{

            /**************** UI input********************/

            DeviceInfoUtils info=new DeviceInfoUtils();
            HashMap<String, String> values = info.createDeviceInfoPanel();

            deviceID=values.get("id");
            devicePWD=values.get("password");

        }

        System.out.println("ID  :"+deviceID);
        System.out.println("Password  :"+devicePWD);




        /************ Generate Token  ****************/

        AuthenticateSocket as=new AuthenticateSocket();
        token=as.generateTokenByDeviceIdAndPassword(deviceID,devicePWD);

        if(token==null){

            System.out.println("Device authentication failed...!");
            UIManager.put("OptionPane.minimumSize",new Dimension(300,150));
            UIManager.put("OptionPane.messageFont", new Font("Arial", Font.BOLD, 14));
            JOptionPane.showMessageDialog(null,"Device authentication failed...!");
            System.exit(0);
        }



        /************ updated code **************/
    try {

        //load properties value
        Properties prop = new Properties();
        String propFileName = "websocket-info.properties";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
        if (inputStream != null) {
            prop.load(inputStream);
        }



        String url = prop.getProperty("ws-write-url");
        String wrtUrl=url+"/ws/data/"+deviceID+"/write/";
        System.out.println(wrtUrl);
        log.info(wrtUrl);

        //connect to webSocket
        wsu=new WebsocketUtils();
        connect = wsu.connectWebsocket(wrtUrl+token+"/");


        multiParameterGraph=new LinkedHashMap<>();

        jsonModal=new OpenIceJsonModal();
        grpModel=new GraphModal();
        jsonModal.setDev(deviceIdentity.unique_device_identifier);
        jsonModal.setDtp(deviceIdentity.model);
        jsonModal.setCon("");

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                   if(ConnectionState.Terminal.equals(getState())){
                       timer.cancel();
                       timer.purge();
                       System.out.println("Timer terminated ");
                       return;
                   }

                    if(ConnectionState.Connected.equals(getState())) {

                        Statistics sts = new Statistics();
                        sts.setBpm(pulse.data.value);
                        sts.setSpo(SpO2.data.value);
                        sts.setHr(heartRate.data.value);
                        sts.setRre(respiratoryRate.data.value);
                        sts.setRrc(ecgRespiratoryRate.data.value);
                        sts.setEtc(etCO2.data.value);
                        sts.setSys(systolic.data.value);
                        sts.setDia(diastolic.data.value);
                        sts.setRst(false);
                        jsonModal.setSts(sts);
                       // jsonModal.setTs(System.currentTimeMillis());

                        String out = receiveGraphData();
                        //System.out.println(out);

                        /************ send json to websocket*******/
                        if (connect.isOpen()) {
                            wsu.sendMessageToWebsocket(out);

                            // clear data list
                            plethUserDataList.clear();
                            pressureUserDataList.clear();
                            co2UserDataList.clear();
                            ecgGaUserDataList.clear();
                            ecgGbUserDataList.clear();
                            ecgGcUserDataList.clear();

                           }else if(connect.isClosing()) {
                                System.out.println("Closing Connection....");
                                log.info("Closing Connection....");
                            }else if(connect.isClosed()){
                                System.out.println("Re-Connecting....WebSocket....");
                                log.info("Re-Connecting....WebSocket....");
                                //connect to webSocket
                                token=as.generateTokenByDeviceIdAndPassword(deviceID,devicePWD);
                                if(token!=null){
                                    wsu=new WebsocketUtils();
                                    connect =wsu.connectWebsocket(wrtUrl+token+"/");
                                }
                            }else{
                                connect.close();
                            }
                    }

                }
            }, 0, 2000);

        }catch (Exception e){

            e.printStackTrace();
        }

        /**************************/
       }

    @Override
    protected String iconResourceName() {
        return "multi.png";
    }
    
    

    @Override
    public void simulatedNumeric(GlobalSimulationObjective obj) {
        // Currently the super ctor registers for this callback; so pulseox might not yet be initialized
        if (obj != null && pulseox != null && capnometer != null && ecg != null && pressure != null) {
            Number value = GlobalSimulationObjectiveListener.toIntegerNumber(obj);
            if(rosetta.MDC_PULS_RATE.VALUE.equals(obj.metric_id)) {
                ecg.setTargetHeartRate(value);
                pulseox.setTargetHeartRate(value);
            } else if(rosetta.MDC_RESP_RATE.VALUE.equals(obj.metric_id)) {
                ecg.setTargetRespiratoryRate(value);
                capnometer.setRespirationRate(value);
            } else if(rosetta.MDC_PRESS_BLD_SYS.VALUE.equals(obj.metric_id)) {
                ibp.setSystolic(value);
            } else if(rosetta.MDC_PRESS_BLD_DIA.VALUE.equals(obj.metric_id)) {
                ibp.setDiastolic(value);
            } else if(rosetta.MDC_PRESS_BLD_MEAN.VALUE.equals(obj.metric_id)) {
                // for the future
            } else if (rosetta.MDC_PULS_OXIM_PULS_RATE.VALUE.equals(obj.metric_id)) {
                pulseox.setTargetHeartRate(value);
            } else if (rosetta.MDC_PULS_OXIM_SAT_O2.VALUE.equals(obj.metric_id)) {
                pulseox.setTargetSpO2(value);
            } else if (rosetta.MDC_CO2_RESP_RATE.VALUE.equals(obj.metric_id)) {
                capnometer.setRespirationRate(value);
            } else if (rosetta.MDC_AWAY_CO2_ET.VALUE.equals(obj.metric_id)) {
                capnometer.setEndTidalCO2(value);
            } else if (rosetta.MDC_ECG_HEART_RATE.VALUE.equals(obj.metric_id)) {
                ecg.setTargetHeartRate(value);
            } else if (rosetta.MDC_TTHOR_RESP_RATE.VALUE.equals(obj.metric_id)) {
                ecg.setTargetRespiratoryRate(value);
            }
        }
    }

    public String receiveGraphData(){

    try {

        GraphModal graphModal=new GraphModal();

        //ECG Modal
        ECGGraphModal ecgGraphModal=new ECGGraphModal();
        ecgGraphModal.setGa(ecgGaUserDataList);
        ecgGraphModal.setGb(ecgGbUserDataList);
        ecgGraphModal.setGc(ecgGcUserDataList);
        ecgGraphModal.setFrq(i.data.frequency);
       // graphModal.setEcg(ecgGraphModal);

        multiParameterGraph.put("ECG",ecgGraphModal);

        //Pleth modal
        CommonGraphModal plethModal=new CommonGraphModal();
        plethModal.setG(plethUserDataList);
        plethModal.setFrq(pleth.data.frequency);
       // graphModal.setPlt(plethModal);

        multiParameterGraph.put("PLT",plethModal);

        //pressure modal
        CommonGraphModal pressureModal=new CommonGraphModal();
        pressureModal.setG(pressureUserDataList);
        pressureModal.setFrq(pressure.data.frequency);
        //graphModal.setIbp(pressureModal);

        multiParameterGraph.put("IBP",pressureModal);

        //co2 modal
        CommonGraphModal co2Modal=new CommonGraphModal();
        co2Modal.setG(co2UserDataList);
        co2Modal.setFrq(co2.data.frequency);
       // graphModal.setCo(co2Modal);

        multiParameterGraph.put("CO",co2Modal);

        //setting time
        if(ecgGaUserDataList!=null) {
            jsonModal.setTs(new Long(i.data.presentation_time.sec)*1000);
        }else  if(plethUserDataList!=null){
            jsonModal.setTs(new Long(pleth.data.presentation_time.sec)*1000);
        }else  if(pressureUserDataList!=null){
            jsonModal.setTs(new Long(pressure.data.presentation_time.sec)*1000);
        }else  if(co2UserDataList!=null){
            jsonModal.setTs(new Long(co2.data.presentation_time.sec)*1000);
        }

       // jsonModal.setGrp(graphModal);

        jsonModal.setGrp(multiParameterGraph);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(jsonModal);
        return  json;

    }catch (Exception e){

    }
        return null;
    }


}

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
package org.mdpnp.devices.simulation.co2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.Subscriber;
import ice.ConnectionState;
import ice.GlobalSimulationObjective;
import org.java_websocket.client.WebSocketClient;
import org.mdpnp.common.graph.CommonGraphModal;
import org.mdpnp.devices.DeviceClock;
import org.mdpnp.devices.simulation.AbstractSimulatedConnectedDevice;
import org.mdpnp.devices.simulation.GlobalSimulationObjectiveListener;
import org.mdpnp.rtiapi.data.EventLoop;
import org.mdpnp.websocket.AuthenticateSocket;
import org.mdpnp.websocket.CommandLineModel;
import org.mdpnp.websocket.DeviceInfoUtils;
import org.mdpnp.websocket.WebsocketUtils;

import java.io.InputStream;
import java.util.*;

/**
 * @author Jeff Plourde
 *
 */
public class SimCapnometer extends AbstractSimulatedConnectedDevice {

    private InstanceHolder<ice.SampleArray> co2;
    private final InstanceHolder<ice.Numeric> respiratoryRate, etCO2;
    private final SimulatedCapnometer capnometer;

    private CapnometerJsonModal jsonModal;
    private String token;

    private WebsocketUtils wsu;
    private WebSocketClient connect;

    private  List<Float> co2UserDataList=new ArrayList<>();
    private class SimulatedCapnometerExt  extends SimulatedCapnometer {

        public SimulatedCapnometerExt(DeviceClock referenceClock) {
            super(referenceClock);
        }



        @Override
        protected void receiveCO2(DeviceClock.Reading sampleTime, Number[] co2Values, int respiratoryRateValue, int etCO2Value, int frequency) {

            co2 = sampleArraySample(co2, co2Values, rosetta.MDC_AWAY_CO2.VALUE, "", 0,
                    rosetta.MDC_DIM_MMHG.VALUE, frequency, sampleTime);
            numericSample(respiratoryRate, respiratoryRateValue, sampleTime);
            numericSample(etCO2, etCO2Value, sampleTime);

           /*************** Updated Code *************************/
            co2UserDataList.addAll(co2.data.values.userData);
        }
    }

    @Override
    public boolean connect(String str) {
        capnometer.connect(executor);
        return super.connect(str);
    }

    @Override
    public void disconnect() {
        capnometer.disconnect();
        super.disconnect();
    }


    String deviceID = null;
    String devicePWD=null;

    public SimCapnometer(final Subscriber subscriber, final Publisher publisher, EventLoop eventLoop) {
        super(subscriber, publisher, eventLoop);

        DeviceClock referenceClock = super.getClockProvider();
        capnometer = new SimulatedCapnometerExt(referenceClock);

        respiratoryRate = createNumericInstance(rosetta.MDC_CO2_RESP_RATE.VALUE, "");
        etCO2 = createNumericInstance(rosetta.MDC_AWAY_CO2_ET.VALUE, "");

        deviceIdentity.model = "Capnometer (Simulated)";
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

        /************ Generate Token  ****************/

//        AuthenticateSocket as=new AuthenticateSocket();
//       // token=as.generateToken();
//
//        token=as.generateTokenByDeviceIdAndPassword(deviceID,devicePWD);
//        System.out.println(token);
//        if(token==null){
//
//            System.out.println("Device authentication failed...!");
//            UIManager.put("OptionPane.minimumSize",new Dimension(300,150));
//            UIManager.put("OptionPane.messageFont", new Font("Arial", Font.BOLD, 14));
//            JOptionPane.showMessageDialog(null,"Device authentication failed...!");
//            System.exit(0);
//        }


        /************ Updated code ***************/

        jsonModal=new CapnometerJsonModal();
        jsonModal.setDev(deviceIdentity.unique_device_identifier);
        jsonModal.setDtp(deviceIdentity.model);
        jsonModal.setCon("");

        Map<String,Object> capnoStats=new LinkedHashMap<>();

        try {

            /************ Generate Token  ****************/

            AuthenticateSocket as=new AuthenticateSocket();
            token=as.generateTokenByDeviceIdAndPassword(deviceID,devicePWD);

            if(token==null){
                System.out.println("Device authentication failed...!");
//           System.exit(0);
            }
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

             //connect to webSocket
             wsu=new WebsocketUtils();
             connect = wsu.connectWebsocket(wrtUrl+token+"/");

            //start timer
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

                        capnoStats.put("RRC",respiratoryRate.data.value);
                        capnoStats.put("ETC",etCO2.data.value);
                        capnoStats.put("RST",false);
                        jsonModal.setSts(capnoStats);


                        String out = receiveGraphData();
                      //  System.out.println(out);

                        /************ send json to webSocket*******/

                        if (connect.isOpen()) {

                           wsu.sendMessageToWebsocket(out);
                           // clear data list
                            co2UserDataList.clear();
                        }else if(connect.isClosing()) {
                            System.out.println("Closing Connection....");
//                            log.info("Closing Connection....");
                        }else if(connect.isClosed()){
                            System.out.println("Re-Connecting....WebSocket....");
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
    }

    @Override
    protected String iconResourceName() {
        return "co2.png";
    }

    @Override
    public void simulatedNumeric(GlobalSimulationObjective obj) {
        Number value = GlobalSimulationObjectiveListener.toIntegerNumber(obj);
        if (rosetta.MDC_RESP_RATE.VALUE.equals(obj.metric_id) ||
                rosetta.MDC_CO2_RESP_RATE.VALUE.equals(obj.metric_id)) {
            capnometer.setRespirationRate(value);
        } else if (rosetta.MDC_AWAY_CO2_ET.VALUE.equals(obj.metric_id)) {
            capnometer.setEndTidalCO2(value);
        }
    }


    public String receiveGraphData(){

        try {
            CapnometerGraphModal gm=new CapnometerGraphModal();
            CommonGraphModal co=new CommonGraphModal();

            int size = co2UserDataList.size();
            int seconds = size/co2.data.frequency;
            float newFreq = size/(float)seconds;
            co.setFrq(newFreq);
            co.setG(co2UserDataList);
            gm.setCo(co);
            jsonModal.setTs(new Long(co2.data.presentation_time.sec)*1000);
            jsonModal.setGrp(gm);

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(jsonModal);
            return  json;

        }catch (Exception e){
            System.out.println(e);
        }
        return null;
    }
}

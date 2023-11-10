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
package org.mdpnp.devices.draeger.medibus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ice.ConnectionState;
import ice.LimitType;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.java_websocket.client.WebSocketClient;
import org.mdpnp.devices.DeviceClock;
import org.mdpnp.devices.DeviceClock.Reading;
import org.mdpnp.devices.DeviceClock.ReadingImpl;
import org.mdpnp.devices.Unit;
import org.mdpnp.devices.draeger.medibus.RTMedibus.RTTransmit;
import org.mdpnp.devices.draeger.medibus.types.Command;
import org.mdpnp.devices.draeger.medibus.types.MeasuredDataCP1;
import org.mdpnp.devices.draeger.medibus.types.MeasuredDataCP2;
import org.mdpnp.devices.draeger.medibus.types.RealtimeData;
import org.mdpnp.devices.draeger.medibus.types.Setting;
import org.mdpnp.devices.io.util.HexUtil;
import org.mdpnp.devices.serial.AbstractDelegatingSerialDevice;
import org.mdpnp.devices.simulation.AbstractSimulatedDevice;
import org.mdpnp.common.graph.CommonGraphModal;
import org.mdpnp.rtiapi.data.EventLoop;
import org.mdpnp.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.Subscriber;
import rosetta.*;
import ice.*;

import javax.swing.*;

public abstract class AbstractDraegerVent extends AbstractDelegatingSerialDevice<RTMedibus> {

    private static final Logger log = LoggerFactory.getLogger(AbstractDraegerVent.class);

    private Map<Enum<?>, String> numerics = new HashMap<Enum<?>, String>();
    private Map<Enum<?>, String> waveforms = new HashMap<Enum<?>, String>();

    protected Map<Object, InstanceHolder<ice.Numeric>> settingUpdates = new HashMap<Object, InstanceHolder<ice.Numeric>>();
    protected Map<Object, InstanceHolder<ice.Numeric>> numericUpdates = new HashMap<Object, InstanceHolder<ice.Numeric>>();
    protected Map<Object, InstanceHolder<ice.SampleArray>> sampleArrayUpdates = new HashMap<Object, InstanceHolder<ice.SampleArray>>();
    protected Map<Object, InstanceHolder<ice.AlarmLimit>> alarmLowLimitUpdates = new HashMap<Object, InstanceHolder<ice.AlarmLimit>>();
    protected Map<Object, InstanceHolder<ice.AlarmLimit>> alarmHighLimitUpdates = new HashMap<Object, InstanceHolder<ice.AlarmLimit>>();

    protected InstanceHolder<ice.Numeric> startInspiratoryCycleUpdate, startExpiratoryCycleUpdate, derivedRespiratoryRate;

    private Reading currentReading,previousReading;
    private static final long millisPerMinute=60*1000L;

    protected static final String[] priorities = new String[31];
    static {
        for(int i = 0; i < 6; i++) {
            priorities[i] = "Advisory("+(i+1)+") w/o tone";
        }
        for(int i = 6; i < 10; i++) {
            priorities[i] = "Advisory("+(i+1)+") w/tone";
        }
        for(int i = 10; i < 24; i++) {
            priorities[i] = "Caution("+(i+1)+")";
        }
        for(int i = 24; i < 31; i++) {
            priorities[i] = "Warning("+(i+1)+")";
        }
    }
    
    protected static final String priorityText(int priority) {
        priority--;
        if(priority >= 0 && priority < priorities.length) {
            return priorities[priority];
        } else {
            return "("+(priority+1)+")";
        }
    }

    protected void processStartInspCycle() {
        // TODO This should not be triggered as a numeric; it's a bad idea
        currentReading=deviceClock.instant();
        startInspiratoryCycleUpdate = numericSample(startInspiratoryCycleUpdate, 0,
                                                    ice.MDC_START_INSPIRATORY_CYCLE.VALUE, "",
                                                    rosetta.MDC_DIM_DIMLESS.VALUE, deviceClock.instant());
        log.info("Publishing startInspiratoryCycleUpdate sample");
            if(previousReading!=null) {
            //Then we have a current and previous reading - publish a derived respiratory rate

            long delta=currentReading.getTime().toEpochMilli()-previousReading.getTime().toEpochMilli();
            log.info("delta is "+delta);
            float rate=0;
            if(delta!=0){
                rate=millisPerMinute/delta;
            }
            derivedRespiratoryRate = numericSample(derivedRespiratoryRate, rate,
                                                   ice.ICE_DERIVED_RESPIRATORY_RATE.VALUE, "",
                                                   rosetta.MDC_DIM_BREATH.VALUE, new ReadingImpl(System.currentTimeMillis()-1000));
            log.info("Publishing derivedRate sample with value "+rate);
        }
        previousReading=currentReading;
    }

    protected void processStartExpCycle() {
        // TODO ditto the bad idea-ness of using Numeric topic for this
        startExpiratoryCycleUpdate = numericSample(startExpiratoryCycleUpdate, 0,
                                                   ice.MDC_START_EXPIRATORY_CYCLE.VALUE,
                                                   "", rosetta.MDC_DIM_DIMLESS.VALUE,
                                                   deviceClock.instant());
    }

    private static final int BUFFER_SAMPLES = 25;

    // Theoretical maximum 16 streams, practical limit seems to be 3
    // Buffering ten points is for testing, size of this buffer might be
    // a function of the sampling rate
    @SuppressWarnings("unchecked")
    private final List<Number>[] realtimeBuffer = new List[16];
    private final RTMedibus.RTDataConfig[] realtimeConfig = new RTMedibus.RTDataConfig[16];
    private final int[] realtimeUpsample = new int[16];
    private final int[] realtimeFrequency = new int[16];
    private long lastRealtime;
    private static final int MAX_UPSAMPLE = 10;

    protected void processRealtime(RTMedibus.RTDataConfig config, int multiplier, int streamIndex, Object code, double value) {
        lastRealtime = System.currentTimeMillis();
        if (streamIndex >= realtimeBuffer.length) {
            log.warn("Invalid realtime streamIndex=" + streamIndex);
            return;
        }
        realtimeConfig[streamIndex] = config;
        if(0 == realtimeFrequency[streamIndex]) {
            realtimeUpsample[streamIndex] = 1;
            while(0 != 1000000 % (config.interval*multiplier/realtimeUpsample[streamIndex])) {
                realtimeUpsample[streamIndex]++;
                if(realtimeUpsample[streamIndex] > MAX_UPSAMPLE) {
                    log.error("Cannot upsample interval of " + (config.interval/multiplier) + "ms to an even number of Hertz");
                    realtimeUpsample[streamIndex] = 1;
                    break;
                }
            }
            if(realtimeUpsample[streamIndex] != 1) {
                log.info("Upsampling " + code + " by factor " + realtimeUpsample[streamIndex]);
            }
            realtimeFrequency[streamIndex] = 1000000 / (config.interval*multiplier/realtimeUpsample[streamIndex]);
        }
        for(int i = 0; i < realtimeUpsample[streamIndex]; i++) {
            realtimeBuffer[streamIndex].add(value);
        }
        startEmitFastData(realtimeFrequency[streamIndex]);
    }

    private static final String codeToString(Object code) {
        if (code == null) {
            return "null";
        } else if (code instanceof Byte) {
            return HexUtil.toHexString((Byte) code) + "H";
        } else if (code instanceof Enum) {
            return ((Enum<?>) code).name();
        } else {
            return code.toString();
        }
    }
    
    private static final String metricOrCode(String metric_id, Object code, String type) {
        if (null != metric_id) {
            return metric_id;
        } else {
            return "DRAEGER_"+type+"_"+codeToString(code);
        }
    }

    @Override
    protected void unregisterAllNumericInstances() {
        super.unregisterAllNumericInstances();
        numericUpdates.clear();
        settingUpdates.clear();
    }

    @Override
    protected void unregisterAllSampleArrayInstances() {
        super.unregisterAllSampleArrayInstances();
        sampleArrayUpdates.clear();
    }

    @Override
    protected void unregisterAllAlarmLimitInstances() {
        super.unregisterAllAlarmLimitInstances();
        alarmLowLimitUpdates.clear();
        alarmHighLimitUpdates.clear();
    }

    protected void processCorrupt() {
    }

    private class MyRTMedibus extends RTMedibus {
        public MyRTMedibus(InputStream in, OutputStream out) throws IOException {
            super(in, out);
        }

        private final RTDataConfig currentRTConfig(RealtimeData rd, RTDataConfig[] currentRTDataConfig) {
            for (int i = 0; i < currentRTDataConfig.length; i++) {
                if (rd.equals(currentRTDataConfig[i].realtimeData)) {
                    return currentRTDataConfig[i];
                }
            }
            return null;
        }

        @Override
        protected void receiveRealtimeConfig(RTDataConfig[] currentRTDataConfig) {
            super.receiveRealtimeConfig(currentRTDataConfig);
            if (ice.ConnectionState.Connected.equals(getState())) {
                List<RTTransmit> transmits = new ArrayList<RTTransmit>();
                for (RealtimeData rd : REQUEST_REALTIME) {
                    RTDataConfig config = currentRTConfig(rd, currentRTDataConfig);
                    if (null != config) {
                        transmits.add(new RTTransmit(rd, 1, config));
                    } else {
                        log.warn("Device does not support requested " + rd);
                    }
                }

                try {
                    log.trace("Realtime configuration received and Connected so sending RT xmit command: " + transmits);
                    sendRTTransmissionCommand(transmits.toArray(new RTTransmit[0]));
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        @Override
        protected void receiveResponse(byte[] response, int len) throws CorruptMedibusException {
            super.receiveResponse(response, len);
            Object cmdEcho = Command.fromByteIf(response[0]);
            if (cmdEcho instanceof Command) {
                synchronized(AbstractDraegerVent.this) {
                    if(lastSlowDataRequest >= 0 && REQUEST_SLOW[lastSlowDataRequest].equals((Command)cmdEcho)) {
                        log.trace("Response to command acknowledges the prior request" + cmdEcho);
                        lastSlowDataRequestAcknowledged = true;
                    }
                }
                switch ((Command) cmdEcho) {
                case InitializeComm:
                    initializeCommAcknowledged();
                    break;
                case ConfigureRealtime:
                    realtimeTransmitAcknowledged();
                    break;
                default:
                }
            }
        }

        @Override
        protected void receiveDeviceIdentification(String idNumber, String name, String revision) {
            receiveDeviceId(idNumber, name);
        }

        @Override
        protected void receiveTextMessage(Data[] data) {
            markOldTechnicalAlertInstances();
            for (Data d : data) {
                if (null != d) {
                    writeTechnicalAlert(d.code.toString(), d.data);
                }
            }
            clearOldTechnicalAlertInstances();
        }

        @Override
        protected void receiveDeviceSetting(Data[] data) {
            for (Data d : data) {
                if (null != d) {
                    // There are a couple of settings that we map to
                    // custom types in the ice package
                    String metric = numerics.get(d.code);
                    metric = metricOrCode(metric, d.code, "SETTING");
                    String s = null == d.data ? null : d.data.toString().trim();
                    Float f = null;
                    try {
                        f = Float.parseFloat(s);
                    } catch (NumberFormatException nfe) {
                        // Stack traces here are too noisy
                        // on our EvitaXL 
                        log.error("Bad number format for device setting " + d.code + " " + nfe.getMessage());
                    }
                    settingUpdates.put(d.code,
                                       numericSample(settingUpdates.get(d.code), f, metric, codeToString(d.code), units(d.code), deviceClock.instant()));


                    /*********** updated code ***************/

                    try {
                        separateStatisticSettingsValues(settingUpdates);
                    }catch (Exception e){
                        // System.out.println(e);
                    }

                    /*****************/


                }
            }
        }

        @Override
        protected void receiveMeasuredData(int codepage, Data[] data) {
            for (Data d : data) {
                if (null != d) {
                    String metric = numerics.get(d.code);
                    metric = metricOrCode(metric, d.code, "MEASURED_CP"+codepage);
                    String s = null == d.data ? null : d.data.toString().trim();
                    Float f = null;
                    try {
                        f = Float.parseFloat(s);
                    } catch (NumberFormatException nfe) {
                        log.error("Bad measured data number format " + d.code + " " + nfe.getMessage());
                    }
                    numericUpdates.put(d.code,
                                       numericSample(numericUpdates.get(d.code), f, metric, codeToString(d.code), units(d.code), deviceClock.instant()));


                    /*********** updated code ***************/

                    try {
                        separateStatistic(numericUpdates);
                    }catch (Exception e){
                        // System.out.println(e);
                    }

                    /*****************/

                }
            }
        }

        @Override
        protected void receiveCorruptResponse() {
            processCorrupt();
        }

        @Override
        public void receiveDataValue(RTMedibus.RTDataConfig config, int multiplier, int streamIndex, Object realtimeData, double data) {
            processRealtime(config, multiplier, streamIndex, realtimeData, data);
        }

        @Override
        protected void receiveAlarmCodes(Command cmdEcho, byte[] response, int len) throws CorruptMedibusException {
            switch (cmdEcho) {
            case ReqAlarmsCP1:
                // Before processing alarms codepage 1, mark current alarms
                markOldPatientAlertInstances();
                break;
            default:
            }
            super.receiveAlarmCodes(cmdEcho, response, len);
            switch (cmdEcho) {
            case ReqAlarmsCP2:
                // After processing alarms codepage 2, clear unrenewed alarms
                clearOldPatientAlertInstances();
                break;
            default:
            }
        }

        @Override
        protected void receiveLowAlarmLimits(int codepage, Data[] data) {
            for (Data d : data) {
                if (null != d) {
                    Float f = null;
                    try {
                        f = Float.parseFloat(d.data);
                    } catch (NumberFormatException nfe) {
                        log.error("Bad number format for low alarm " + d.code + " " + nfe.getMessage());
                    }
                    InstanceHolder<ice.AlarmLimit> a = alarmLowLimitUpdates.get(d.code);
                    String metric = numerics.get(d.code);
                    metric = metricOrCode(metric, d.code, "ALARM_LIMIT_CP"+codepage);
                    alarmLowLimitUpdates.put(d.code, alarmLimitSample(a, rosetta.MDC_DIM_DIMLESS.VALUE, f, metric, LimitType.low_limit));
                }
            }
        }

        @Override
        protected void receiveHighAlarmLimits(int codepage, Data[] data) {
            for (Data d : data) {
                if (null != d) {
                    Float f = null;
                    try {
                        f = Float.parseFloat(d.data);
                    } catch (NumberFormatException nfe) {
                        log.error("Bad number format for high alarm " + d.code + " " + nfe.getMessage());
                    }
                    InstanceHolder<ice.AlarmLimit> a = alarmHighLimitUpdates.get(d.code);
                    String metric = numerics.get(d.code);
                    metric = metricOrCode(metric, d.code, "ALARM_LIMIT_CP"+codepage);
                    alarmHighLimitUpdates.put(d.code, alarmLimitSample(a, rosetta.MDC_DIM_DIMLESS.VALUE, f, metric, LimitType.high_limit));
                }
            }
        }

        
        @Override
        protected void receiveAlarms(Alarm[] alarms) {
            for (Alarm a : alarms) {
                if (a != null) {
                    writePatientAlert(a.alarmCode.toString(), a.alarmPhrase+" "+priorityText(a.priority));

                    /**** Updated Code **********/
                    fetchAlarm(a);
                }
            }
           
        }

        @Override
        protected void receiveDateTime(Date date) {
            deviceClock.receiveDateTime(date);
        }

        @Override
        public void startInspiratoryCycle() {
            processStartInspCycle();
        }

        @Override
        public void startExpiratoryCycle() {
            processStartExpCycle();
        }

    }

    DraegerVentClock deviceClock = new DraegerVentClock(getClockProvider());

    static class DraegerVentClock implements DeviceClock  {
        final DeviceClock referenceClock;
        public DraegerVentClock(final DeviceClock referenceClock) {
            this.referenceClock = referenceClock;
        }
        
        private final ThreadLocal<Long> currentTime = new ThreadLocal<Long>() {
            protected Long initialValue() {
                return 0L;
            };
        };

        protected long deviceClockOffset = 0L;

        protected long receiveDateTime(Date date) {
            deviceClockOffset = date.getTime() - systemCurrentTimeMillis();
            log.debug("Device says date is: " + date + " - Local clock offset " + deviceClockOffset + "ms from device");
            return deviceClockOffset;
        }

        long systemCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public Reading instant() {
            return new DeviceClock.CombinedReading(
                    referenceClock.instant(),
                    new DeviceClock.ReadingImpl(currentTimeAdjusted()));
        }

        protected long currentTimeAdjusted() {
            long now =  systemCurrentTimeMillis() + deviceClockOffset;
            long then = currentTime.get();
            if (then - now > 0L) {
                // This happens too routinely to expend the I/O here
                // tried using the desination_order.source_timestamp_tolerance but
                // that was even too tight
                // TODO reconsider how we are deriving a device timestamp
                // log.warn("Not emitting timestamp="+new
                // Date(now)+" where last timestamp was "+new Date(then));
                return then;
            } else {
                currentTime.set(now);
                return now;
            }
        }
    }

    private static final RealtimeData[] REQUEST_REALTIME = new RealtimeData[] { RealtimeData.AirwayPressure, RealtimeData.FlowInspExp,
            RealtimeData.RespiratoryVolumeSinceInspBegin, RealtimeData.ExpiratoryCO2mmHg, RealtimeData.ExpiratoryVolume, RealtimeData.Ptrach,
            RealtimeData.InspiratoryFlow, RealtimeData.ExpiratoryFlow, RealtimeData.Pleth };

    private static final Command[] REQUEST_SLOW = new Command[] { 
            Command.ReqDateTime, Command.ReqDeviceSetting, 
            Command.ReqAlarmsCP1, //Command.ReqAlarmsCP2,
            Command.ReqMeasuredDataCP1, //Command.ReqMeasuredDataCP2, 
            Command.ReqLowAlarmLimitsCP1, //Command.ReqLowAlarmLimitsCP2, 
            Command.ReqHighAlarmLimitsCP1,//Command.ReqHighAlarmLimitsCP2, 
            Command.ReqTextMessages 
    };

    private int lastSlowDataRequest = -1;
    private long lastSlowDataRequestTime = 0L;
    private volatile boolean lastSlowDataRequestAcknowledged = false;
    private static final long MAX_WAIT_SLOW_DATA = 5000L;

    private class EmitFastData implements Runnable {

        private final int frequency;

        public EmitFastData(final int frequency) {
            this.frequency = frequency;
        }

        @Override
        public void run() {
            if(ice.ConnectionState.Connected.equals(getState())) {
                try {
                    for (int i = 0; i < realtimeBuffer.length; i++) {
                        if (null == realtimeConfig[i] || realtimeFrequency[i] != this.frequency) {
                            continue;
                        }
                        Object code = realtimeConfig[i].realtimeData;
                        InstanceHolder<ice.SampleArray> sa = sampleArrayUpdates.get(code);
                        if (null != sa) {
                            // In this implementation we're not changing the
                            // requested realtime data; so we
                            // expedite here using the same preregistered instance
                            synchronized (realtimeBuffer[i]) {
                                if (realtimeBuffer[i].size() >= BUFFER_SAMPLES) {
                                    if (realtimeBuffer[i].size() > BUFFER_SAMPLES) {
                                        realtimeBuffer[i].subList(0, realtimeBuffer[i].size() - BUFFER_SAMPLES).clear();
                                    }
                                    sampleArraySample(sa, realtimeBuffer[i], deviceClock.instant());

                                    /********* Updated code ***********/
                                    separateGraph(sa);

                                }
                            }
                        } else {
    
                            String metric_id = null;
                            // flush
                            if (realtimeConfig[i].realtimeData instanceof Enum<?>) {
                                metric_id = waveforms.get(realtimeConfig[i].realtimeData);
                            }
                            // NOTE: config.interval is the sampling interval
                            // expressed in MICRO-seconds
                            // The specification is ambiguous using ms for micro and
                            // milli...
                            // but in the examples '16000' is stated to mean 16
                            // milliseconds
                            // int frequency = (int)(1000000f /
                            // realtimeConfig[i].interval /
                            // realtimeConfig[i].multiplier);
    
                            metric_id = metricOrCode(metric_id, code, "RT");
                            synchronized (realtimeBuffer[i]) {
                                if (realtimeBuffer[i].size() >= BUFFER_SAMPLES) {
                                    if (realtimeBuffer[i].size() > BUFFER_SAMPLES) {
                                        realtimeBuffer[i].subList(0, realtimeBuffer[i].size() - BUFFER_SAMPLES).clear();
                                    }
                                    sampleArrayUpdates.put(code,
                                            sampleArraySample(sa, realtimeBuffer[i], metric_id, codeToString(code), 0, units(code), realtimeFrequency[i], deviceClock.instant()));
                                }
                            }
                        }
    
                    }
                } catch (Throwable t) {
                    log.error("error emitting fast data", t);
                }
            }
        }

    }

    private class RequestSlowData implements Runnable {
        public void run() {
            if (ice.ConnectionState.Connected.equals(getState())) {
                long now = System.currentTimeMillis();
                try {
                    RTMedibus medibus = AbstractDraegerVent.this.getDelegate();
                    if ((now - lastRealtime) >= 10000L) {
                        log.warn("" + (now - lastRealtime) + "ms since realtime data, requesting realtime config");
                        // Starts a process by requesting the realtime
                        // configuration
                        // see receiveRealtimeConfig(...)
                        lastRealtime = System.currentTimeMillis();
                        medibus.sendCommand(Command.ReqRealtimeConfig);
                        return;
                    }
                    
                    synchronized(AbstractDraegerVent.this) {
                        // Should we send a new request?  If no requests have been sent or the previous request was acknowledged
                        boolean makeNewRequest = lastSlowDataRequest < 0 || lastSlowDataRequestAcknowledged;
                        
                        // If too much time has elapsed since the request was made move on
                        if(!makeNewRequest && now >= (lastSlowDataRequestTime+MAX_WAIT_SLOW_DATA)) {
                            log.warn("Timed out waiting for a response to " + REQUEST_SLOW[lastSlowDataRequest] + " after " + (now-lastSlowDataRequestTime));
                            makeNewRequest = true;
                        }
                        
                        if(makeNewRequest) {
                            lastSlowDataRequest++;
                            lastSlowDataRequestTime = now;
                            lastSlowDataRequest = lastSlowDataRequest % REQUEST_SLOW.length;
                            lastSlowDataRequestAcknowledged = false;
                            log.trace("Requesting the slow data for " + REQUEST_SLOW[lastSlowDataRequest]);
                            medibus.sendCommand(REQUEST_SLOW[lastSlowDataRequest]);
                        }
                    }
                    // if (now - lastReqDateTime >= 15000L) {
                    // log.debug("Slow data too old, requesting DateTime");
                    // lastReqDateTime = now;
                    // medibus.sendCommand(Command.ReqDateTime);
                    // return;
                    // }

                    // Data is sparse in standby mode; trying to keep alive
                    // TODO need to externalize all these timing settings
                    // eventually
                    if ((now - timeAwareInputStream[0].getLastReadTime()) >= (getMaximumQuietTime(0) / 2L)) {
                        medibus.sendCommand(Command.NoOperation);
                        return;
                    }
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                }
            }

        }
    }

    @Override
    public void disconnect() {
        stopRequestSlowData();
        RTMedibus medibus = null;
        synchronized (this) {
            medibus = getDelegate(false);
        }
        if (null != medibus) {
            try {
                medibus.sendCommand(Command.StopComm);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.debug("rtMedibus was already null in disconnect");
        }
        super.disconnect();
    }

    public void init() {

        super.init();

        AbstractSimulatedDevice.randomUDI(deviceIdentity);
        deviceIdentity.manufacturer = "Dr\u00E4ger";
        deviceIdentity.model = "???";
        writeDeviceIdentity();


        /***** Updated code ******/
        fetchConnectedDeviceData();

    }

    static void loadMap(Map<Enum<?>, String> numerics, Map<Enum<?>, String> waveforms) {
        loadMap(AbstractDraegerVent.class.getResource("draeger.map"), numerics, waveforms);
    }

    static void loadMap(URL uri, Map<Enum<?>, String> numerics, Map<Enum<?>, String> waveforms) {

        InputStream source;
        try {
            source = uri.openStream();
        }
        catch(IOException ex) {
            throw new IllegalArgumentException("Cannot open input stream");
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(source));
            String line = null;
            String draegerPrefix = MeasuredDataCP1.class.getPackage().getName() + ".";

            while (null != (line = br.readLine())) {
                line = line.trim();
                if ('#' != line.charAt(0)) {
                    String v[] = line.split("\t");

                    if (v.length < 3) {
                        log.debug("Bad line:" + line);
                    } else {
                        String c[] = v[0].split("\\.");
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        Enum<?> draeger = (Enum<?>) Enum.valueOf((Class<? extends Enum>) Class.forName(draegerPrefix + c[0]), c[1]);
                        String tag = getValue(v[1]);
                        if (tag == null) {
                            log.warn("cannot find value for " + v[1]);
                            continue;
                        }
                        log.trace("Adding " + draeger + " mapped to " + tag);
                        v[2] = v[2].trim();
                        if ("W".equals(v[2])) {
                            waveforms.put(draeger, tag);
                        } else if ("N".equals(v[2])) {
                            numerics.put(draeger, tag);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                source.close();
            } catch (IOException e) {
                //
            }
        }
    }

    private ScheduledFuture<?> requestSlowData;
    private final Map<Integer, ScheduledFuture<?>> emitFastDataByFrequency = new HashMap<Integer, ScheduledFuture<?>>();

    private DraegerJsonModal jsonModal;
    private String token;
    private Map<String,Object> draegerStats;
    private Map<String,Object> draegerGraph;

    private Map<String,Object> draegerAlarm;

    private JsonArray observationArray = new JsonArray();


    private WebsocketUtils wsu;
    private WebSocketClient connect;

    @Override
    protected void stateChanged(ConnectionState newState, ConnectionState oldState, String transitionNote) {

        if (ice.ConnectionState.Connected.equals(newState) && !ice.ConnectionState.Connected.equals(oldState)) {
            startRequestSlowData();
        }
        if (!ice.ConnectionState.Connected.equals(newState) && ice.ConnectionState.Connected.equals(oldState)) {
            stopRequestSlowData();
            stopEmitFastData();
        }
        super.stateChanged(newState, oldState, transitionNote);
    }

    private synchronized void stopRequestSlowData() {
        if (null != requestSlowData) {
            requestSlowData.cancel(false);
            requestSlowData = null;
            log.trace("Canceled slow data request task");
        } else {
            log.trace("Slow data request already canceled");
        }
    }

    private synchronized void stopEmitFastData() {
        for (Integer frequency : emitFastDataByFrequency.keySet()) {
            log.info("stop emit fast data at frequency " + frequency);
            emitFastDataByFrequency.get(frequency).cancel(false);
        }
        emitFastDataByFrequency.clear();
    }

    private synchronized void startRequestSlowData() {
        if (null == requestSlowData) {
            requestSlowData = executor.scheduleWithFixedDelay(new RequestSlowData(), 0L, 200L, TimeUnit.MILLISECONDS);
            log.trace("Scheduled slow data request task");
        } else {
            log.trace("Slow data request already scheduled");
        }
    }

    private synchronized void startEmitFastData(int frequency) {
        long interval = 1000L / frequency * BUFFER_SAMPLES;
        if (!emitFastDataByFrequency.containsKey(frequency)) {
            log.info("Start emit fast data at frequency " + frequency);
            emitFastDataByFrequency.put(frequency, executor.scheduleAtFixedRate(new EmitFastData(frequency), interval - System.currentTimeMillis()
                    % interval, interval, TimeUnit.MILLISECONDS));
        }
    }

    private static String getValue(String name) throws Exception {
        try {
            Class<?> cls = Class.forName(name);
            return (String) cls.getField("VALUE").get(null);
        } catch (ClassNotFoundException e) {
            // If it's not a class then maybe it's a static member
            int lastIndexOfDot = name.lastIndexOf('.');
            if (lastIndexOfDot < 0) {
                throw e;
            }
            Class<?> cls = Class.forName(name.substring(0, lastIndexOfDot));
            Object obj = cls.getField(name.substring(lastIndexOfDot + 1, name.length())).get(null);
            return (String) obj.getClass().getMethod("value").invoke(obj);

        }

    }

    public AbstractDraegerVent(final Subscriber subscriber, final Publisher publisher, EventLoop eventLoop) {
        super(subscriber, publisher, eventLoop, RTMedibus.class);
        for (int i = 0; i < realtimeBuffer.length; i++) {
            realtimeBuffer[i] = Collections.synchronizedList(new ArrayList<Number>());
        }
        loadMap(numerics, waveforms);

    }

    @Override
    protected RTMedibus buildDelegate(int idx, InputStream in, OutputStream out) {
        log.trace("Creating an RTMedibus");
        try {
            return new MyRTMedibus(in, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean delegateReceive(int idx, RTMedibus delegate) throws IOException {
        return delegate.receive();
    }

    protected synchronized void receiveDeviceId(String guid, String name) {
        log.trace("receiveDeviceId:guid=" + guid + ", name=" + name);

        boolean writeIt = false;
        if (null != guid) {
            deviceIdentity.serial_number = guid;
            writeIt = true;

        }
        if (null != name) {
            deviceIdentity.model = name;
            writeIt = true;
        }
        if (writeIt) {
            writeDeviceIdentity();
        }
        reportConnected("Device Id Message Received");
    }

    @Override
    protected void doInitCommands(int idx) throws IOException {
        super.doInitCommands(idx);
        RTMedibus rtMedibus = getDelegate();

        rtMedibus.sendCommand(Command.InitializeComm);
    }

    protected void realtimeTransmitAcknowledged() {
        if (ice.ConnectionState.Connected.equals(getState())) {
            RTTransmit[] lastTransmitted = getDelegate().getLastTransmitted();
            int[] traces = new int[lastTransmitted.length];
            for (int i = 0; i < traces.length; i++) {
                traces[i] = lastTransmitted[i].rtDataConfig.ordinal;
            }
            try {
                log.trace("Realtime transmits acknowledged so enabling realtime traces:" + Arrays.toString(traces));
                getDelegate().sendEnableRealtime(traces);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    protected void initializeCommAcknowledged() {

        try {
            getDelegate().sendCommand(Command.ReqDeviceId);
        } catch (IOException ioe) {
            log.error("Unable to request device id", ioe);
        }
    }

    @Override
    protected long getMaximumQuietTime(int idx) {
        return 3000L;
    }

    @Override
    protected long getConnectInterval(int idx) {
        return 3000L;
    }

    @Override
    protected long getNegotiateInterval(int idx) {
        return 1000L;
    }

    protected static final String units(Object obj) {
        if (obj == null) {
            return rosetta.MDC_DIM_DIMLESS.VALUE;
        } else if (obj instanceof MeasuredDataCP1) {
            return units(((MeasuredDataCP1) obj).getUnit());
        } else if (obj instanceof MeasuredDataCP2) {
            return units(((MeasuredDataCP2) obj).getUnit());
        } else if (obj instanceof RealtimeData) {
            return units(((RealtimeData) obj).getUnit());
        } else if (obj instanceof Setting) {
            return units(((Setting) obj).getUnit());
        } else {
            return "DRAEGER_UNKNOWN_UNITS";
        }
    }

    protected static final String units(Unit unit) {
        if (null == unit) {
            return rosetta.MDC_DIM_DIMLESS.VALUE;
        }
        switch (unit) {
        case kg:
            return rosetta.MDC_DIM_KILO_G.VALUE;
        case kPa:
            return rosetta.MDC_DIM_KILO_PASCAL.VALUE;
        case L:
            return rosetta.MDC_DIM_L.VALUE;
        case LPerMin:
            return rosetta.MDC_DIM_L_PER_MIN.VALUE;
        case mL:
            return rosetta.MDC_DIM_MILLI_L.VALUE;
        case mmHg:
            return rosetta.MDC_DIM_MMHG.VALUE;
        case mLPerMin:
            return rosetta.MDC_DIM_MILLI_L_PER_MIN.VALUE;
        case sec:
            return rosetta.MDC_DIM_SEC.VALUE;
        case pct:
            return rosetta.MDC_DIM_PERCENT.VALUE;
        case OnePerMin:
        case pctFullScale:
        case a:
        case None:
        case mlPerMBar:
        case mbar:
        case TenMlPerMin:
        case mbarPerL:
        default:
            return "DRAEGER_" + unit.name();
        }
    }

    /*********** updated code ************/

    String deviceID = null;
    String devicePWD=null;
    public void fetchConnectedDeviceData(){



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




        try{


           /************ Draeger Statistic Modal ************/

            System.out.println("State  :"+getState());
            log.info("State  :"+getState());

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    if(ConnectionState.Terminal.equals(getState())){
                        timer.cancel();
                        timer.purge();
                        System.out.println("Timer terminated ");
                        log.info("Timer terminated ");
                        return;
                    }

                    if(ConnectionState.Connected.equals(getState())) {

                        UpdateObservations uo = new UpdateObservations();
                        JsonArray mainArray = new JsonArray();
                        mainArray.add(observationArray);
                        Gson gson = new Gson();
                        String jsonString = gson.toJson(mainArray);

                        int status = uo.SendObservations(jsonString);
                        System.out.println( "Data sent: " + jsonString );
                        System.out.println( "ObservationXXXXXXXXXXXXXXXX test result Code: " + status );

                        /************ send json to webSocket *******/
                        if (status == 200) {
                            observationArray = new JsonArray();

                            // clear data list
                            pressureGraphUserDataList.clear();
                            flowGraphUserDataList.clear();
                            volumeGraphUserDataList.clear();
                            plethGraphUserDataList.clear();
                            capnoGraphUserDataList.clear();
                            oxygenLevelGraphUserDataList.clear();

                            draegerAlarmList.clear();

                        }
                    }

                }
            }, 0, 2000);

        }catch (Exception e){

            e.printStackTrace();
        }

    }

//    public String receiveGraphData(){
//
//        try {
//            draegerGraph=new LinkedHashMap<>();
//
//            //Pressure Graph modal
//            CommonGraphModal pressureModal=new CommonGraphModal();
//            pressureModal.setG(pressureGraphUserDataList);
//            pressureModal.setFrq(pressureFrequency);
//
//            if(!pressureGraphUserDataList.isEmpty()) {
//                draegerGraph.put("P", pressureModal);
//            }
//
//            //Flow Graph modal
//            CommonGraphModal flowModal=new CommonGraphModal();
//            flowModal.setG(flowGraphUserDataList);
//            flowModal.setFrq(flowFrequency);
//
//            if(!flowGraphUserDataList.isEmpty()) {
//                draegerGraph.put("F", flowModal);
//            }
//
//            //Volume Graph modal
//            CommonGraphModal volumeModal=new CommonGraphModal();
//            volumeModal.setG(volumeGraphUserDataList);
//            volumeModal.setFrq(volumeFrequency);
//
//            if(!volumeGraphUserDataList.isEmpty()) {
//                draegerGraph.put("V", volumeModal);
//            }
//
//            //PlethysmoGraphy
//            CommonGraphModal plethModal=new CommonGraphModal();
//            plethModal.setG(plethGraphUserDataList);
//            plethModal.setFrq(plethFrequency);
//
//            if(!plethGraphUserDataList.isEmpty()) {
//                draegerGraph.put("PLT", plethModal);
//            }
//
//            //Capnography modal
//            CommonGraphModal capnoModal=new CommonGraphModal();
//            capnoModal.setG(capnoGraphUserDataList);
//            capnoModal.setFrq(capnoFrequency);
//
//            if(!capnoGraphUserDataList.isEmpty()) {
//                draegerGraph.put("CO", capnoModal);
//            }
//
//            //OxygenLevel modal
//            CommonGraphModal oxygenLevelModal=new CommonGraphModal();
//            oxygenLevelModal.setG(oxygenLevelGraphUserDataList);
//            oxygenLevelModal.setFrq(oxygenLevelFrequency);
//
//            if(!oxygenLevelGraphUserDataList.isEmpty()) {
//                draegerGraph.put("OO", oxygenLevelModal);
//            }
//
//            //Json Modal
//            jsonModal.setTs(new Long(presentationTime)*1000);
//            jsonModal.setGrp(draegerGraph);
//
//            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
//            String json = ow.writeValueAsString(jsonModal);
//            return  json;
//
//        }catch (Exception e){
//
//        }
//        return null;
//    }

    private List<Float> pressureGraphUserDataList=new ArrayList<>();
    private List<Float> flowGraphUserDataList=new ArrayList<>();
    private List<Float> volumeGraphUserDataList=new ArrayList<>();
    private List<Float> plethGraphUserDataList=new ArrayList<>();
    private List<Float> capnoGraphUserDataList=new ArrayList<>();
    private List<Float> oxygenLevelGraphUserDataList=new ArrayList<>();

    private Set<Map<String,Object>> draegerAlarmList=new HashSet<>();

    private float pressureFrequency,flowFrequency,volumeFrequency,plethFrequency,
                 capnoFrequency,oxygenLevelFrequency;
    private long presentationTime;

    public void separateGraph(InstanceHolder<ice.SampleArray> sa){

        //Pressure Graph
        if(sa.data.metric_id.equals(rosetta.MDC_PRESS_AWAY.VALUE)){

            pressureGraphUserDataList.addAll(sa.data.values.userData);
            pressureFrequency=sa.data.frequency;

            int size = pressureGraphUserDataList.size();
            float seconds = size/pressureFrequency;
            pressureFrequency = size/seconds;
            presentationTime=sa.data.presentation_time.sec;
            createWaveformObservation("P", "0.1cmH2O", pressureFrequency, pressureGraphUserDataList, 0, -10, 60);

        }
        //Flow Graph
        if(sa.data.metric_id.equals(rosetta.MDC_FLOW_AWAY.VALUE)){

            flowGraphUserDataList.addAll(sa.data.values.userData);
            flowFrequency=sa.data.frequency;

            int size = flowGraphUserDataList.size();
            float seconds = size/flowFrequency;
            flowFrequency = size/seconds;
            presentationTime=sa.data.presentation_time.sec;
            createWaveformObservation("F", "1mlps", flowFrequency, flowGraphUserDataList, 0, -60, 60);

        }
        //Volume Graph
        if(sa.data.metric_id.equals(rosetta.MDC_VENT_VOL_TIDAL.VALUE)){

            volumeGraphUserDataList.addAll(sa.data.values.userData);
            volumeFrequency=sa.data.frequency;

            int size = volumeGraphUserDataList.size();
            float seconds = size/volumeFrequency;
            volumeFrequency = size/seconds;
            presentationTime=sa.data.presentation_time.sec;
            createWaveformObservation("V", "1mL", volumeFrequency, volumeGraphUserDataList, 0, -50, 1000);

        }
        //PlethysmoGraphy
        if(sa.data.metric_id.equals(rosetta.MDC_PULS_OXIM_PLETH.VALUE)){

            plethGraphUserDataList.addAll(sa.data.values.userData);
            plethFrequency=sa.data.frequency;

            int size = plethGraphUserDataList.size();
            float seconds = size/plethFrequency;
            plethFrequency = size/seconds;
            presentationTime=sa.data.presentation_time.sec;

        }
        //Capnography
        if(sa.data.metric_id.equals(rosetta.MDC_AWAY_CO2.VALUE)){

            capnoGraphUserDataList.addAll(sa.data.values.userData);
            capnoFrequency=sa.data.frequency;

            int size = capnoGraphUserDataList.size();
            float seconds = size/capnoFrequency;
            capnoFrequency = size/seconds;
            presentationTime=sa.data.presentation_time.sec;

        }
        //Oxygen Level
        if(sa.data.metric_id.equals(rosetta.MDC_FLOW_O2_CONSUMP.VALUE)){

            oxygenLevelGraphUserDataList.addAll(sa.data.values.userData);
            oxygenLevelFrequency=sa.data.frequency;

            int size = oxygenLevelGraphUserDataList.size();
            float seconds = size/oxygenLevelFrequency;
            oxygenLevelFrequency = size/seconds;
            presentationTime=sa.data.presentation_time.sec;

        }

    }

    public void createObservation(String obId, String unit, Number value, Number lowLimit, Number highLimit){
        if(!(Float.isNaN(value.floatValue())) && !(Double.isNaN(value.doubleValue()))) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);

            JsonObject tempObservation = new JsonObject();
            tempObservation.addProperty("observation_id", obId);
            tempObservation.addProperty("device_id", "192.168.1.190");
            tempObservation.addProperty("date-time", formattedDateTime);
            tempObservation.addProperty("patient-id", "3");
            tempObservation.addProperty("patient-name", "PATIENT 3");
            tempObservation.addProperty("status", "final");
            tempObservation.addProperty("value", value);
            tempObservation.addProperty("unit", unit);
            tempObservation.addProperty("interpretation", "normal");
            tempObservation.addProperty("low-limit", lowLimit);
            tempObservation.addProperty("high-limit", highLimit);

            observationArray.add(tempObservation);
        }

    }

    public void createObservation(String obId, String unit, String value, Number lowLimit, Number highLimit){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        JsonObject tempObservation = new JsonObject();
        tempObservation.addProperty("observation_id", obId);
        tempObservation.addProperty("device_id", "192.168.1.190");
        tempObservation.addProperty("date-time", formattedDateTime);
        tempObservation.addProperty("patient-id", "3");
        tempObservation.addProperty("patient-name", "PATIENT 3");
        tempObservation.addProperty("status", "final");
        tempObservation.addProperty("value", value);
        tempObservation.addProperty("unit", unit);
        tempObservation.addProperty("interpretation", "normal");
        tempObservation.addProperty("low-limit", lowLimit);
        tempObservation.addProperty("high-limit", highLimit);

        observationArray.add(tempObservation);

    }

    public void createWaveformObservation(String waveName, String resolution, float samplingRate, List<Float> data, Number baseline, Number lowLimit, Number highLimit){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        StringBuilder sb = new StringBuilder();
        for (Float value : data) {
            sb.append(value).append(" ");
        }

        String dataString = sb.toString().trim();

        JsonObject tempObservation = new JsonObject();
        tempObservation.addProperty("observation_id", "waveform");
        tempObservation.addProperty("device_id", "192.168.1.190");
        tempObservation.addProperty("date-time", formattedDateTime);
        tempObservation.addProperty("patient-id", "3");
        tempObservation.addProperty("patient-name", "PATIENT 3");
        tempObservation.addProperty("wave-name", waveName);
        tempObservation.addProperty("resolution", resolution);
        tempObservation.addProperty("sampling rate", samplingRate + "/sec");
        tempObservation.addProperty("data-baseline", baseline);
        tempObservation.addProperty("data-low-limit", lowLimit);
        tempObservation.addProperty("data-high-limit", highLimit);
        tempObservation.addProperty("data", dataString);

        observationArray.add(tempObservation);

    }

    private float ieIpart,ieEpart;
    //Statistics
    public void separateStatistic(Map<Object, InstanceHolder<ice.Numeric>> numericUpdates){

        numericUpdates.values().stream().forEach(s-> {

            //Pulse BPM
            if (s.data.metric_id.equals(MDC_PULS_OXIM_PULS_RATE.VALUE)) {

            }
            //End tidal CO2
            if (s.data.metric_id.equals(MDC_AWAY_CO2_ET.VALUE)) {
                createObservation("etCO2","mmHg", s.data.value,null,null);
            }
            //Respiratory rate //Capno
            if (s.data.metric_id.equals(MDC_CO2_RESP_RATE.VALUE)) {
            }
            //RR-Respiratory Rate
            if (s.data.metric_id.equals(MDC_RESP_RATE.VALUE)) {
                createObservation("R.Rate","bpm", s.data.value,null,null);
            }
            //PEEP
            if (s.data.metric_id.equals(MDC_PRESS_AWAY.VALUE)) {
                createObservation("PEEP","cmH2O", s.data.value,null,null);
            }
            if (s.data.metric_id.equals(MDC_TIME_PD_INSPIRATORY.VALUE)) {
                createObservation("Insp-Time","s", s.data.value,null,null);
            }
            //InspO2
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_InspO2")) {
                createObservation("FiO2","%", s.data.value,null,null);
            }
            //PeakBreathingPressure
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_PeakBreathingPressure")) {
                createObservation("peakPressure","cmH2O", s.data.value,null,null);
            }
            //plateau pressure
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_PlateauPressure")) {
                createObservation("plateauPressure","cmH2O", s.data.value,null,null);
            }
            //Tidal Volume
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_TidalVolume")) {
                createObservation("volume","mL", s.data.value,null,null);
            }
            //Tidal Volume exp
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_VTemand")) {
                createObservation("expVolume","mL", s.data.value,null,null);
            }
            //RRmand
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_RRmand")) {
            }
            //MeanBreathingPressure
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_MeanBreathingPressure")) {
                createObservation("meanPressure","cmH2O", s.data.value,null,null);
            }
            //MinimalAirwayPressure
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_MinimalAirwayPressure")) {
            }

            //DRAEGER_MEASURED_CP1_ItoE_Ipart
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_ItoE_Ipart")) {
                ieIpart=s.data.value;
            }
            //DRAEGER_MEASURED_CP1_ItoE_Epart
            if (s.data.metric_id.equals("DRAEGER_MEASURED_CP1_ItoE_Epart")) {
                ieEpart=s.data.value;
            }
        });

    }

    private float settingIpart,settingEpart;
    public void separateStatisticSettingsValues(Map<Object, InstanceHolder<ice.Numeric>> settingUpdates){


        settingUpdates.values().stream().forEach(s-> {

            //Set apnea time
            if (s.data.metric_id.equals(MDC_TIME_PD_APNEA.VALUE)) {
                draegerStats.put("SAP",s.data.value);
            }
            //Set Inspiratory time
            if (s.data.metric_id.equals(MDC_TIME_PD_INSPIRATORY.VALUE)) {
                draegerStats.put("SIT",s.data.value);
            }
            //Freq-Intermittent positive pressure ventilation
            if (s.data.metric_id.equals(MDC_VENT_TIME_PD_PPV.VALUE)) {
                draegerStats.put("SIF",s.data.value);
            }


            //DRAEGER_SETTING_VT
            if (s.data.metric_id.equals("DRAEGER_SETTING_VT")) {
                draegerStats.put("SVT",s.data.value);
            }
            //DRAEGER_SETTING_ABSRamp
            if (s.data.metric_id.equals("DRAEGER_SETTING_ABSRamp")) {
                draegerStats.put("SRP",s.data.value);
            }
            //DRAEGER_SETTING_FlowTrigger
            if (s.data.metric_id.equals("DRAEGER_SETTING_FlowTrigger")) {
                draegerStats.put("SFT",s.data.value);
            }
            //DRAEGER_SETTING_PEEP
            if (s.data.metric_id.equals("DRAEGER_SETTING_PEEP")) {
                draegerStats.put("SPE",s.data.value);
            }
            //DRAEGER_SETTING_FrequencyIMV
            if (s.data.metric_id.equals("DRAEGER_SETTING_FrequencyIMV")) {
                draegerStats.put("SRR",s.data.value);
            }
            //DRAEGER_SETTING_Oxygen
            if (s.data.metric_id.equals("DRAEGER_SETTING_Oxygen")) {
                draegerStats.put("SOO",s.data.value);
            }


            //DRAEGER_SETTING_PressureSupportPressure
            if (s.data.metric_id.equals("DRAEGER_SETTING_PressureSupportPressure")) {
                draegerStats.put("SPS",s.data.value);
            }
            //patient height
            if (s.data.metric_id.equals("DRAEGER_SETTING_82H")) {
                draegerStats.put("SPH",s.data.value);
            }
            //Apnea Volume
            if (s.data.metric_id.equals("DRAEGER_SETTING_6EH")) {
                draegerStats.put("SVA",s.data.value);
            }
            //Apnea RR
            if (s.data.metric_id.equals("DRAEGER_SETTING_MinimalFrequency")) {
                draegerStats.put("SRA",s.data.value);
            }
            //DRAEGER_SETTING_InspiratoryPressure
            if (s.data.metric_id.equals("DRAEGER_SETTING_InspiratoryPressure")) {
                draegerStats.put("SIP",s.data.value);
            }


            //DRAEGER_SETTING_IPart
            if (s.data.metric_id.equals("DRAEGER_SETTING_IPart")) {
                settingIpart=s.data.value;
            }
            //DRAEGER_SETTING_EPart
            if (s.data.metric_id.equals("DRAEGER_SETTING_EPart")) {
                settingEpart=s.data.value;
            }
            //SIE
            draegerStats.put("SIE",settingIpart + ":" + settingEpart);

        });

        draegerStats.put("RST",false);
        jsonModal.setSts(draegerStats);

    }

    public void fetchAlarm(Medibus.Alarm alarm){

         if (alarm != null) {
                draegerAlarm = new LinkedHashMap<>();
                draegerAlarm.put("PRT", alarm.priority);
                draegerAlarm.put("K", alarm.alarmPhrase);

                draegerAlarmList.add(draegerAlarm);
            }
        jsonModal.setAlm(draegerAlarmList);
    }


}

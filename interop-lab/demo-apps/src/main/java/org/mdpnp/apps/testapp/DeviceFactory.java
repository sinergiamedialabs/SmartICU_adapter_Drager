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
package org.mdpnp.apps.testapp;

import org.mdpnp.devices.AbstractDevice;
import org.mdpnp.devices.DeviceDriverProvider;
import org.mdpnp.devices.DeviceDriverProvider.SpringLoadedDriver;
import org.mdpnp.devices.cpc.bernoulli.DemoBernoulli;
import org.mdpnp.devices.denver.mseries.MSeriesScale;
import org.mdpnp.devices.draeger.medibus.*;
import org.mdpnp.devices.fluke.prosim68.DemoProsim68;
import org.mdpnp.devices.ge.serial.DemoGESerial;
import org.mdpnp.devices.hospira.symbiq.DemoSymbiq;
import org.mdpnp.devices.ivy._450c.DemoIvy450C;
import org.mdpnp.devices.masimo.radical.DemoRadical7;
import org.mdpnp.devices.nellcor.pulseox.DemoN595;
import org.mdpnp.devices.nonin.pulseox.DemoNoninPulseOx;
import org.mdpnp.devices.oridion.capnostream.DemoCapnostream20;
import org.mdpnp.devices.philips.intellivue.DemoEthernetIntellivue;
import org.mdpnp.devices.philips.intellivue.DemoSerialIntellivue;
import org.mdpnp.devices.simulation.co2.SimCapnometer;
import org.mdpnp.devices.simulation.ecg.SimElectroCardioGram;
import org.mdpnp.devices.simulation.ibp.SimInvasivePressure;
import org.mdpnp.devices.simulation.multi.SimMultiparameter;
import org.mdpnp.devices.simulation.nibp.DemoSimulatedBloodPressure;
import org.mdpnp.devices.simulation.pulseox.EightSecFixedAvgSimPulseOximeter;
import org.mdpnp.devices.simulation.pulseox.FourSecFixedAvgSimPulseOximeter;
import org.mdpnp.devices.simulation.pulseox.FourSecNoSoftAvgSimPulseOximeter;
import org.mdpnp.devices.simulation.pulseox.InitialEightSecIceSettableAvgSimPulseOximeter;
import org.mdpnp.devices.simulation.pulseox.InitialEightSecOperSettableAvgSimPulseOximeter;
import org.mdpnp.devices.simulation.pulseox.SimPulseOximeter;
import org.mdpnp.devices.simulation.pump.SimControllablePump;
import org.mdpnp.devices.simulation.clcbp.SimControllableBPMonitor;
import org.mdpnp.devices.simulation.pump.SimInfusionPump;
import org.mdpnp.devices.simulation.temp.SimThermometer;
import org.mdpnp.devices.zephyr.biopatch.DemoBioPatch;
import org.mdpnp.rtiapi.data.EventLoop;
import org.springframework.context.support.AbstractApplicationContext;

import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.Subscriber;

import java.util.*;

public class DeviceFactory {

    static DeviceDriverProvider[] getAvailableDevices() {
        ServiceLoader<DeviceDriverProvider> l = ServiceLoader.load(DeviceDriverProvider.class);

        Collection<DeviceDriverProvider> all = new TreeSet<DeviceDriverProvider>(new Comparator<DeviceDriverProvider>() {
            @Override
            public int compare(DeviceDriverProvider o1, DeviceDriverProvider o2) {
                return o1.getDeviceType().toString().compareTo(o2.getDeviceType().toString());
            }
        });

        final Iterator<DeviceDriverProvider> iter = l.iterator();
        while (iter.hasNext()) {
            all.add(iter.next());
        }

        DeviceDriverProvider[] arr = all.toArray(new DeviceDriverProvider[all.size()]);
        return arr;
    }

    static DeviceDriverProvider getDeviceDriverProvider(String alias) {
        ServiceLoader<DeviceDriverProvider> l = ServiceLoader.load(DeviceDriverProvider.class);
        final Iterator<DeviceDriverProvider> iter = l.iterator();
        while (iter.hasNext()) {
            DeviceDriverProvider ddp = iter.next();
            if(alias.equals(ddp.getDeviceType().getAlias()))
                return ddp;
        }
        throw new IllegalArgumentException("Cannot resolve '" + alias + " to a known device");
    }

    public static class DraegerEvitaXLProvider extends SpringLoadedDriver {

        @Override
        public DeviceType getDeviceType(){
            return new DeviceType(ice.ConnectionType.Serial, "Dr\u00E4ger", "EvitaXL", new String[] { "DraegerEvitaXL", "Dr\u00E4gerEvitaXL" }, 1);
        }

        @Override
        public AbstractDevice newInstance(AbstractApplicationContext context) throws Exception {
            EventLoop eventLoop = context.getBean("eventLoop", EventLoop.class);
            Subscriber subscriber = context.getBean("subscriber", Subscriber.class);
            Publisher publisher = context.getBean("publisher", Publisher.class);
            return new DemoEvitaXL(subscriber, publisher, eventLoop);

        }
    }

    public static class DraegerV500Provider extends SpringLoadedDriver {

        @Override
        public DeviceType getDeviceType(){
            return new DeviceType(ice.ConnectionType.Serial, "Dr\u00E4ger", "V500", new String[] { "DraegerV500", "Dr\u00E4gerV500" }, 1);
        }

        @Override
        public AbstractDevice newInstance(AbstractApplicationContext context) throws Exception {
            EventLoop eventLoop = context.getBean("eventLoop", EventLoop.class);
            Subscriber subscriber = context.getBean("subscriber", Subscriber.class);
            Publisher publisher = context.getBean("publisher", Publisher.class);
            return new DemoV500(subscriber, publisher, eventLoop);

        }
    }

    public static class DraegerSavinaProvider extends SpringLoadedDriver {

        @Override
        public DeviceType getDeviceType(){
            return new DeviceType(ice.ConnectionType.Serial, "Dr\u00E4ger", "Savina", new String[] { "DraegerSavina", "Dr\u00E4gerSavina" }, 1);
        }

        @Override
        public AbstractDevice newInstance(AbstractApplicationContext context) throws Exception {
            EventLoop eventLoop = context.getBean("eventLoop", EventLoop.class);
            Subscriber subscriber = context.getBean("subscriber", Subscriber.class);
            Publisher publisher = context.getBean("publisher", Publisher.class);
            return new DemoSavina(subscriber, publisher, eventLoop);

        }
    }

    public static class DraegerV500_38400Provider extends SpringLoadedDriver {

        @Override
        public DeviceType getDeviceType(){
            return new DeviceType(ice.ConnectionType.Serial, "Dr\u00E4ger", "V500", new String[] { "DraegerV500_38400", "Dr\u00E4gerV500_38400" }, 1);
        }

        @Override
        public AbstractDevice newInstance(AbstractApplicationContext context) throws Exception {
            EventLoop eventLoop = context.getBean("eventLoop", EventLoop.class);
            Subscriber subscriber = context.getBean("subscriber", Subscriber.class);
            Publisher publisher = context.getBean("publisher", Publisher.class);
            return new DemoV500_38400(subscriber, publisher, eventLoop);

        }
    }

    public static class DraegerEvita4Provider extends SpringLoadedDriver {

        @Override
        public DeviceType getDeviceType(){
            return new DeviceType(ice.ConnectionType.Serial, "Dr\u00E4ger", "Evita4", new String[] { "DraegerEvita4", "Dr\u00E4gerEvita4" }, 1);
        }

        @Override
        public AbstractDevice newInstance(AbstractApplicationContext context) throws Exception {
            EventLoop eventLoop = context.getBean("eventLoop", EventLoop.class);
            Subscriber subscriber = context.getBean("subscriber", Subscriber.class);
            Publisher publisher = context.getBean("publisher", Publisher.class);
            return new DemoEvita4(subscriber, publisher, eventLoop);

        }
    }

}

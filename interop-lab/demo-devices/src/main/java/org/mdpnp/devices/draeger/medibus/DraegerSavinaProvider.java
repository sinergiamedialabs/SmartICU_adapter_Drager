package org.mdpnp.devices.draeger.medibus;

import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.Subscriber;
import org.mdpnp.devices.AbstractDevice;
import org.mdpnp.devices.DeviceDriverProvider;
import org.mdpnp.rtiapi.data.EventLoop;
import org.springframework.context.support.AbstractApplicationContext;

/**
 *
 */
public class DraegerSavinaProvider extends DeviceDriverProvider.SpringLoadedDriver {

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

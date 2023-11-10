package org.mdpnp.devices.simulation.co2;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mdpnp.common.graph.CommonGraphModal;

public class CapnometerGraphModal {

    public CommonGraphModal getCo() {
        return co;
    }

    @JsonProperty("CO")
    public void setCo(CommonGraphModal co) {
        this.co = co;
    }

    private CommonGraphModal co;
}

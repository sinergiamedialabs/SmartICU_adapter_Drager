package org.mdpnp.devices.simulation.multi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mdpnp.common.graph.CommonGraphModal;
import org.mdpnp.common.graph.ECGGraphModal;

public class GraphModal {


    private ECGGraphModal ecg;
    private CommonGraphModal plt;
    private CommonGraphModal co;
    private CommonGraphModal ibp;

    public ECGGraphModal getEcg() {
        return ecg;
    }

    @JsonProperty("ECG")
    public void setEcg(ECGGraphModal ecg) {
        this.ecg = ecg;
    }

    public CommonGraphModal getPlt() {
        return plt;
    }

    @JsonProperty("PLT")
    public void setPlt(CommonGraphModal plt) {
        this.plt = plt;
    }

    public CommonGraphModal getCo() {
        return co;
    }

    @JsonProperty("CO")
    public void setCo(CommonGraphModal co) {
        this.co = co;
    }

    public CommonGraphModal getIbp() {
        return ibp;
    }

    @JsonProperty("IBP")
    public void setIbp(CommonGraphModal ibp) {
        this.ibp = ibp;
    }


}

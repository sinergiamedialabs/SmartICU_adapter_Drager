package org.mdpnp.common.graph;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CommonGraphModal {

    private List<Float> g;
   // private long frq;

    private float frq;

    public float getFrq() {
        return frq;
    }

    @JsonProperty("FRQ")
    public void setFrq(float frq) {
        this.frq = frq;
    }

    public List<Float> getG() {
        return g;
    }
    @JsonProperty("G")
    public void setG(List<Float> g) {
        this.g = g;
    }

   /* public long getFrq() {
        return frq;
    }

    @JsonProperty("FRQ")
    public void setFrq(long frq) {
        this.frq = frq;
    }*/


}

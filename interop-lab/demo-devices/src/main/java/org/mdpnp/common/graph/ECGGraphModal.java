package org.mdpnp.common.graph;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ECGGraphModal {

    private List<Float> ga;
    private List<Float> gb;
    private List<Float> gc;
   // private long frq;

    private float frq;

    public float getFrq() {
        return frq;
    }

    @JsonProperty("FRQ")
    public void setFrq(float frq) {
        this.frq = frq;
    }

    public List<Float> getGa() {
        return ga;
    }

    @JsonProperty("GA")
    public void setGa(List<Float> ga) {
        this.ga = ga;
    }

    public List<Float> getGb() {
        return gb;
    }

    @JsonProperty("GB")
    public void setGb(List<Float> gb) {
        this.gb = gb;
    }

    public List<Float> getGc() {
        return gc;
    }

    @JsonProperty("GC")
    public void setGc(List<Float> gc) {
        this.gc = gc;
    }


   /* public long getFrq() {
        return frq;
    }

    @JsonProperty("FRQ")
    public void setFrq(long frq) {
        this.frq = frq;
    }*/



}

package org.mdpnp.devices.simulation.multi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Statistics {

    private float bpm;
    private float spo;
    private float hr;
    private float rre;
    private float rrc;
    private float etc;
    private float sys;
    private float dia;
    private boolean rst;


    public float getBpm() {
        return bpm;
    }
    @JsonProperty("BPM")
    public void setBpm(float bpm) {
        this.bpm = bpm;
    }

    public float getSpo() {
        return spo;
    }
    @JsonProperty("SPO")
    public void setSpo(float spo) {
        this.spo = spo;
    }

    public float getHr() {
        return hr;
    }
    @JsonProperty("HR")
    public void setHr(float hr) {
        this.hr = hr;
    }

    public float getRre() {
        return rre;
    }
    @JsonProperty("RRE")
    public void setRre(float rre) {
        this.rre = rre;
    }

    public float getRrc() {
        return rrc;
    }
    @JsonProperty("RRC")
    public void setRrc(float rrc) {
        this.rrc = rrc;
    }

    public float getEtc() {
        return etc;
    }
    @JsonProperty("ETC")
    public void setEtc(float etc) {
        this.etc = etc;
    }

    public float getSys() {
        return sys;
    }
    @JsonProperty("SYS")
    public void setSys(float sys) {
        this.sys = sys;
    }

    public float getDia() {
        return dia;
    }
    @JsonProperty("DIA")
    public void setDia(float dia) {
        this.dia = dia;
    }

    public boolean isRst() {
        return rst;
    }
    @JsonProperty("RST")
    public void setRst(boolean rst) {
        this.rst = rst;
    }


}

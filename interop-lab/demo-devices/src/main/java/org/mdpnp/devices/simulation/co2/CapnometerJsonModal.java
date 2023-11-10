package org.mdpnp.devices.simulation.co2;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class CapnometerJsonModal {

    private long ts;
    private String dev;
    private String dtp;
    private String con;
    private CapnometerGraphModal grp;
    private Map<String,Object> sts;

    public Map<String, Object> getSts() {
        return sts;
    }

    @JsonProperty("STS")
    public void setSts(Map<String, Object> sts) {
        this.sts = sts;
    }

    public CapnometerGraphModal getGrp() {
        return grp;
    }
    @JsonProperty("GRP")
    public void setGrp(CapnometerGraphModal grp) {
        this.grp = grp;
    }



    public long getTs() {
        return ts;
    }

    @JsonProperty("TS")
    public void setTs(long ts) {
        this.ts = ts;
    }

    public String getDev() {
        return dev;
    }

    @JsonProperty("DEV")
    public void setDev(String dev) {
        this.dev = dev;
    }

    public String getDtp() {
        return dtp;
    }

    @JsonProperty("DTP")
    public void setDtp(String dtp) {
        this.dtp = dtp;
    }

    public String getCon() {
        return con;
    }

    @JsonProperty("CON")
    public void setCon(String con) {
        this.con = con;
    }


}

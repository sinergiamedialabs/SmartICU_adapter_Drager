package org.mdpnp.devices.draeger.medibus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

public class DraegerJsonModal {

    private long ts;
    private String dev;
    private String dtp;

    private Map<String,Object> sts;
    private Map<String,Object> grp;
    private Set<Map<String,Object>> alm;

    public Set<Map<String, Object>> getAlm() {
        return alm;
    }

    @JsonProperty("ALM")
    public void setAlm(Set<Map<String, Object>> alm) {
        this.alm = alm;
    }

    public Map<String, Object> getSts() {
        return sts;
    }

    @JsonProperty("STS")
    public void setSts(Map<String, Object> sts) {
        this.sts = sts;
    }

    public Map<String, Object> getGrp() {
        return grp;
    }
    @JsonProperty("GRP")
    public void setGrp(Map<String, Object> grp) {
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


}

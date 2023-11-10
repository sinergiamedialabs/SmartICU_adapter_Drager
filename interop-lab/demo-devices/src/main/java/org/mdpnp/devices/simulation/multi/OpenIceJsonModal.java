package org.mdpnp.devices.simulation.multi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class OpenIceJsonModal {

    private long ts;
    private String dev;
    private String dtp;
    private String con;
    //private GraphModal grp;

    public Map<String, Object> getGrp() {
        return grp;
    }

    public void setGrp(Map<String, Object> grp) {
        this.grp = grp;
    }

    private Map<String,Object> grp;


    private Statistics sts;
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

  /*  public GraphModal getGrp() {
        return grp;
    }

    @JsonProperty("GRP")
    public void setGrp(GraphModal grp) {
        this.grp = grp;
    }*/

    public Statistics getSts() {
        return sts;
    }

    @JsonProperty("STS")
    public void setSts(Statistics sts) {
        this.sts = sts;
    }






}

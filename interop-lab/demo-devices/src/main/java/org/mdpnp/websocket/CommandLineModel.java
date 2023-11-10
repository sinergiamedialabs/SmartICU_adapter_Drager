package org.mdpnp.websocket;

public class CommandLineModel {

    public static String deviceId;
    public static String devicePassword;

    public static String getDeviceId() {
        return deviceId;
    }

    public static void setDeviceId(String deviceId) {
        CommandLineModel.deviceId = deviceId;
    }

    public static String getDevicePassword() {
        return devicePassword;
    }

    public static void setDevicePassword(String devicePassword) {
        CommandLineModel.devicePassword = devicePassword;
    }

}

package org.mdpnp.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;

public class WebsocketUtils {

    public WebSocketClient mWs;

    public WebSocketClient connectWebsocket(String url){

        try {
            mWs = new WebSocketClient(new URI(url), new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("opened connection");
                }

                @Override
                public void onMessage(String message) {
                //    JSONObject obj = new JSONObject(message);
                  //  String data = obj.getString("data");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("closed connection  :" + reason);
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }

            };
            //open websocket
            mWs.connect();

        }catch (Exception e){

        }
        return  mWs;

    }

    public void sendMessageToWebsocket(String json){
        try {

           //send message
            mWs.send(json);
            System.out.println("message sent");


        }catch (Exception e){
           // System.out.println("error while send message");
        }

    }
}

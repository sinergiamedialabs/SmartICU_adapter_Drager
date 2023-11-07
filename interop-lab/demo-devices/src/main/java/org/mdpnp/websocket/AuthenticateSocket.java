package org.mdpnp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.util.*;

public class AuthenticateSocket {

    public String generateTokenByDeviceIdAndPassword(String id,String password){

        try {

            // load property value
            Properties prop = new Properties();
            String propFileName = "websocket-info.properties";
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            }

            String url = prop.getProperty("ws-auth-url");
            String authUrl = url+"/api/authenticate_device/";

            System.out.println("Connecting authenticate device....");

            // Http connection
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(authUrl);

            // Request parameters and other properties.
            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("id", id));
            params.add(new BasicNameValuePair("password", password));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            Map<String, Object> content = new ObjectMapper().readValue(response.getEntity().getContent(), HashMap.class);
            return content.get("token").toString();

        }catch (Exception e){

            System.out.println(e);
        }
        return  null;
    }


}

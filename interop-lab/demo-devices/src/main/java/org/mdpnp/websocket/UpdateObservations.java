package org.mdpnp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.util.*;

public class UpdateObservations {
    public int SendObservations(String message){

        try {
            Properties prop = new Properties();
            String propFileName = "websocket-info.properties";
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            }

            String url = prop.getProperty("update-observation-url");

            System.out.println("Sending Observation");

            // Http connection
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(url);

            // Request parameters and other properties.
            httppost.setEntity(new StringEntity(message, ContentType.APPLICATION_JSON));

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
//            Map<String, Object> content = new ObjectMapper().readValue(response.getEntity().getContent(), HashMap.class);
//            return content.get("token").toString();
            return response.getStatusLine().getStatusCode();

        }catch (Exception e){

            System.out.println(e);
        }
        return 0;
    }
}

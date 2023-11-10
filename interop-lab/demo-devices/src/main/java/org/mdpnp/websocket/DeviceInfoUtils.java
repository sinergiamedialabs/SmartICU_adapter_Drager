package org.mdpnp.websocket;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.HashMap;

public class DeviceInfoUtils {

    public HashMap<String,String> createDeviceInfoPanel(){

        HashMap<String,String> values=new HashMap<>();

        UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("ARIAL",Font.PLAIN,20)));
        JPanel pnl = new JPanel();
        pnl.setPreferredSize(new Dimension(640, 120));

        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        JLabel labelId = new JLabel("Device Id");
        labelId.setFont(new Font("Calibri", Font.PLAIN, 18));
        labels.add(labelId,BorderLayout.NORTH);

        labels.add(Box.createVerticalStrut(20));

        JLabel labelPassword = new JLabel("Password");
        labelPassword.setFont(new Font("Calibri", Font.PLAIN, 18));
        labels.add(labelPassword,BorderLayout.CENTER);

        pnl.add(labels, BorderLayout.WEST);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JTextField id = new JTextField();
        id.setPreferredSize( new Dimension( 500, 34 ) );
        controls.add(id, BorderLayout.NORTH);

        controls.add(Box.createVerticalStrut(20));

        JPasswordField password = new JPasswordField();
        password.setPreferredSize( new Dimension( 500, 34 ) );
        controls.add(password, BorderLayout.CENTER);

        pnl.add(controls, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(null, pnl, "Device Info", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {

            if ((id.getText() != null) && (id.getText().length() > 0)
                    && (String.valueOf(password.getPassword()) != null) && (String.valueOf(password.getPassword()).length() > 0)) {
                values.put("id",id.getText());
                values.put("password",String.valueOf(password.getPassword()));

                return values;

            }else{

                System.out.println("No value ");
                System.exit(0);
            }

        } else {
            System.out.println(" cancelled");
            System.exit(0);
        }
        return  values;
    }




}

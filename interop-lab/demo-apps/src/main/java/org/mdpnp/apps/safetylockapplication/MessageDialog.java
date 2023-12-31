package org.mdpnp.apps.safetylockapplication;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MessageDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	ImageButton okay;
	
	public MessageDialog(String message, Dimension dimension)
	{
		int useWidth = dimension.width;
		int useHeight = dimension.height;
		setSize(dimension);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
	    final Dimension screenSize = toolkit.getScreenSize();
	    final int x = (screenSize.width - useWidth) / 2;
	    final int y = (screenSize.height - useHeight) / 2;
	    setLocation(x, y);
	    setUndecorated(true);
	    getRootPane().setBorder(BorderFactory.createLineBorder(Color.white, 2, true));
		
		okay = new ImageButton(Resources.loadImage("OkButton.png"));
		okay.addActionListener(this);
		
		JLabel messageLabel = new JLabel(message);
		Font font = new Font("Arial", Font.PLAIN, 20);
		messageLabel.setFont(font);
		messageLabel.setOpaque(true);
		messageLabel.setForeground(Color.GRAY);
		messageLabel.setBackground(Color.BLACK);
		messageLabel.setAlignmentY(CENTER_ALIGNMENT);
		
		JPanel messagePanel = new JPanel();
		messagePanel.setBackground(Color.BLACK);
		messagePanel.setLayout(new GridBagLayout());
		messagePanel.add(messageLabel);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(Resources.physiologicalDisplayPanelBackgroundColor);
		buttonPanel.setLayout(new FlowLayout((FlowLayout.CENTER), 0, 0));
		buttonPanel.add(okay);
		
		setLayout(new BorderLayout());
		add(messagePanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
	}
	
	public void showDialog()
	{
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		dispose();
	}

}

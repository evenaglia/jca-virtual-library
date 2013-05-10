package com.jivesoftware.jcalibrary;

import net.venaglia.realms.common.util.Pair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Properties;

/**
 * User: ed
 * Date: 5/10/13
 * Time: 7:30 AM
 */
public class LoginForm {

    private final Properties props;
    private JFrame frame;

    private JTextField username;
    private JPasswordField password;

    private boolean visible = false;
    private boolean ready = false;
    private Pair<String,String> credentials;

    public LoginForm(Properties props) {
        this.props = props;
    }

    public synchronized Pair<String,String> getCredentials() {
        if (!ready) {
            showForm();
            try {
                wait();
            } catch (InterruptedException e) {
                // don't care
            }
        }
        return credentials;
    }

    public synchronized void showForm() {
        if (!visible) {
            visible = true;
            username = new JTextField(props.getProperty("username", ""));
            password = new JPasswordField(props.getProperty("password", ""));

            JButton button = new JButton();
            button.setDefaultCapable(true);
            button.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    credentials = new Pair<String,String>(username.getText(), new String(password.getPassword()));
                    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    frame.dispose();
                    visible = false;
                    ready = true;
                    synchronized (LoginForm.this) {
                        LoginForm.this.notifyAll();
                    }
                }
            });
            button.setText("Login");

            JLabel label1 = new JLabel("Username:");
            JLabel label2 = new JLabel("Password:");

            JPanel panel = new JPanel();
            panel.add(label1);
            panel.add(username);
            panel.add(label2);
            panel.add(password);
            panel.add(button);

            frame = new JFrame("JCA Login");
            frame.setSize(250, 140);
            frame.add(panel);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);

            label1.setLocation(25,25);
            label2.setLocation(25,55);
            username.setLocation(100,20);
            username.setSize(130,30);
            password.setLocation(100,50);
            password.setSize(130,30);
            button.setLocation(150,80);
            button.setSize(80,30);
        }
    }

    public static void main(String[] args) {
        LoginForm loginForm = new LoginForm(new Properties());
        System.out.println(loginForm.getCredentials());
    }
}

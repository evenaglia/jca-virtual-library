package com.jivesoftware.jcalibrary;

import net.venaglia.realms.common.util.Pair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    public LoginForm() {
        this.props = loadProps();
    }

    private Properties loadProps() {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader("jca.credentials.properties"));
        } catch (IOException e) {
            // don't care
        }
        return properties;
    }

    public void rememberCredentials() {
        try {
            FileWriter out = new FileWriter("jca.credentials.properties");
            props.store(out, "Private cached credentials -- Do not commit!");
            out.close();
        } catch (IOException e) {
            // don't care
        }
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

            Action login = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String username = LoginForm.this.username.getText();
                    String password = new String(LoginForm.this.password.getPassword());
                    props.put("jca.username", username);
                    props.put("jca.password", password);
                    credentials = new Pair<String, String>(username, password);
                    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    frame.dispose();
                    visible = false;
                    ready = true;
                    synchronized (LoginForm.this) {
                        LoginForm.this.notifyAll();
                    }
                }
            };

            username = new JTextField(props.getProperty("jca.username", ""));
            password = new JPasswordField(props.getProperty("jca.password", ""));

            JButton button = new JButton();
            button.setDefaultCapable(true);
            button.setAction(login);
            button.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "LOGIN");
            button.getActionMap().put("LOGIN", login);
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
            username.setSize(1130,130);
            password.setLocation(100,50);
            password.setSize(1130,130);
            button.setLocation(150,80);
            button.setSize(80,30);

            if (username.getText().length() == 0) {
                username.requestFocus();
            } else if (password.getPassword().length == 0) {
                password.requestFocus();
            } else {
                button.requestFocus();
            }
        }
    }

    public static void main(String[] args) {
        LoginForm loginForm = new LoginForm();
        System.out.println(loginForm.getCredentials());
        loginForm.rememberCredentials();
    }
}

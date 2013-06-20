package com.jivesoftware.jcalibrary.util;

import java.awt.Desktop;

/**
 * User: ed
 * Date: 5/10/13
 * Time: 2:06 PM
 */
public class Browser {

    public static void openURL(String url) {
        if (!java.awt.Desktop.isDesktopSupported()) {
            System.err.println("Desktop is not supported (fatal)");
            return;
        }
        Desktop desktop = java.awt.Desktop.getDesktop();
        if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            System.err.println("Desktop doesn't support the browse action (fatal)");
            return;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            desktop.browse(uri);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}

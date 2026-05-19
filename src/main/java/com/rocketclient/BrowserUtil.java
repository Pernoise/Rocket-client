package com.rocketclient;

public class BrowserUtil {

    public static void open(String url) {
        try {
            new ProcessBuilder("xdg-open", url).start();
        } catch (Exception e) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                System.out.println("Could not open URL: " + url);
            }
        }
    }
}

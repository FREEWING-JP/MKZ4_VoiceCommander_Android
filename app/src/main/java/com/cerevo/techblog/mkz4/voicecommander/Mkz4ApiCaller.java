package com.cerevo.techblog.mkz4.voicecommander;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class Mkz4ApiCaller {
    enum Command {
        None(""),
        Stop("stop"),
        Forward("forward"),
        Back("back"),
        Left("left"),
        Right("right"),
        LeftForward("leftforward"),
        RightForward("rightforward"),
        LeftBack("leftback"),
        RightBack("rightback");

        private final String command;

        Command(final String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

    private static final String API_URL = "http://192.168.4.1:8080";
    private static Mkz4ApiCaller sInstance = new Mkz4ApiCaller();

    static Mkz4ApiCaller getInstance() {
        return sInstance;
    }

    void sendCommand(Command command) {
        get(API_URL + "/" + command.getCommand());
    }

    private void get(final String urlStr) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(1000);
                    urlConnection.setReadTimeout(200);
                    try {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }).start();
    }
}

package com.cerevo.techblog.mkz4.voicecommander;

import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.mlkcca.client.DataElementValue;
import com.mlkcca.client.DataStore;
import com.mlkcca.client.MilkCocoa;
import com.mlkcca.client.SystemEventListener;


// Reference
// https://github.com/milk-cocoa/android-examples/blob/master/MilkChat2/app/src/main/java/com/mlkcca/android/sample/milkchat/MainActivity.java

class MilkCocoaClient {
    private static final String TAG = "MilkCocoaClient";

    /**
     * Your Milkcocoa Setup
     */
    private static final String milkcocoaAppID = "milkcocoa_app_id";
    private static final String milkcocoaDataStore = "data";

    private ArrayAdapter<String> adapter;
    private MilkCocoa milkcocoa;
    private DataStore messagesDataStore;
    private boolean isConnected = false;

    private static MilkCocoaClient sInstance = new MilkCocoaClient();

    static MilkCocoaClient getInstance() {
        return sInstance;
    }

    void connect() {
        try {
            this.milkcocoa = new MilkCocoa(milkcocoaAppID);
            this.milkcocoa.setSystemEventListener(new SystemEventListener() {
                @Override
                public void onConnectionLost() {
                    Log.d(TAG, "connection lost.");
                    MilkCocoaClient.this.isConnected = false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        this.messagesDataStore = this.milkcocoa.dataStore(milkcocoaDataStore);
        this.isConnected = true;
        Log.d(TAG, "milkcocoa connected.");
    }

    void sendCommand(Mkz4ApiCaller.Command command) {
        if (!this.isConnected) {
            connect();
        }
        DataElementValue params = new DataElementValue();
        params.put("com", getMilkcocoaCommand(command));
        this.messagesDataStore.push(params);
        Log.d(TAG, "milkcocoa send command: " + command);
    }

    // milkcocoa_esp8266 用に、コマンドを変換する
    // https://github.com/cerevo/MKZ4/tree/master/custom/milkcocoa_esp8266
    private int getMilkcocoaCommand(Mkz4ApiCaller.Command command) {
        switch (command) {
            case Forward:
                return 2;
            case LeftForward:
                return 1;
            case RightForward:
                return 3;
            case Stop:
                return 0;
            case LeftBack:
                return 7;
            case Back:
                return 8;
            case RightBack:
                return 9;
            case Left:
                return 4;
            case Right:
                return 5;
            default:
                return 0;
        }
    }
}

package com.cerevo.techblog.mkz4.voicecommander;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;


/**
 * Reference: http://kivantium.hateblo.jp/entry/2016/02/29/191901
 */

// RecognitionListenerの定義
// 中が空でも全てのメソッドを書く必要がある
class MyRecognitionListener implements RecognitionListener {

    private RecognitionResultReceiver mReceiver; // 弱い参照
    private static final String TAG = "MyRecognitionListener";

    MyRecognitionListener(RecognitionResultReceiver receiver) {
        mReceiver = receiver;
    }

    // 話し始めたときに呼ばれる
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningofSpeech");
    }

    // 結果に対する反応などで追加の音声が来たとき呼ばれる
    // しかし呼ばれる保証はないらしい
    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived");
    }

    // 話し終わった時に呼ばれる
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
    }

    // ネットワークエラーか認識エラーが起きた時に呼ばれる
    public void onError(int error) {
        String reason = "";
        switch (error) {
            // Audio recording error
            case SpeechRecognizer.ERROR_AUDIO:
                reason = "ERROR_AUDIO";
                break;
            // Other client side errors
            case SpeechRecognizer.ERROR_CLIENT:
                reason = "ERROR_CLIENT";
                break;
            // Insufficient permissions
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                reason = "ERROR_INSUFFICIENT_PERMISSIONS";
                break;
            // 	Other network related errors
            case SpeechRecognizer.ERROR_NETWORK:
                reason = "ERROR_NETWORK";
                    /* ネットワーク接続をチェックする処理をここに入れる */
                break;
            // Network operation timed out
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                reason = "ERROR_NETWORK_TIMEOUT";
                break;
            // No recognition result matched
            case SpeechRecognizer.ERROR_NO_MATCH:
                reason = "ERROR_NO_MATCH";
                break;
            // RecognitionService busy
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                reason = "ERROR_RECOGNIZER_BUSY";
                break;
            // Server sends error status
            case SpeechRecognizer.ERROR_SERVER:
                reason = "ERROR_SERVER";
                    /* ネットワーク接続をチェックをする処理をここに入れる */
                break;
            // No speech input
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                reason = "ERROR_SPEECH_TIMEOUT";
                break;
        }
        if (mReceiver != null) {
            mReceiver.onError(reason);
        }
    }

    // 将来の使用のために予約されている
    public void onEvent(int eventType, Bundle params) {
    }

    // 部分的な認識結果が利用出来るときに呼ばれる
    // 利用するにはインテントでEXTRA_PARTIAL_RESULTSを指定する必要がある
    public void onPartialResults(Bundle partialResults) {
    }

    // 音声認識の準備ができた時に呼ばれる
    public void onReadyForSpeech(Bundle params) {
        if (mReceiver != null) {
            mReceiver.onReady();
        }
    }

    // 認識結果が準備できた時に呼ばれる
    public void onResults(Bundle results) {
        // 結果をArrayListとして取得
        ArrayList results_array = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
        String resultsString = "";
        if (results_array != null) {
            // 取得した文字列を結合
            for (int i = 0; i < results_array.size(); i++) {
                resultsString += results_array.get(i) + ";";
            }
        }
        if (mReceiver != null) {
            mReceiver.onResult(resultsString);
        }
    }

    // サウンドレベルが変わったときに呼ばれる
    // 呼ばれる保証はない
    public void onRmsChanged(float rmsdB) {
    }


    static interface RecognitionResultReceiver {
        public void onError(String reason);
        public void onReady();
        public void onResult(String result);
    }
}

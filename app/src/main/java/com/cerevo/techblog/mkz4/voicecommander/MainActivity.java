package com.cerevo.techblog.mkz4.voicecommander;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reference: http://kivantium.hateblo.jp/entry/2016/02/29/191901
 */

public class MainActivity extends AppCompatActivity {
    private enum Direction {
        Neutral, Back, Forward;
    }
    // 現在の進行方向
    private Direction mDirection = Direction.Neutral;

    // Android の speech recognition service を使うためのインスタンス
    private SpeechRecognizer mSpeechRecognizer;

    // MKZ4に送信するコマンドと、それに対応するキーワードの組み合わせ
    // initCommandSet メソッドで初期化する
    private HashMap<Mkz4ApiCaller.Command, ArrayList<String>> mCommandSet;

    private TextView mCommandTextView;
    private TextView mResultTextView;
    private Button mRecordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCommandTextView = (TextView) findViewById(R.id.command_text_view);
        mResultTextView = (TextView) findViewById(R.id.result_text_view);
        mRecordButton = (Button) findViewById(R.id.record_button);

        mResultTextView.setText(R.string.speech_recognition_please_start);
        mRecordButton.setText(R.string.speech_recognition_start);
        mCommandTextView.setText("");

        mRecordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startListening();
                        break;
                    case MotionEvent.ACTION_UP:
                        stopListening(false);
                        break;
                }
                return false;
            }
        });

        initCommandSet();
        MilkCocoaClient.getInstance().connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                //許可されていない時の処理
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    //拒否された時 Permissionが必要な理由を表示して再度許可を求めたり、機能を無効にしたりします。
                    Toast.makeText(getApplicationContext(), getString(R.string.speech_recognition_please_grant),
                            Toast.LENGTH_LONG).show();
                }

                //まだ許可を求める前の時、許可を求めるダイアログを表示します。
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
            }
        }
    }

    @Override
    protected void onPause() {
        stopListening(true);
        super.onPause();
    }

    /**
     * 音声認識を開始する
     */
    private void startListening() {
        try {
            // インテントの作成
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            // 言語モデル指定
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            }
            getSpeechRecognizer().startListening(intent);

            mRecordButton.setText(R.string.speech_recognition_stop);
        } catch (Exception ex) {
            Log.d("", ex.getMessage());
            mResultTextView.setText(R.string.speech_recognition_failed_to_start);
        }
    }

    /**
     * SpeechRecognizerのインスタンスを取得する。まだ初期化されていなければ初期化する。
     *
     * @return SpeechRecognizer
     */
    private SpeechRecognizer getSpeechRecognizer() {
        if (mSpeechRecognizer != null) {
            return mSpeechRecognizer;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_recognition_not_available),
                    Toast.LENGTH_LONG).show();
            finish();
        }

        MyRecognitionListener.RecognitionResultReceiver receiver = new MyRecognitionListener.RecognitionResultReceiver() {
            @Override
            public void onError(String reason) {
                Log.d("", reason);
                mRecordButton.setText(R.string.speech_recognition_start);
                if (mResultTextView.getText() == getString(R.string.speech_recognition_listening)) {
                    mResultTextView.setText(R.string.speech_recognition_please_start);
                }
            }

            @Override
            public void onReady() {
                mResultTextView.setText(R.string.speech_recognition_listening);
            }

            @Override
            public void onResult(String result) {
                MainActivity.this.onResult(result);
            }
        };
        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new MyRecognitionListener(receiver));
        mSpeechRecognizer = speechRecognizer;
        return mSpeechRecognizer;
    }

    /**
     * 音声認識を終了する
     *
     * @param finish アプリ終了時かどうか。
     */
    private void stopListening(boolean finish) {
        if (mSpeechRecognizer != null) {
            if (finish) {
                mSpeechRecognizer.destroy();
                mSpeechRecognizer = null;
            } else {
                mSpeechRecognizer.stopListening();
            }
        }
        mRecordButton.setText(R.string.speech_recognition_start);
    }

    /**
     * 音声認識した文字列を受け取る
     *
     * @param text 音声認識した文字列
     */
    private void onResult(String text) {
        mRecordButton.setText(R.string.speech_recognition_start);
        mResultTextView.setText(text);

        sendCommand(extractCommand(text));
    }

    /**
     * 音声認識した文字列から、キーワードを見つけて、MKZ4に送信するコマンドを選択する
     *
     * @param text 音声認識した文字列
     * @return MKZ4に送信するコマンド
     */
    private Mkz4ApiCaller.Command extractCommand(String text) {
        Mkz4ApiCaller.Command command = Mkz4ApiCaller.Command.None;
        if (mCommandSet == null) {
            return command;
        }

        for (Map.Entry<Mkz4ApiCaller.Command, ArrayList<String>> entry : mCommandSet.entrySet()) {
            Mkz4ApiCaller.Command key = entry.getKey();
            ArrayList<String> value = entry.getValue();
            int maxIndex = -1;
            for (String c : value) {
                int index = text.lastIndexOf(c);
                if (maxIndex < index) {
                    command = key;
                    maxIndex = index;
                }
            }
        }

        return command;
    }

    /**
     * MKZ4にコマンドを送信する
     *
     * @param command MKZ4に送信するコマンド
     */
    private void sendCommand(Mkz4ApiCaller.Command command) {
        Mkz4ApiCaller.Command sendingCommand = command;

        switch (command) {
            case Forward:
                sendingCommand = Mkz4ApiCaller.Command.Forward;
                mDirection = Direction.Forward;
                break;
            case Back:
                sendingCommand = Mkz4ApiCaller.Command.Back;
                mDirection = Direction.Back;
                break;
            case Right:
                switch (mDirection) {
                    case Neutral:
                        sendingCommand = Mkz4ApiCaller.Command.Right;
                        break;
                    case Back:
                        sendingCommand = Mkz4ApiCaller.Command.RightBack;
                        break;
                    case Forward:
                        sendingCommand = Mkz4ApiCaller.Command.RightForward;
                        break;
                }
                break;
            case Left:
                switch (mDirection) {
                    case Neutral:
                        sendingCommand = Mkz4ApiCaller.Command.Left;
                        break;
                    case Back:
                        sendingCommand = Mkz4ApiCaller.Command.LeftBack;
                        break;
                    case Forward:
                        sendingCommand = Mkz4ApiCaller.Command.LeftForward;
                        break;
                }
                break;
            case Stop:
                sendingCommand = Mkz4ApiCaller.Command.Stop;
                mDirection = Direction.Neutral;
                break;
            default:
                break;
        }

        if (sendingCommand != Mkz4ApiCaller.Command.None) {
            //Mkz4ApiCaller.getInstance().sendCommand(sendingCommand);
            MilkCocoaClient.getInstance().sendCommand(sendingCommand);
        }
        mCommandTextView.setText(sendingCommand.getCommand());
    }

    /**
     * MKZ4に送信するコマンドと、それに対応するキーワードの組み合わせを初期化する
     */
    private void initCommandSet() {
        mCommandSet = new HashMap<>();
        Locale locale = Locale.getDefault();
        if (locale.equals(Locale.JAPAN)) {
            mCommandSet.put(Mkz4ApiCaller.Command.Forward,
                    new ArrayList<>(Arrays.asList("進", "行", "いけ", "スタート", "すすめ", "池", "前", "ゴー")));
            mCommandSet.put(Mkz4ApiCaller.Command.Back,
                    new ArrayList<>(Arrays.asList("後", "下", "バック", "佐賀")));
            mCommandSet.put(Mkz4ApiCaller.Command.Left,
                    new ArrayList<>(Arrays.asList("左", "レフト")));
            mCommandSet.put(Mkz4ApiCaller.Command.Right,
                    new ArrayList<>(Arrays.asList("右", "ライト")));
            mCommandSet.put(Mkz4ApiCaller.Command.Stop,
                    new ArrayList<>(Arrays.asList("止", "ストップ", "とまれ")));
        } else if (locale.equals(Locale.CHINA)) {
            mCommandSet.put(Mkz4ApiCaller.Command.Forward,
                    new ArrayList<>(Arrays.asList("前进")));
            mCommandSet.put(Mkz4ApiCaller.Command.Back,
                    new ArrayList<>(Arrays.asList("倒车")));
            mCommandSet.put(Mkz4ApiCaller.Command.Left,
                    new ArrayList<>(Arrays.asList("左转")));
            mCommandSet.put(Mkz4ApiCaller.Command.Right,
                    new ArrayList<>(Arrays.asList("右转")));
            mCommandSet.put(Mkz4ApiCaller.Command.Stop,
                    new ArrayList<>(Arrays.asList("停")));
        } else {
            mCommandSet.put(Mkz4ApiCaller.Command.Forward,
                    new ArrayList<>(Arrays.asList("Forward", "forward", "Go", "go", "Start", "start")));
            mCommandSet.put(Mkz4ApiCaller.Command.Back,
                    new ArrayList<>(Arrays.asList("Back", "back")));
            mCommandSet.put(Mkz4ApiCaller.Command.Left,
                    new ArrayList<>(Arrays.asList("Left", "left")));
            mCommandSet.put(Mkz4ApiCaller.Command.Right,
                    new ArrayList<>(Arrays.asList("Right", "right")));
            mCommandSet.put(Mkz4ApiCaller.Command.Stop,
                    new ArrayList<>(Arrays.asList("Stop", "stop", "Halt", "halt")));
        }
    }
}

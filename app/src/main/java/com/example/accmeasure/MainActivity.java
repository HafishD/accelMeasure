package com.example.accmeasure;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener, Runnable, View.OnClickListener {

    private SensorManager sensorManager;
    private TextView textView, textInfo;

//    private long startTime;
    private long checkpoint;

    private Button startButton;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean stopRun = false;

    private SoundPool soundPool;
    private int tick;
    private int count = 0;
    private final String txt = "カウント：";
    private String info;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(1).build();

        tick = soundPool.load(this, R.raw.maou_se_system41, 1);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        textInfo = findViewById(R.id.text_info);
        info = txt + count;
        textInfo.setText(info);

        // Get an instance of the TextView
        textView = findViewById(R.id.text_view);

        startButton = findViewById(R.id.start);
        startButton.setOnClickListener(this);

        Button stopButton = findViewById(R.id.finish);
        stopButton.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        Sensor accel = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        /*
        // sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        // sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        // sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
         */
    }

    // 解除するコードも入れる!
    @Override
    protected void onPause() {
        super.onPause();
        // Listenerを解除
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float sensorX, sensorY, sensorZ;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorX = event.values[0];
            sensorY = event.values[1];
            sensorZ = event.values[2];

            String strTmp = "加速度センサー\n"
                    + " X: " + sensorX + "\n"
                    + " Y: " + sensorY + "\n"
                    + " Z: " + sensorZ;
            textView.setText(strTmp);

            // showInfo(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        Thread thread;
        if (v == startButton){
            stopRun = false;
            thread = new Thread(this);
            thread.start();

//            startTime = System.currentTimeMillis();
            checkpoint = System.currentTimeMillis();
        } else {
            stopRun = true;
            count = 0;
            info = txt + count;
            textInfo.setText(info);
        }
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        int period = 10;

        while(!stopRun) {
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopRun = true;
            }

            handler.post(() -> {
                if (System.currentTimeMillis() - checkpoint >= 2000) {
                    count++;
                    soundPool.play(tick, 1.0f, 1.0f, 0, 0, 1);
                    checkpoint = System.currentTimeMillis();
                }
                info = txt + count;
                textInfo.setText(info);
            });
        }
    }
}
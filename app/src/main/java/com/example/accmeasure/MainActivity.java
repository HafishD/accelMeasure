package com.example.accmeasure;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener, Runnable, View.OnClickListener {

    private SensorManager sensorManager;

    private Sensor accel;

    private TextView textView, textInfo;

    private long startTime = 0;
    private long checkpoint;

    private Button startButton;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean stopRun = false;

    private SoundPool soundPool;
    private int tick;
    private int count = 0;
    private final String txt = "カウント：";
    private String info;

    private LineChart mChart;
    private final String[] labels = new String[]{
            "linear_accelerationX",
            "linear_accelerationY",
            "linear_accelerationZ"};
    private final int[] colors = new int[]{
            Color.BLUE,
            Color.GRAY,
            Color.MAGENTA};

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 縦画面
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        startTime = System.currentTimeMillis();

        // 効果音
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(1).build();

        tick = soundPool.load(this, R.raw.maou_se_system41, 1);

        //　加速度センサー
        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        textInfo = findViewById(R.id.text_info);
        info = txt + count;
        textInfo.setText(info);

        // Get an instance of the TextView
        textView = findViewById(R.id.text_view);

        // ボタン
        startButton = findViewById(R.id.start);
        startButton.setOnClickListener(this);

        Button stopButton = findViewById(R.id.finish);
        stopButton.setOnClickListener(this);

        // グラフ
        mChart = findViewById(R.id.line_chart);

        mChart.setData(new LineData()); //インスタンス生成

        mChart.getDescription().setEnabled(false);

        mChart.setDrawGridBackground(true);

        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        mChart.getAxisRight().setEnabled(false);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        accel = sensorManager.getDefaultSensor(
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
         long updTime = 0;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            String strTmp = "加速度センサー\n"
                    + " X: " + event.values[0] + "\n"
                    + " Y: " + event.values[1] + "\n"
                    + " Z: " + event.values[2];
            textView.setText(strTmp);

            LineData data = mChart.getLineData();


            if (startTime > 0){
                updTime = System.currentTimeMillis() - startTime;
            }

            if (data != null){
                for(int i = 0; i < 3; i++){
                    ILineDataSet set3 = data.getDataSetByIndex(i);
                    if (set3 == null) {
                        LineDataSet set = new LineDataSet(null, labels[i]);
                        set.setLineWidth(2.0f);
                        set.setColor(colors[i]);
                        set.setDrawCircles(false);
                        set.setDrawValues(false);
                        set3 = set;
                        data.addDataSet(set3);
                    }

                    data.addEntry(new Entry((float) updTime/1000, event.values[i]), i);

                    data.notifyDataChanged();
                }
                mChart.notifyDataSetChanged();
                mChart.setVisibleXRangeMaximum(50);
                mChart.moveViewToX(data.getEntryCount());
            }
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

            checkpoint = System.currentTimeMillis();

            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            stopRun = true;
            sensorManager.unregisterListener(this);
            count = 0;
            info = txt + count;
            textInfo.setText(info);
        }
    }

    @SuppressWarnings("BusyWait") // happen when there are no process other than Thread.sleep on a loop
    @Override
    public void run() {
        int period = 10;

        while(!stopRun) {
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopRun = true;
                count = 0;
                textInfo.setText(info);
            }

            // 誘導用
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
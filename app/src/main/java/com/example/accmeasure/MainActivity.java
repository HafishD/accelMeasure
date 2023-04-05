package com.example.accmeasure;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.DashPathEffect;
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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener, Runnable, View.OnClickListener {

    private SensorManager sensorManager;

    private final ArrayList<Entry> values1 = new ArrayList<>();
    private final ArrayList<Entry> values2 = new ArrayList<>();
    private final ArrayList<Entry> values3 = new ArrayList<>();
    private TextView textView, textInfo;

    private long startTime;
    private long checkpoint;

    private Button startButton;
    private boolean startDraw = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean stopRun = false;

    private SoundPool soundPool;
    private int tick;
    private int count = 0;
    private final String txt = "カウント：";
    private String info;

    private LineChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        mChart.setDrawGridBackground(true);

        mChart.getDescription().setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setAxisMaximum(15f);
        leftAxis.setAxisMinimum(-15f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);

        mChart.getAxisRight().setEnabled(false);

         setData();

        mChart.animateX(1000);
        mChart.invalidate();

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

    @SuppressWarnings("SuspiciousNameCombination") // named sensorX but got set as 'y' parameter
    @Override
    public void onSensorChanged(SensorEvent event) {
         float sensorX, sensorY, sensorZ;
         float updTime;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorX = event.values[0];
            sensorY = event.values[1];
            sensorZ = event.values[2];

            String strTmp = "加速度センサー\n"
                    + " X: " + sensorX + "\n"
                    + " Y: " + sensorY + "\n"
                    + " Z: " + sensorZ;
            textView.setText(strTmp);

            if (startDraw) {
                updTime = (float) System.currentTimeMillis() - startTime;
                values1.add(new Entry(updTime, sensorX));
                values2.add(new Entry(updTime, sensorY));
                values3.add(new Entry(updTime, sensorZ));
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
            startDraw = true;
            thread = new Thread(this);
            thread.start();

            startTime = System.currentTimeMillis();
            checkpoint = System.currentTimeMillis();
        } else {
            stopRun = true;
            startDraw = false;
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
                startDraw = false;
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

    private void setData() {
        LineDataSet set1, set2, set3;

        if (mChart.getData() != null && mChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
            set2 = (LineDataSet) mChart.getData().getDataSetByIndex(1);
            set3 = (LineDataSet) mChart.getData().getDataSetByIndex(2);

            set1.setValues(values1);
            set2.setValues(values2);
            set3.setValues(values3);

            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values1, "DataSet 1");
            set1.setDrawIcons(false);
            set1.setColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setValueTextSize(0f);
            set1.setDrawFilled(false);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            // create a dataset and give it a type
            set2 = new LineDataSet(values2, "DataSet 2");
            set2.setDrawIcons(false);
            set2.setColor(Color.RED);
            set2.setLineWidth(1f);
            set2.setValueTextSize(0f);
            set2.setDrawFilled(false);
            set2.setFormLineWidth(1f);
            set2.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set2.setFormSize(15.f);

            set3 = new LineDataSet(values3, "DataSet 3");
            set3.setDrawIcons(false);
            set3.setColor(Color.GREEN);
            set3.setLineWidth(1f);
            set3.setValueTextSize(0f);
            set3.setDrawFilled(false);
            set3.setFormLineWidth(1f);
            set3.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set3.setFormSize(15.f);

            // create a data object with the data sets
            LineData data = new LineData(set1, set2, set3);

            // set data
            mChart.setData(data);
        }
    }
}
package com.gertec.heatmapexample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import ca.hss.heatmaplib.HeatMap;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        HeatMap heatMap = (HeatMap) findViewById(R.id.heatmap);

        heatMap.setMinimum(0.0);
        heatMap.setMaximum(100.0);
        heatMap.setRadius(1000);

        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            HeatMap.DataPoint point = new HeatMap.DataPoint(rand.nextFloat(), rand.nextFloat(), rand.nextDouble() * 100.0);
            heatMap.addData(point);
        }
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        HeatMap.DataPoint point = new HeatMap.DataPoint(x,y,100.0);
        mPreviousX = x;
        mPreviousY = y;
        return true;
    }
}

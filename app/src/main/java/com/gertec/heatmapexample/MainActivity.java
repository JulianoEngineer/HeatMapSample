package com.gertec.heatmapexample;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import ca.hss.heatmaplib.HeatMap;

public class MainActivity extends AppCompatActivity {

    public HeatMap heatMap;
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;
    private float arrayDados[][] = new float[100][100];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        heatMap = (HeatMap) findViewById(R.id.heatmap);
        final Handler handler = new Handler();

        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<100;i++) {
                    for(int j=0;j<100;j++) {
                        if(arrayDados[i][j] >= 10.0f )
                            arrayDados[i][j] -= 10.0f;
                    }
                }
                RefreshScreen();
                handler.postDelayed(this, 500);
            }
        };

        handler.post(runnableCode);

        heatMap.setMinimum(0.0);
        heatMap.setMaximum(100.0);
        heatMap.setRadius(250);

        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            HeatMap.DataPoint point = new HeatMap.DataPoint(i/100.0f, i/100.0f,  100.0);
            heatMap.addData(point);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        float x = e.getX()/width;
        float y = e.getY()/height;
        AddPoint(x,y);
        mPreviousX = x;
        mPreviousY = y;
        return true;
    }


    private void AddPoint(float x, float y) {
        int actualx = Math.round(x*100);
        int actualy = Math.round(y*100);

        arrayDados[actualx][actualy] = 100.0f;
        RefreshScreen();
    }

    private void RefreshScreen()
    {
        for(int i=0;i<100;i++) {
            for(int j=0;j<100;j++) {
                HeatMap.DataPoint point = new HeatMap.DataPoint(i/100.0f, j / 100.0f, arrayDados[i][j]);
                heatMap.addData(point);
            }
        }

        heatMap.forceRefresh();
    }
}

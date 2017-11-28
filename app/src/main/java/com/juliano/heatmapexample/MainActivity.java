package com.juliano.heatmapexample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import ca.hss.heatmaplib.HeatMap;

public class MainActivity extends AppCompatActivity {

    public HeatMap heatMap;
    private int sizex = 10;
    private int sizey = 10;
    private float arrayDados[][] = new float[sizex][sizey];
    private int timelapse = 10;
    private TextView myLabel;
    private EditText blueToothName;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        heatMap = (HeatMap) findViewById(R.id.heatmap);
        heatMap.setMinimum(0.0);
        heatMap.setMaximum(100.0);
        heatMap.setRadius(250);



        final Button openButton = (Button)findViewById(R.id.open);
        final Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        blueToothName = (EditText) findViewById(R.id.BluetoothName);

        // Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    if(blueToothName.getText().length()<=0)
                    {
                        Toast.makeText(v.getContext(),"O campo Nome BlueTooth deve ser preenchido",Toast.LENGTH_LONG).show();
                    }else {
                        if(findBT()==0) {
                            openBT();
                            closeButton.setEnabled(true);
                            openButton.setEnabled(false);
                        }else {
                            Toast.makeText(v.getContext(), "Não foi possível se conectar ao dispositivo bluetooth.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                catch (IOException ex) { }
            }
        });


        // Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                    closeButton.setEnabled(false);
                    openButton.setEnabled(true);
                }
                catch (IOException ex) { }
            }
        });

        final Handler handler = new Handler();

        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<sizex;i++) {
                    for(int j=0;j<sizey;j++) {
                        if(arrayDados[i][j] >= 5.0f )
                            arrayDados[i][j] -= 5.0f;
                    }
                }
                RefreshScreen();
                handler.postDelayed(this, timelapse);
            }
        };

        handler.post(runnableCode);

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

        return true;
    }


    private void AddPoint(float x, float y) {
        int actualx = Math.round(x*sizex);
        int actualy = Math.round(y*sizey);

        arrayDados[actualx][actualy] = 100.0f;

        RefreshScreen();
    }
    private void parse_data(byte[] encodedBytes) {


        for(int i = 0; i< 10;i++)
        {
            for(int j = 0; j< 10;j++)
                arrayDados[i][j] = encodedBytes[10*i+j];
        }

        RefreshScreen();
    }

    private void RefreshScreen()
    {
        heatMap.clearData();
        for(int i=0;i<sizex;i++) {
            for(int j=0;j<sizey;j++) {
                HeatMap.DataPoint point = new HeatMap.DataPoint(i/(sizex*1.0f), j/(sizey*1.0f), arrayDados[i][j]);
                heatMap.addData(point);
            }
        }

        heatMap.forceRefresh();
    }

    int findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals(blueToothName.getText().toString()))
                {
                    mmDevice = device;
                    return 0;
                }
            }
        }
        myLabel.setText("Bluetooth Device Found");
        return 1;
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 3; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    final byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    //final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            parse_data(encodedBytes);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }


    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
}

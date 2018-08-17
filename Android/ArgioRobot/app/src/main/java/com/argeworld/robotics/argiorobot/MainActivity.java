package com.argeworld.robotics.argiorobot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import info.hoang8f.widget.FButton;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private FButton btnWalk;
    private FButton btnBack;
    private FButton btnLeft;
    private FButton btnRight;
    private FButton btnRandom;
    private FButton btnUpDown;
    private FButton btnDance;
    private FButton btnTalk;
    private FButton btnPatrol;
    private FButton btnMoonWalk;

    private MaterialDialog dialog;

    private boolean bInited = false;

    private BluetoothSPP bt;

    private int iBTSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt = new BluetoothSPP(this);

        if(!bt.isBluetoothAvailable())
        {
            new MaterialDialog.Builder(MainActivity.this)
                    .title("Error")
                    .content("Bluetooth is not available !")
                    .negativeText("EXIT")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                            System.exit(0);
                        }
                    })
                    .canceledOnTouchOutside(false)
                    .cancelable(false)
                    .show();

            return;
        }

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener()
        {
            public void onDataReceived(byte[] data, String message)
            {
                Log.i(TAG, "BT onDataReceived: " + message);
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener()
        {
            public void onDeviceConnected(String name, String address)
            {
                Log.i(TAG, "BT onDeviceConnected: " + name + " " + address);

                Toast.makeText(getApplicationContext()
                        , "Connected to " + name
                        , Toast.LENGTH_SHORT).show();

                SendCommand("I");

                iBTSetup = 1;

                SharedPreferences settings = getApplicationContext().getSharedPreferences("ARGIO_APP", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("BT_SETUP", iBTSetup);
                editor.apply();
            }

            public void onDeviceDisconnected()
            {
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed()
            {
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener()
        {
            public void onServiceStateChanged(int state)
            {
                if(state == BluetoothState.STATE_CONNECTED)
                {
                    Log.i(TAG, "BT onServiceStateChanged to CONNECTED");

                    if(dialog != null)
                        dialog.dismiss();

                    Toast.makeText(getApplicationContext()
                            , "Connected to ARGIO"
                            , Toast.LENGTH_SHORT).show();

                    SendCommand("I");
                }
                else if(state == BluetoothState.STATE_CONNECTING)
                {
                    Log.i(TAG, "BT onServiceStateChanged to CONNECTING");
                }
                else if(state == BluetoothState.STATE_LISTEN)
                {
                    Log.i(TAG, "BT onServiceStateChanged to LISTEN");
                }
                else if(state == BluetoothState.STATE_NONE)
                {
                    Log.i(TAG, "BT onServiceStateChanged to STATE_NONE");
                }
            }
        });

        bt.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener()
        {
            public void onNewConnection(String name, String address)
            {
                Log.i(TAG, "New Connection - " + name + " - " + address);

                if(!name.equals("ARGIO"))
                {
                    if(dialog != null)
                        dialog.dismiss();

                    iBTSetup = 0;

                    SharedPreferences settings = getApplicationContext().getSharedPreferences("ARGIO_APP", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("BT_SETUP", iBTSetup);
                    editor.apply();

                    InitBT();
                }
            }

            public void onAutoConnectionStarted()
            {
                Log.i(TAG, "Auto menu_connection started");
            }
        });

        InitBT();

        SetupButtons();
    }

    public void onStart()
    {
        Log.i(TAG, "onStart");

        super.onStart();

        if (!bt.isBluetoothEnabled())
        {
            Log.i(TAG, "BT disabled");

            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        }
        else
        {
            Log.i(TAG, "BT enabled");

            if(!bt.isServiceAvailable())
            {
                StartBTService();
            }
            else
            {
                SendCommand("I");
            }
        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        bt.stopService();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE)
        {
            if(resultCode == Activity.RESULT_OK)
                bt.connect(data);
        }
        else if(requestCode == BluetoothState.REQUEST_ENABLE_BT)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                StartBTService();
            }
            else
            {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void StartBTService()
    {
        Log.i(TAG,"StartBTService");

        dialog = new MaterialDialog.Builder(this)
                .title("Please Wait")
                .content("Connecting to ARGIO...")
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .canceledOnTouchOutside(false)
                .cancelable(false)
                .show();

        bt.setupService();
        bt.startService(BluetoothState.DEVICE_OTHER);

        if(iBTSetup == 1)
        {
            Log.i(TAG,"AutoConnect");

            bt.autoConnect("ARGIO");
        }
    }

    public void InitBT()
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("ARGIO_APP", 0);
        iBTSetup = settings.getInt("BT_SETUP", 0);

        if(iBTSetup == 0)
        {
            if (bt.getServiceState() == BluetoothState.STATE_CONNECTED)
            {
                bt.disconnect();
            }
            else
            {
                Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
            }
        }
    }

    public void SetupButtons()
    {
        btnWalk = (FButton) findViewById(R.id.walk_button);
        btnWalk.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnWalk.setOnTouchListener(this);

        btnLeft = (FButton) findViewById(R.id.left_button);
        btnLeft.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnLeft.setOnTouchListener(this);

        btnRight = (FButton) findViewById(R.id.right_button);
        btnRight.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnRight.setOnTouchListener(this);

        btnBack = (FButton) findViewById(R.id.back_button);
        btnBack.setButtonColor(getResources().getColor(R.color.fbutton_color_orange));
        btnBack.setOnTouchListener(this);

        btnRandom = (FButton) findViewById(R.id.random_button);
        btnRandom.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnRandom.setOnTouchListener(this);

        btnUpDown = (FButton) findViewById(R.id.updown_button);
        btnUpDown.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnUpDown.setOnTouchListener(this);

        btnDance = (FButton) findViewById(R.id.dance_button);
        btnDance.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnDance.setOnTouchListener(this);

        btnTalk = (FButton) findViewById(R.id.talk_button);
        btnTalk.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnTalk.setOnTouchListener(this);

        btnPatrol = (FButton) findViewById(R.id.patrol_button);
        btnPatrol.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnPatrol.setOnTouchListener(this);

        btnMoonWalk = (FButton) findViewById(R.id.moonwalk_button);
        btnMoonWalk.setButtonColor(getResources().getColor(R.color.fbutton_color_nephritis));
        btnMoonWalk.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if(v.getId() == R.id.walk_button)
            {
                Log.i(TAG,"Walk Button Down");

                btnWalk.onTouch(v, event);

                SendCommand("W");
            }
            else if(v.getId() == R.id.back_button)
            {
                Log.i(TAG,"Back Button Down");

                btnBack.onTouch(v, event);

                SendCommand("B");
            }
            else if(v.getId() == R.id.left_button)
            {
                Log.i(TAG,"Left Button Down");

                btnLeft.onTouch(v, event);

                SendCommand("L");
            }
            else if(v.getId() == R.id.right_button)
            {
                Log.i(TAG,"Right Button Down");

                btnRight.onTouch(v, event);

                SendCommand("R");
            }
            else if(v.getId() == R.id.random_button)
            {
                Log.i(TAG,"Random Button Down");

                btnRandom.onTouch(v, event);

                SendCommand("X");
            }
            else if(v.getId() == R.id.updown_button)
            {
                Log.i(TAG,"UpDown Button Down");

                btnUpDown.onTouch(v, event);

                SendCommand("U");
            }
            else if(v.getId() == R.id.dance_button)
            {
                Log.i(TAG,"Dance Button Down");

                btnDance.onTouch(v, event);

                SendCommand("D");
            }
            else if(v.getId() == R.id.talk_button)
            {
                Log.i(TAG,"Talk Button Down");

                btnTalk.onTouch(v, event);

                SendCommand("T");
            }
            else if(v.getId() == R.id.patrol_button)
            {
                Log.i(TAG,"Patrol Button Down");

                btnPatrol.onTouch(v, event);

                SendCommand("P");
            }
            else if(v.getId() == R.id.moonwalk_button)
            {
                Log.i(TAG,"MoonWalk Button Down");

                btnMoonWalk.onTouch(v, event);

                SendCommand("M");
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if(v.getId() == R.id.walk_button)
            {
                Log.i(TAG,"Walk Button Up");

                btnWalk.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.back_button)
            {
                Log.i(TAG,"Back Button Up");

                btnBack.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.left_button)
            {
                Log.i(TAG,"Left Button Up");

                btnLeft.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.right_button)
            {
                Log.i(TAG,"Right Button Up");

                btnRight.onTouch(v, event);

                SendCommand("S");
            }
            else if(v.getId() == R.id.random_button)
            {
                Log.i(TAG,"Random Button Up");

                btnRandom.onTouch(v, event);
            }
            else if(v.getId() == R.id.updown_button)
            {
                Log.i(TAG,"UpDown Button Up");

                btnUpDown.onTouch(v, event);
            }
            else if(v.getId() == R.id.dance_button)
            {
                Log.i(TAG,"Dance Button Up");

                btnDance.onTouch(v, event);
            }
            else if(v.getId() == R.id.talk_button)
            {
                Log.i(TAG,"Talk Button Up");

                btnTalk.onTouch(v, event);
            }
            else if(v.getId() == R.id.patrol_button)
            {
                Log.i(TAG,"Patrol Button Up");

                btnPatrol.onTouch(v, event);
            }
            else if(v.getId() == R.id.moonwalk_button)
            {
                Log.i(TAG,"MoonWalk Button Up");

                btnMoonWalk.onTouch(v, event);
            }
        }

        return true;
    }

    public void SendCommand(String cmd)
    {
        Log.i(TAG, "SendCommand: " + cmd);

        bt.send(cmd, true);
    }
}

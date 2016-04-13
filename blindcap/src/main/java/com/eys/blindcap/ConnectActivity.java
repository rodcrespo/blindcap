package com.eys.blindcap;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.eys.ble.BLEDeviceListAdapter;
import com.eys.ble.BluetoothHandler;


public class ConnectActivity extends Activity {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private ImageButton scanButton;

    private View bleDeviceListHolder;
    private ListView bleDeviceListView;
    private BLEDeviceListAdapter listViewAdapter;

    private BluetoothHandler bluetoothHandler;
    private boolean isConnected;

    private ViewGroup contentView;
    private ViewGroup connectView;

    private Animation animScale;
    private AnimatorSet animOpacity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }

        animScale = AnimationUtils.loadAnimation(this, R.anim.scale_anim);

        contentView = (ViewGroup) findViewById(R.id.contentView);
        connectView = (ViewGroup) findViewById(R.id.connectingView);

        scanButton = (ImageButton) findViewById(R.id.scanButton);
        scanButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.startAnimation(animScale);
                    scanOnClick();
                    return true;
                }
                return false;
            }
        });

        bleDeviceListHolder = findViewById(R.id.bleDeviceListHolder);
        bleDeviceListHolder.setVisibility(View.GONE);

        bleDeviceListView = (ListView) bleDeviceListHolder.findViewById(R.id.bleDeviceListView);
        listViewAdapter = new BLEDeviceListAdapter(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothHandler = new BluetoothHandler(this);
        bluetoothHandler.setOnConnectedListener(new BluetoothHandler.OnConnectedListener() {
            @Override
            public void onConnected(boolean isConnected) {
                setConnectStatus(isConnected);
            }
        });

        bleDeviceListView.setAdapter(bluetoothHandler.getDeviceListAdapter());
        bleDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                BluetoothDevice device = bluetoothHandler.getDeviceListAdapter().getItem(position).device;

                // connect
                bluetoothHandler.connect(device.getAddress());

                hideDeviceList();
            }
        });

        initConnectView();
    }


    private BluetoothHandler.OnRecievedDataListener recListener = new BluetoothHandler.OnRecievedDataListener() {
        @Override
        public void onRecievedData(byte[] bytes) {
            showMessage(bytes.toString());
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("BLINDCAP", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    public void scanOnClick(){
        //showMessage("Start scan");

        showConnectView();
    }


    public void ledOnClick(final View v){
        if(isConnected){
            bluetoothHandler.sendData(new byte[]{1});
            showMessage("Led ON");
        }else{
            showMessage("Need to connect first");
        }
    }


    public void ledOffClick(final View v){

        if(isConnected){
            bluetoothHandler.sendData(new byte[]{0});
            showMessage("Led OFF");
        }else{
            showMessage("Need to connect first");
        }
    }


    public void setConnectStatus(boolean isConnected){
        this.isConnected = isConnected;
        if (isConnected) {
            //showMessage("Connection successful");

            goToNextActivity();
        } else {
            bluetoothHandler.onPause();
            bluetoothHandler.onDestroy();

            showMessage("Connection failed");

            exitConnectView();
        }
    }


    private void showMessage(String str){
        Toast.makeText(ConnectActivity.this, str, Toast.LENGTH_SHORT).show();
    }


    public void goToNextActivity() {
        exitConnectView();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        bluetoothHandler.onDestroy();
    }


    @Override
    public void onResume(){
        super.onResume();
        bluetoothHandler.onResume();
    }


    @Override
    public void onPause(){
        super.onPause();
        bluetoothHandler.onPause();
    }


    private void initConnectView() {
        Typeface fontMedium = Typeface.createFromAsset(getAssets(), "font/SamsungSharpSans-Medium.ttf");
        setFonts(connectView, fontMedium);

        // text animation
        animOpacity = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.connecting_animation);
        animOpacity.setTarget(findViewById(R.id.connectingText));
        animOpacity.start();

        // hide
        connectView.setVisibility(View.GONE);
    }


    private void showConnectView() {
        Command startScanning = new Command() {
            @Override
            public void execute() {
                if (!isConnected) {
                    bluetoothHandler.setOnScanListener(new BluetoothHandler.OnScanListener() {
                        @Override
                        public void onScanFinished() {
                            //showMessage("Scan Finished");

                            showDeviceList();

                            // exit connecting view if no devices were found
                            if (bluetoothHandler.getDeviceListAdapter().getCount() == 0) {
                                exitConnectView();

                                showMessage("No devices found");
                            }
                        }
                        @Override
                        public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        }
                    });

                    // start scanning
                    bluetoothHandler.scanLeDevice(true);

                } else {

                    showMessage("Already connected");
                    //setConnectStatus(false);
                }
            }
        };

        crossfade(contentView, connectView, startScanning);
    }


    private void exitConnectView(){
        crossfade(connectView, contentView, null);

        hideDeviceList();
    }


    private void showDeviceList() {
        bleDeviceListHolder.setVisibility(View.VISIBLE);
    }

    private void hideDeviceList() {
        bleDeviceListHolder.setVisibility(View.GONE);
    }


    // aux methods:


    private void crossfade(final View viewOut, View viewIn, final Command callback) {
        viewIn.setAlpha(0f);
        viewIn.setVisibility(View.VISIBLE);
        viewIn.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);

        viewOut.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewOut.setVisibility(View.GONE);

                        if (callback != null) {
                            callback.execute();
                        }
                    }
                });
    }


    private void setFonts(ViewGroup group, Typeface font) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup) {
                setFonts((ViewGroup) child, font);
            }
            else if (child instanceof TextView) {
                ((TextView) child).setTypeface(font);
            }
        }
    }



    // a Command object is passed to crossfade() as a parameter
    public interface Command {
        void execute();
    }

}


package com.eys.blindcap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.eys.ble.BLEDeviceListAdapter;
import com.eys.ble.BluetoothHandler;

public class ConnectActivity extends Activity {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private Button scanButton;
    private ListView bleDeviceListView;
    private BLEDeviceListAdapter listViewAdapter;

    private BluetoothHandler bluetoothHandler;
    private boolean isConnected;

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

        scanButton = (Button) findViewById(R.id.scanButton);
        bleDeviceListView = (ListView) findViewById(R.id.bleDeviceListView);
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
    }

    private BluetoothHandler.OnRecievedDataListener recListener = new BluetoothHandler.OnRecievedDataListener() {

        @Override
        public void onRecievedData(byte[] bytes) {
            showMessage(bytes.toString());
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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
    public void scanOnClick(final View v){
        showMessage("Start scan");

        if(!isConnected){
            bleDeviceListView.setAdapter(bluetoothHandler.getDeviceListAdapter());
            bleDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String buttonText = (String) ((Button)v).getText();
                    if(buttonText.equals("scanning")){
                        showMessage("scanning...");
                        return ;
                    }
                    BluetoothDevice device = bluetoothHandler.getDeviceListAdapter().getItem(position).device;
                    // connect
                    bluetoothHandler.connect(device.getAddress());
                }
            });
            bluetoothHandler.setOnScanListener(new BluetoothHandler.OnScanListener() {
                @Override
                public void onScanFinished() {
                    // TODO Auto-generated method stub
                    ((Button)v).setText("scan");
                    ((Button)v).setEnabled(true);
                    showMessage("Scan Finished");
                }
                @Override
                public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord) {}
            });
            ((Button)v).setText("scanning");
            ((Button)v).setEnabled(false);
            bluetoothHandler.scanLeDevice(true);
        }else{
            showMessage("Already connected");
            setConnectStatus(false);
        }
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
        if(isConnected){
            showMessage("Connection successful");
            scanButton.setText("break");
        }else{
            bluetoothHandler.onPause();
            bluetoothHandler.onDestroy();
            scanButton.setText("scan");
        }
    }

    private void showMessage(String str){
        Toast.makeText(ConnectActivity.this, str, Toast.LENGTH_SHORT).show();
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
}


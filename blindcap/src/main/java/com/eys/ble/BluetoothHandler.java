package com.eys.ble;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import android.app.Activity;
import com.eys.blindcap.MainActivity;

public class BluetoothHandler {
	// scan bluetooth device
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mEnabled = false;
	private boolean mScanning = false;
	private static final long SCAN_PERIOD = 2000;
	private BLEDeviceListAdapter mDevListAdapter;
	private String mCurrentConnectedBLEAddr;
	
	// connect bluetooth device
	private BluetoothLeService mBluetoothLeService;
	private String mDeviceAddress = null;
	private boolean mConnected = false;
	private OnRecievedDataListener onRecListener;
	private OnConnectedListener onConnectedListener;
	private OnScanListener onScanListener;
	
	private List<BluetoothGattService> gattServices = null;
	private UUID targetServiceUuid =
			UUID.fromString("969c692e-9cc2-4f99-8e70-c5a844400451");
//            UUID.fromString("629a0c20-0418-d8bc-e411-22a2d08a13fa");
    private UUID targetCharacterUuid =
		UUID.fromString("969c692e-9cc2-4f99-8e70-c5a844400451");
//            UUID.fromString("fa138a01-a222-11e4-bcd8-1804200c9a62");
    private UUID readUUID =
		UUID.fromString("969c692e-9cc2-4f99-8e70-c5a844400451");
//            UUID.fromString("fa138a01-a222-11e4-bcd8-1804200c9a62");
    private BluetoothGattCharacteristic targetGattCharacteristic = null;
    
    private Context context;

    private boolean isConnected = false;
    public boolean reconnecting = false;

	private static BluetoothHandler INSTANCE = new BluetoothHandler();

	// El constructor privado no permite que se genere un constructor por defecto.
	// (con mismo modificador de acceso que la definición de la clase)
	private BluetoothHandler() {}

	public static BluetoothHandler getInstance() {
		return INSTANCE;
	}
	
    public interface OnRecievedDataListener{
    	public void onRecievedData(byte[] bytes);
    };
    
    public interface OnConnectedListener{
    	public void onConnected(boolean isConnected);
    };
    
    public interface OnScanListener{
    	public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    	public void onScanFinished();
    };
    
    public void setOnScanListener(OnScanListener l){
    	onScanListener = l;
    }

    public void setContext(Activity activity){
        this.context = activity;
    }

	public void init(Context context) {
		// TODO Auto-generated constructor stub
		this.context = context;

        if (mConnected){
            ((Activity) context).unbindService(mServiceConnection);
            mBluetoothLeService.disconnect();
        }
        mBluetoothLeService = null;
        mConnected = false;

        this.isConnected = false;
		mDevListAdapter = new BLEDeviceListAdapter(context);
		mBluetoothAdapter = null;
		mCurrentConnectedBLEAddr = null;
		
		if(!isSupportBle()){
			Toast.makeText(context, "your device not support BLE!", Toast.LENGTH_SHORT).show();
			((Activity)context).finish();
			return ;
		}
		// open bluetooth
        if (!getBluetoothAdapter().isEnabled()) { 
            Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); 
            ((Activity)context).startActivityForResult(mIntent, 1);   
        }else{
        	setEnabled(true);
        }
	}
	
	public BLEDeviceListAdapter getDeviceListAdapter(){
		return mDevListAdapter;
	} 
	
	public void connect(String deviceAddress){
		mDeviceAddress = deviceAddress;	
		Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
		
		if(!((Activity)context).bindService(gattServiceIntent, mServiceConnection, ((Activity)context).BIND_AUTO_CREATE)){
			System.out.println("bindService failed!");
		} 
		
		((Activity)context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }else{
        	System.out.println("mBluetoothLeService = null");
        }
	}
	
	private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
	
	// Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

        	mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("onServiceConnected", "Unable to initialize Bluetooth");
                ((Activity) context).finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        	mBluetoothLeService = null;

            Log.e("onServiceDisconnected", "Service disconnected");
            setIsConnected(false);
        }
    };

    public void reconnect(){
        reconnecting = true;
        mBluetoothLeService.disconnect();
        setOnScanListener(new BluetoothHandler.OnScanListener() {
            @Override
            public void onScanFinished() {

                mBluetoothLeService.connect(mDeviceAddress);
            }
            @Override
            public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord) {}
        });
        scanLeDevice(true);
    }
    
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mCurrentConnectedBLEAddr = mDeviceAddress;
                if(onConnectedListener != null){
                	onConnectedListener.onConnected(true);
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.e("onServiceDisconnected", "Other Service disconnected");
                mConnected = false;
                setIsConnected(false);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	if(mBluetoothLeService != null)
            		getCharacteristic(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	byte[] bytes = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
            	if(onRecListener != null)
            		onRecListener.onRecievedData(bytes);
            }
        }
    };
    
    public void setOnRecievedDataListener(OnRecievedDataListener l){
    	onRecListener = l;
    }
    
    public void setOnConnectedListener(OnConnectedListener l){
    	onConnectedListener = l;
    }
    
	public void getCharacteristic(List<BluetoothGattService> gattServices){
    	this.gattServices = gattServices;
        String uuid = null;
        BluetoothGattCharacteristic characteristic = null;
        BluetoothGattService targetGattService = null;
        // get target gattservice
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.i("Le service", "The service is "  + uuid);
            if(uuid.equals(targetServiceUuid.toString())){
                targetGattService = gattService;
                break;
            }
        }
        if(targetGattService != null){
            //Toast.makeText(DeviceControlActivity.this, "get service ok", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(context, "not support this BLE module", Toast.LENGTH_SHORT).show();
            return ;
        }
        List<BluetoothGattCharacteristic> gattCharacteristics =
            targetGattService.getCharacteristics();
        targetGattCharacteristic = targetGattService.getCharacteristic(targetCharacterUuid);
		BluetoothGattCharacteristic readGattCharacteristic = targetGattService.getCharacteristic(readUUID);
		if(readGattCharacteristic != null)
			mBluetoothLeService.setCharacteristicNotification(readGattCharacteristic, true);
        
        if(targetGattCharacteristic != null){
            //Toast.makeText(context, "get character ok", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(context, "not support this BLE module", Toast.LENGTH_SHORT).show();
            return ;
        }
    }  
    
	public void onPause() {
		// TODO Auto-generated method stub
		if(mConnected){
			((Activity) context).unregisterReceiver(mGattUpdateReceiver);
		}
	}
	
	public void onDestroy(){
		if(mConnected){
			mDevListAdapter.clearDevice();
			mDevListAdapter.notifyDataSetChanged();
			((Activity) context).unbindService(mServiceConnection);
			mBluetoothLeService = null;
			mConnected = false;
        }
	}
	
	public void onResume(){
		if(!mConnected || mBluetoothLeService == null)
			return ;
		((Activity)context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d("registerReceiver", "Connect request result=" + result);
        }
	}
	
	public synchronized void sendData(byte[] value){
        Log.i("gatt", Boolean.toString(targetGattCharacteristic != null));
        Log.i("leservice", Boolean.toString(mBluetoothLeService != null));
        Log.i("connected", Boolean.toString(mConnected == true));
    	if(targetGattCharacteristic != null && mBluetoothLeService != null && mConnected == true){
    		int targetLen = 0;
    		int offset=0;
    		for(int len = (int)value.length; len > 0; len -= 20){
    		  	if(len < 20)
    		  		targetLen = len;
    			else
    				targetLen = 20;
    		  	byte[] targetByte = new byte[targetLen];
    		  	System.arraycopy(value, offset, targetByte, 0, targetLen);
    			offset += 20;
    			targetGattCharacteristic.setValue(targetByte);
    			mBluetoothLeService.writeCharacteristic(targetGattCharacteristic);
    			try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    }
	
	public synchronized void sendData(String value){
    	if(targetGattCharacteristic != null && mBluetoothLeService != null && mConnected == true){
    		targetGattCharacteristic.setValue(value);
    		mBluetoothLeService.writeCharacteristic(targetGattCharacteristic);  
    	}
    }
	
	public boolean isSupportBle(){
		// is support 4.0 ?
		final BluetoothManager bluetoothManager = (BluetoothManager)
				context.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();	
		if (mBluetoothAdapter == null) 
			return false;
		else
			return true;			
	}
	
	public BluetoothAdapter getBluetoothAdapter(){
		return mBluetoothAdapter;
	}
	
	public void setEnabled(boolean enabled){
		mEnabled = enabled;
	}
	
	public boolean isEnabled(){
		return mEnabled;
	}
	
	Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if(msg.obj != null){
				mDevListAdapter.addDevice((BluetoothScanInfo) msg.obj);
				mDevListAdapter.notifyDataSetChanged();	
			}
		}
    };
    
    // scan device
 	public void scanLeDevice(boolean enable) {
 		if (enable) {
 			mDevListAdapter.clearDevice();
 			mDevListAdapter.notifyDataSetChanged();
 			mHandler.postDelayed(new Runnable() {
 			@Override
 				public void run() {
 					mScanning = false;
 					mBluetoothAdapter.stopLeScan(mLeScanCallback);
 					if(onScanListener != null){
 		        		onScanListener.onScanFinished();
 		        	}
 				}
 			}, SCAN_PERIOD);

 			mScanning = true;
 			mBluetoothAdapter.startLeScan(mLeScanCallback);
 		} else {
 			mScanning = false;
 			mBluetoothAdapter.stopLeScan(mLeScanCallback);
 		}
 	}
 	
 	public boolean isScanning(){
 		return mScanning;
 	}
 	
 	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                final byte[] scanRecord) {
        	
        	if(onScanListener != null){
        		onScanListener.onScan(device, rssi, scanRecord);
        	}
        	
        	System.out.println("scan info:");
        	System.out.println("rssi="+rssi);
        	System.out.println("ScanRecord:");
        	for(byte b:scanRecord)
        		System.out.printf("%02X ", b);
        	System.out.println("");
        	
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	Message msg = new Message();
                	BluetoothScanInfo info = new BluetoothScanInfo();
                	info.device = device;
                	info.rssi = rssi;
                	info.scanRecord = scanRecord;
                	msg.obj = info;
                	mHandler.sendMessage(msg);
                }
            });      
        }
    };
    
    public class BluetoothScanInfo{
    	public BluetoothDevice device;
    	public int rssi;
    	public byte[] scanRecord;
    };
    
    public static double calculateAccuracy(int txPower, double rssi) {
    	if (rssi == 0) {
    		return -1.0; // if we cannot determine accuracy, return -1.
    	}

    	double ratio = rssi*1.0/txPower;
    	if (ratio < 1.0) {
    		return Math.pow(ratio,10);
    	}
    	else {
    		double accuracy =  (0.89976)*Math.pow(ratio, 7.7095) + 0.111;
    		return accuracy;
    	}
    }

    public boolean isConnected(){
        return isConnected;
    }

    public void setIsConnected(boolean newConnected){
        this.isConnected = newConnected;
    }

    private void showMessage(String str){
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }
}

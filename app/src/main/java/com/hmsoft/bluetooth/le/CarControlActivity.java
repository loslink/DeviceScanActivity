/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmsoft.bluetooth.le;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetooth.le.R;

public class CarControlActivity extends Activity {

    private final static String TAG = CarControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName="BT05";
    private String mDeviceAddress="00:15:87:00:B0:70";
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    private String GO="1";
    private String BACK="2";
    private String LEFT="3";
    private String RIGHT="4";
    private String STOP="0";

    Button btn_go;
    Button btn_back;
    Button btn_left;
    Button btn_right;
    Button btn_stop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_control);

//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        btn_go = (Button) this.findViewById(R.id.btn_go);
        btn_back = (Button) this.findViewById(R.id.btn_back);
        btn_left = (Button) this.findViewById(R.id.btn_left);
        btn_right = (Button) this.findViewById(R.id.btn_right);
        btn_stop = (Button) this.findViewById(R.id.btn_stop);

        btn_go.setOnClickListener(new ClickEvent());
        btn_back.setOnClickListener(new ClickEvent());
        btn_left.setOnClickListener(new ClickEvent());
        btn_right.setOnClickListener(new ClickEvent());
        btn_stop.setOnClickListener(new ClickEvent());

        getActionBar().setTitle(mDeviceName);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        Log.d(TAG, "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(!mConnected){
                Toast.makeText(CarControlActivity.this,"断连",Toast.LENGTH_SHORT).show();
                return;
            }
            if (v == btn_go) {
                mBluetoothLeService.WriteValue(GO);
            }else if(v == btn_back){
                mBluetoothLeService.WriteValue(BACK);
            }else if(v == btn_left){
                mBluetoothLeService.WriteValue(LEFT);
            }else if(v == btn_right){
                mBluetoothLeService.WriteValue(RIGHT);
            }else if(v == btn_stop){
                mBluetoothLeService.WriteValue(STOP);
            }
        }

    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
            Log.e(TAG, "mBluetoothLeService is okay");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            	Log.e(TAG, "Only gatt, just wait");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
            	mConnected = true;
            	ShowDialog();
            	Log.e(TAG, "In what we need");
            	invalidateOptionsMenu();
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	Log.e(TAG, "RECV DATA");
            	String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
            	if (data != null) {

                }
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
//        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBluetoothLeService != null)
        {
        	mBluetoothLeService.close();
        	mBluetoothLeService = null;
        }
        if(mConnected)
        {
            mBluetoothLeService.disconnect();
            mConnected = false;
        }
        Log.d(TAG, "We are in destroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
            	if(mConnected)
            	{
            		mBluetoothLeService.disconnect();
            		mConnected = false;
            	}
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ShowDialog()
    {
    	Toast.makeText(this, "已连接", Toast.LENGTH_SHORT).show();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        return intentFilter;
    }
}

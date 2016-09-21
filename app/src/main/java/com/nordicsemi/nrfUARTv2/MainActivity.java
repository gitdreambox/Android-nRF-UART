
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.nordicsemi.nrfUARTv2;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.Logger;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener, GizwitsServiceCallbacks {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private ILogSession mLogSession;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect, btnSyncInfo, btnAuth, btnDataPoint, btnOTA, btnShowLog;
    private ProtocolPacket TxPacket, RxPacket;
    private ArrayList<Byte> buffer = new ArrayList<Byte>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        btnSyncInfo = (Button) findViewById(R.id.btnSyncInfo);
        btnAuth = (Button) findViewById(R.id.btnAuth);
        btnDataPoint = (Button) findViewById(R.id.btnDataPoint);
        btnOTA = (Button) findViewById(R.id.btnOTA);
        btnShowLog = (Button) findViewById(R.id.btnShowLog);
        service_init();
        TxPacket = new ProtocolPacket(this);
        TxPacket.setGattCallbacks(this);
        RxPacket = new ProtocolPacket(this);
        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();

                        }
                    }
                }
            }
        });
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSyncInfo:
                SyncInfo();
                break;
            case R.id.btnAuth:
                Auth();
                break;
            case R.id.btnOTA:
                OTA();
                break;
            case R.id.btnDataPoint:
                byte[] b=new byte[]{0x05,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F};
                DataPoint(b);
                break;
            case R.id.btnShowLog:
                if (mLogSession != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionUri());//Show log entries
                    //Intent intent = new Intent(Intent.ACTION_VIEW, ((LogSession) mLogSession).getSessionsUri());//Show all log sessions
                    startActivity(intent);
                }
                break;
            default:
                break;
        }
    }

    private void printLog(String log) {
        Logger.i(mLogSession, log);
        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        listAdapter.add("[" + currentDateTimeString + "] " + log);
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service 
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            if (action.equals(ProtocolPacket.ACTION_LOG)) {
                final String log = intent.getStringExtra(ProtocolPacket.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        //printLog(log);
                        Logger.d(mLogSession, log);
                    }
                });
            }
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        btnSyncInfo.setEnabled(true);
                        btnAuth.setEnabled(true);
                        btnDataPoint.setEnabled(true);
                        btnOTA.setEnabled(true);
                        btnShowLog.setEnabled(true);
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - ready");
                        listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mDevice.getName());
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        btnSyncInfo.setEnabled(false);
                        btnAuth.setEnabled(false);
                        btnDataPoint.setEnabled(false);
                        btnOTA.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

                    }
                });
            }
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                TxPacket.addRecvData(txValue);
            }
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(ProtocolPacket.ACTION_LOG);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);
                    mLogSession = Logger.newSession(getApplication(), mDevice.getAddress(), mDevice.getName());
                    Logger.a(mLogSession, "a");
                    Logger.d(mLogSession, "d");
                    Logger.e(mLogSession, "e");
                    Logger.i(mLogSession, "i");
                    Logger.v(mLogSession, "v");
                    Logger.w(mLogSession, "w");
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }


    @Override
    public void SyncInfoResponse(String productKey, String mac, String hardwareVersion, String softwareVersion, String protocolVersion) {
        printLog("SyncInfoRes:"
                + "\r\nproductKey=" + productKey
                + "\r\nmac=" + mac
                + "\r\nhardwareVersion=" + hardwareVersion
                + "\r\nsoftwareVersion=" + softwareVersion
                + "\r\nprotocolVersion=" + protocolVersion
                + "\r\n"
        );
    }

    @Override
    public void AuthResponse(boolean encrypt, int random) {
        printLog("AuthRes:"
                + "\r\nencrypt=" + encrypt
                + "\r\nrandom=" + random
                + "\r\n"
        );
    }

    @Override
    public void OTAResponse(int statusCode) {
        printLog("OTARes:"
                + "\r\nstatusCode=" + statusCode
                + "\r\n"
        );
    }

    @Override
    public void LogResponse(String log) {
        printLog("Log:"+log+ "\r\n");
    }

    @Override
    public void DataPointResponse(byte[] data) {
        printLog("SyncInfoRes:"
                + "\r\ndata=" + hexUtils.bytesToHexString(data)
                + "\r\n"
        );
    }
    public static final int PLATFORM_IOS = 0x00;
    public static final int PLATFORM_ANDROID = 0x01;
    public static final int PLATFORM_WP = 0x02;
    public static final int PLATFORM_OTHER = 0x03;
    private void SyncInfo() {
        SyncInfo(PLATFORM_ANDROID, System.currentTimeMillis() / 1000, "AABBCCDDEEFF00112233445566778899");
    }

    private void SyncInfo(int platformType, long timestamp, String UID) {
        RxPacket.setValue(platformType, ProtocolPacket.FORMAT_UINT8);
        RxPacket.setValue(timestamp, ProtocolPacket.FORMAT_UINT32);
        RxPacket.setValue(hexUtils.hexStringToBytes(UID.substring(0, 32)));
        mService.writeRXCharacteristic(RxPacket.getValue(0x01));
        printLog("SyncInfoReq:"
                + "\r\nplatformType=" + platformType
                + "\r\ntimestamp=" + timestamp
                + "\r\nUID=" + UID
                + "\r\n"
        );
    }

    private void Auth() {
        byte[] mac = hexUtils.hexStringToBytes(mDevice.getAddress().replace(":", ""));
        byte[] sessionKey = new byte[16];
        int random = (int) (Math.random() * 65535);
        for (int i = 0; i < 16; i++) {
            sessionKey[i] = (byte) (Math.random() * 256);
        }
        Auth(mac, sessionKey, random);
    }

    private void Auth(byte[] mac, byte[] sessionKey, int random) {
        RxPacket.setValue(mac, 0, 6);
        RxPacket.setValue(sessionKey, 0, 16);
        RxPacket.setValue(random, ProtocolPacket.FORMAT_UINT32);
        mService.writeRXCharacteristic(RxPacket.getValue(0x02));
        printLog("AuthReq:"
                + "\r\nmac=" + hexUtils.bytesToHexString(mac, 0, 6)
                + "\r\nsessionKey=" + hexUtils.bytesToHexString(sessionKey, 0, 16)
                + "\r\nrandom=" + random
                + "\r\n"
        );
    }

    private void OTA() {
        mService.writeRXCharacteristic(RxPacket.getValue(0x03));
        printLog("OTAReq:"+"\r\n");
    }

    private void DataPoint(byte[] b) {
        RxPacket.setValue(b);
        mService.writeRXCharacteristic(RxPacket.getValue(0x05));
        printLog("DataPointReq:"
                +"\r\ndata="+hexUtils.bytesToHexString(b)
                + "\r\n"
        );
    }
}

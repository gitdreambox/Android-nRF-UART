package com.nordicsemi.nrfUARTv2;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class DFUActivity extends Activity {

    Button btnUpdate;
    private ProgressBar mProgressBar;
    private TextView mTextPercentage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dfu);
        //btnUpdate=(Button) findViewById(R.id.btnUpdate);
        mTextPercentage = (TextView) findViewById(R.id.progress_status);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        startDFU();
    }
    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_connecting);
        }

        @Override
        public void onDfuProcessStarting(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_starting);
        }

        @Override
        public void onEnablingDfuMode(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_switching_to_dfu);
        }

        @Override
        public void onFirmwareValidating(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_validating);
        }

        @Override
        public void onDeviceDisconnecting(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_disconnecting);
        }

        @Override
        public void onDfuCompleted(final String deviceAddress) {
            mTextPercentage.setText(R.string.dfu_status_completed);
            // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //onTransferCompleted();

                    // if this activity is still open and upload process was completed, cancel the notification
                    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(DfuService.NOTIFICATION_ID);
                }
            }, 200);
            finish();
        }

        @Override
        public void onDfuAborted(final String deviceAddress) {
            mTextPercentage.setText(R.string.dfu_status_aborted);
            // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //onUploadCanceled();

                    // if this activity is still open and upload process was completed, cancel the notification
                    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(DfuService.NOTIFICATION_ID);
                }
            }, 200);
        }

        @Override
        public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(percent);
            mTextPercentage.setText(String.format("%d%%", percent));
//            if (partsTotal > 1)
//                mTextUploading.setText(getString("Uploading part %d/%d…", currentPart, partsTotal));
//            else
//                mTextUploading.setText("Uploading…");
        }

        @Override
        public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
            showErrorMessage(message);

            // We have to wait a bit before canceling notification. This is called before DfuService creates the last notification.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // if this activity is still open and upload process was completed, cancel the notification
                    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(DfuService.NOTIFICATION_ID);
                }
            }, 200);
            finish();
        }
    };
    private void showErrorMessage(final String message) {
        showToast("Upload failed: " + message);
    }
    private void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void startDFU(){
        String MAC=getIntent().getStringExtra("mac");
        mProgressBar.setVisibility(View.VISIBLE);
        mTextPercentage.setVisibility(View.VISIBLE);
        mTextPercentage.setText("");
        final DfuServiceInitiator starter = new DfuServiceInitiator(MAC)
                .setDeviceName("Gizwits BLE")
                .setKeepBond(false)
                .setForceDfu(false)
                .setPacketsReceiptNotificationsEnabled(true)
                .setPacketsReceiptNotificationsValue(12);
        starter.setZip(null, "/storage/emulated/0/Tencent/QQfile_recv/DFU_nrf51822_xxaa_s130_20161102.zip");
//        // Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
//        // In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
//        if (mFileType == DfuService.TYPE_AUTO)
//            starter.setZip(null, "/storage/emulated/0/Tencent/QQfile_recv/DFU_nrf51822_xxaa_s130_20161026.zip");
//        else {
//            starter.setBinOrHex(mFileType, mFileStreamUri, mFilePath).setInitFile(mInitFileStreamUri, mInitFilePath);
//        }
        final DfuServiceController controller = starter.start(this, DfuService.class);
        // You may use the controller to pause, resume or abort the DFU process.
    }
    @Override
    protected void onPause() {
        //Log.d(TAG, "onPause");
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }
    @Override
    public void onResume() {
        super.onResume();
//        Log.d(TAG, "onResume");
//        if (!mBtAdapter.isEnabled()) {
//            Log.i(TAG, "onResume - BT not enabled yet");
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//        }
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);

    }
}

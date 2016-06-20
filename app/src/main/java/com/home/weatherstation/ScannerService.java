package com.home.weatherstation;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScannerService extends Service {
    public static final String INITIALIZE = "com.home.weatherstation.action.initialize";
    public static final String SCAN = "com.home.weatherstation.action.scan";
    public static final String STOP = "com.home.weatherstation.action.stop";
    private static final String TAG = ScannerService.class.getSimpleName();

    private static final String DEVICE_NO8_MAC_ADDRESS = "D3:60:FB:B2:D1:39";
    private static final String DEVICE_NO9_MAC_ADDRESS = "FA:67:91:00:D7:B2";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> scanFilters;

    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 20 seconds.
    private static final long SCAN_PERIOD = 20000;

    public ScannerService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCreate() {
        mHandler = new Handler();

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        enableBT();

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(500)
                .build();

        scanFilters = new ArrayList<ScanFilter>();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action;
        if (intent == null) {
            action = INITIALIZE;
        }

        action = intent.getAction();

        if (INITIALIZE.equals(action)) {
            scheduleScans();
        } else if (SCAN.equals(action)) {
            scanAndUpload();
        }

        return START_REDELIVER_INTENT;
    }


    private void scheduleScans() {
        Log.i(TAG, "Scheduling scans ...");
    }

    private void scanAndUpload() {
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mLEScanner.flushPendingScanResults(mScanCallback);
                    mLEScanner.stopScan(mScanCallback);
                    Log.i(TAG, "Scanner stopped");
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLEScanner.startScan(scanFilters, settings, mScanCallback);


        } else {
            mScanning = false;
            mLEScanner.flushPendingScanResults(mScanCallback);
            mLEScanner.stopScan(mScanCallback);
            Log.i(TAG, "Scanner stopped");
        }

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG, "onScanResult: callbackType = " + String.valueOf(callbackType));
            Log.i(TAG, "onScanResult: result = " + result.toString());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Date now = new Date();

            Sample deviceNr8 = null;
            Sample deviceNr9 = null;
            for (ScanResult result : results) {
                Log.i(TAG, "onBachScanResult: Results" + result.toString());

                if (DEVICE_NO8_MAC_ADDRESS.equals(result.getDevice().getAddress())) {
                    deviceNr8 = parse(result.getScanRecord(), now);
                } else if (DEVICE_NO9_MAC_ADDRESS.equals(result.getDevice().getAddress())) {
                    deviceNr9 = parse(result.getScanRecord(), now);
                }

            }

            if (deviceNr8 == null || deviceNr9 == null) {
                Log.w(TAG, "Did not receive results from both devices! DeviceNo8="+deviceNr8 + ", DeviceNo9="+deviceNr9);
            } else {
                process(deviceNr8, deviceNr9);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "onScanFailed: Error Code: " + errorCode);
        }
    };

    private void process(final Sample sampleDeviceNo8, final Sample sampleDeviceNo9) {
        Log.i(TAG, "Processing samples\n"+sampleDeviceNo8+"\n"+sampleDeviceNo9);
        UploadService.startUpload(this, sampleDeviceNo8, sampleDeviceNo9);
    }

    private void enableBT() {
        // TODO implement automatically enable BT
    }

    private Sample parse(ScanRecord record, Date date) {

        byte[] manufacturerSpecData = record.getManufacturerSpecificData().valueAt(0);

        if (manufacturerSpecData == null) {
            Log.w(TAG, "ManufacturerSpecificData is null");
            return null;
        }

        ByteBuffer bytes = ByteBuffer.wrap(manufacturerSpecData).order(ByteOrder.LITTLE_ENDIAN);

        bytes.get();                          // ? flag
        short tempLowest = bytes.getShort();  // temp*10 (lowest)
        short tempCurrent = bytes.getShort(); // temp*10 (current)
        short tempHighest = bytes.getShort(); // temp*10 (highest)
        byte humidity = bytes.get();          // humidity in %
        short pressure = bytes.getShort();    // pressure
        //bed.getLong());                     // unknown 8 byte

        return new Sample(date, record.getDeviceName(), (float) tempCurrent / 10, (float) tempLowest / 10, (float) tempHighest / 10, (int) humidity, (int) pressure);
    }

}

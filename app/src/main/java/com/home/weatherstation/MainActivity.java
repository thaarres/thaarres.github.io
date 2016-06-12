package com.home.weatherstation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> scanFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        TextView timelabel = (TextView) findViewById(R.id.timelabel);
        Date now = new Date();
        timelabel.setText("Hello again. Time is " + now);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        enableBT();

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanFilters = new ArrayList<ScanFilter>();

        scanLeDevice(true);
    }

    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLEScanner.startScan(scanFilters, settings, mScanCallback);


        } else {
            mScanning = false;
            mLEScanner.stopScan(mScanCallback);
        }

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            Sample sample = parse(result.getScanRecord());
            Log.i("Received ", "" + sample);

            if (sample != null) {
                process(sample);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private void process(final Sample sample) {
        Log.i("Process", "Processing sample ...");
    }

    private void enableBT() {
        // TODO implement automatically enable BT
    }

    private Sample parse(ScanRecord record) {

        byte[] manufacturerSpecData = record.getManufacturerSpecificData().valueAt(0);

        ByteBuffer bytes = ByteBuffer.wrap(manufacturerSpecData).order(ByteOrder.LITTLE_ENDIAN);

        bytes.get();                          // ? flag
        short tempLowest = bytes.getShort();  // temp*10 (lowest)
        short tempCurrent = bytes.getShort(); // temp*10 (current)
        short tempHighest = bytes.getShort(); // temp*10 (highest)
        byte humidity = bytes.get();          // humidity in %
        short pressure = bytes.getShort();    // pressure
        //bed.getLong());                     // unknown 8 bytes

        if (record.getDeviceName() == null || record.getDeviceName().isEmpty()) {
            return null;
        }

        return new Sample(new Date(), record.getDeviceName(), (float)tempCurrent/10, (float)tempLowest/10, (float)tempHighest/10, (short)humidity, pressure);
    }
}

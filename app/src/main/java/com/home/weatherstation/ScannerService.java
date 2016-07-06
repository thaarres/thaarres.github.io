package com.home.weatherstation;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ScannerService extends Service {
    public static final String START_SCHEDULER = "com.home.weatherstation.action.start_scheduled_scans";
    public static final String STOP_SCHEDULER = "com.home.weatherstation.action.stop_scheduled_scans";
    public static final String SCAN_AND_UPLOAD = "com.home.weatherstation.action.scan_and_upload";

    private static final String TAG = ScannerService.class.getSimpleName();

    private static final String DEVICE_NO8_MAC_ADDRESS = "D3:60:FB:B2:D1:39";
    private static final String DEVICE_NO9_MAC_ADDRESS = "FA:67:91:00:D7:B2";


    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> scanFilters = new ArrayList<>();

    private Handler mHandler;

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    // results from scanner, poor mans simple caching approach...
    Sample deviceNr8 = null;
    Sample deviceNr9 = null;

    // Stops scanning after 20 seconds.
    private static final long SCAN_PERIOD = 20000;
    private Runnable stopScanAndProcessRunnable = new Runnable() {
        @Override
        public void run() {
            stopScanAndProcessResults();
        }
    };

    public static Intent buildStartSchedulerIntent(Context context) {
        Intent serviceIntent = new Intent(context, ScannerService.class);
        serviceIntent.setAction(ScannerService.START_SCHEDULER);
        return serviceIntent;
    }

    public static Intent buildStopSchedulerIntent(Context context) {
        Intent serviceIntent = new Intent(context, ScannerService.class);
        serviceIntent.setAction(ScannerService.STOP_SCHEDULER);
        return serviceIntent;
    }

    public static Intent buildScanAndUploadAndScheduleNextIntent(Context context) {
        Intent serviceIntent = buildScanAndUploadIntent(context);
        serviceIntent.putExtra("schedule_next", true);
        return serviceIntent;
    }

    public static Intent buildScanAndUploadIntent(Context context) {
        Intent serviceIntent = new Intent(context, ScannerService.class);
        serviceIntent.setAction(ScannerService.SCAN_AND_UPLOAD);
        return serviceIntent;
    }

    public ScannerService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onCreate() {
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        mHandler = new Handler();

        // Initializes Bluetooth adapter.
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        enableBT();

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                //.setReportDelay(500)
                .build();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action;
        boolean scheduleNext;
        if (intent == null) {
            action = START_SCHEDULER;
            scheduleNext = false;
        } else {
            action = intent.getAction();
            scheduleNext = intent.hasExtra("schedule_next");
        }

        if (START_SCHEDULER.equals(action)) {
            scheduleNextScanAndUpload();
        } else if (STOP_SCHEDULER.equals(action)) {
            cancelNextScanAndUpload();
        } else if (SCAN_AND_UPLOAD.equals(action)) {
            scanAndUpload();
            if (scheduleNext) {
                scheduleNextScanAndUpload();
            }
        }

        return START_REDELIVER_INTENT;
    }


    private void scheduleNextScanAndUpload() {
        Log.i(TAG, "Scheduling next scan ...");
        alarmIntent = PendingIntent.getService(this, 0, buildScanAndUploadAndScheduleNextIntent(this), 0);
        long triggerTime = calculateNextHalfHourInMillis();
        alarmMgr.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerTime, PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0)), alarmIntent);
        Log.i(TAG, "Next scan scheduled at " + new Date(triggerTime));
        Storage.storeNextScheduledScanTime(this, triggerTime);
    }

    private static long calculateNextHalfHourInMillis() {
        Calendar cal = Calendar.getInstance();
        int currentHourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        int currentMinuteOfHour = cal.get(Calendar.MINUTE);

        cal.set(Calendar.MINUTE, 0); // reset to 00m:00s
        cal.set(Calendar.SECOND, 0);

        if (currentMinuteOfHour >= 0 && currentMinuteOfHour < 30) {
            cal.add(Calendar.MINUTE, 30);
        } else if (currentMinuteOfHour >= 30 && currentMinuteOfHour <= 59) {
            cal.add(Calendar.HOUR, 1);
        } else {
            throw new IllegalArgumentException("currentHour=" + currentHourOfDay + ", currentMin=" + currentMinuteOfHour);
        }

        return cal.getTimeInMillis();
    }

    private void cancelNextScanAndUpload() {
        if (alarmIntent != null) {
            Log.i(TAG, "Canceling scans ...");
            alarmMgr.cancel(alarmIntent);
            alarmIntent.cancel();
            alarmIntent = null;
        }
    }

    private void scanAndUpload() {
        Authenticator authenticator = new Authenticator(getBaseContext());
        authenticator.invalidateToken();
        authenticator.requestToken(new AuthenticatorCallback() {
            @Override
            public void doCoolAuthenticatedStuff() {
                scanLeDevice(true);
            }

            @Override
            public void failed() {
                Log.e(TAG, "Could not start scan because authentication failed!");
            }
        });

    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(stopScanAndProcessRunnable, SCAN_PERIOD);

            resetCachedSampleData();
            mLEScanner.startScan(scanFilters, settings, mScanCallback);
        } else {
            stopScanAndProcessResults();
        }

    }

    private void resetCachedSampleData() {
        deviceNr8 = null; // reset samples!
        deviceNr9 = null; // reset samples!
    }

    private boolean hasSampleData() {
        return deviceNr8 != null && deviceNr9 != null;
    }


    private void stopScanAndProcessResults() {
        mLEScanner.flushPendingScanResults(mScanCallback);
        mLEScanner.stopScan(mScanCallback);
        Log.i(TAG, "Scanner stopped");
        process();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG, "onScanResult: Result = " + result.toString());
            cacheSample(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                Log.i(TAG, "onBachScanResult: Result = " + result.toString());
                cacheSample(result);
            }
        }

        private void cacheSample(ScanResult result) {
            if (DEVICE_NO8_MAC_ADDRESS.equals(result.getDevice().getAddress())) {
                deviceNr8 = parse(result.getScanRecord(), new Date());
            } else if (DEVICE_NO9_MAC_ADDRESS.equals(result.getDevice().getAddress())) {
                deviceNr9 = parse(result.getScanRecord(), new Date());
            }
            if (hasSampleData()) {
                mHandler.removeCallbacks(stopScanAndProcessRunnable);
                stopScanAndProcessResults();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "onScanFailed: Error Code: " + errorCode);
        }
    };

    private void process() {
        long now = System.currentTimeMillis();
        Storage.storeLastScanTime(getBaseContext(), now);

        if (hasSampleData()) {
            Storage.storeLastSuccessfulScanTime(getBaseContext(), now);
            Date timestamp = deviceNr8.getTimestamp();
            Log.i(TAG, "Processing samples timestamp=" + timestamp + "\n" + deviceNr8 + "\n" + deviceNr9);
            UploadService.startUpload(this, timestamp, deviceNr8, deviceNr9);
        } else {
            Log.w(TAG, "Did not receive results from both devices! DeviceNo8=" + deviceNr8 + ", DeviceNo9=" + deviceNr9);
        }
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


    public static long getNextScheduled(Context context) {
        AlarmManager.AlarmClockInfo nextAlarmClock = ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).getNextAlarmClock();
        return nextAlarmClock != null ? nextAlarmClock.getTriggerTime() : -1;
    }
}

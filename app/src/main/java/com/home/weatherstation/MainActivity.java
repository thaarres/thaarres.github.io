package com.home.weatherstation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 1001;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;

    private BluetoothGatt mGatt;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
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
//            connectToDevice(btDevice);

            parse(result.getScanRecord());
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



    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                Log.i("onServicesDiscovered", service.getUuid().toString());
                if (service.getUuid().toString().equals("20652000-02f3-4f75-848f-323ac2a6af8a")) {
                    Log.i(" onServicesDiscovered", "found characteristics ...");

                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic c : characteristics) {
                        Log.i("  onServicesDiscovered", c.getUuid().toString() + ", permissions="+c.getPermissions());

                        if (c.getUuid().toString().equals("20653012-02f3-4f75-848f-323ac2a6af8a")) {
                            Log.i("   onServicesDiscovered", "reading characteristic ...");
                            gatt.readCharacteristic(c);
                        }

                    }

                }

            }


        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", asString(characteristic));
            gatt.disconnect();
        }

        private String asString(BluetoothGattCharacteristic characteristic) {

                int lsb = characteristic.getValue()[0] & 0xff;
                Log.d("CHAR", String.valueOf(lsb));

                Log.d("CHAR", "" + characteristic.getProperties());

            return "";
        }
    };

    private void enableBT() {
        // TODO implement automatically enable BT
    }

private void parse(ScanRecord record) {
//    Map<Integer, String> recordData = ParseRecord(record.getBytes());
//    System.out.println(recordData);
    //
//    System.out.println("#####");
//    System.out.println("#####");
//    System.out.println("#####");
    //
    printScanRecord(record.getBytes());
}

    /*
    BLE Scan record type IDs
    data from:
    https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
    */
    static final int EBLE_FLAGS           = 0x01;//«Flags»	Bluetooth Core Specification:
    static final int EBLE_16BitUUIDInc    = 0x02;//«Incomplete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_16BitUUIDCom    = 0x03;//«Complete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_32BitUUIDInc    = 0x04;//«Incomplete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_32BitUUIDCom    = 0x05;//«Complete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitUUIDInc   = 0x06;//«Incomplete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitUUIDCom   = 0x07;//«Complete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    static final int EBLE_SHORTNAME       = 0x08;//«Shortened Local Name»	Bluetooth Core Specification:
    static final int EBLE_LOCALNAME       = 0x09;//«Complete Local Name»	Bluetooth Core Specification:
    static final int EBLE_TXPOWERLEVEL    = 0x0A;//«Tx Power Level»	Bluetooth Core Specification:
    static final int EBLE_DEVICECLASS     = 0x0D;//«Class of Device»	Bluetooth Core Specification:
    static final int EBLE_SIMPLEPAIRHASH  = 0x0E;//«Simple Pairing Hash C»	Bluetooth Core Specification:​«Simple Pairing Hash C-192»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_SIMPLEPAIRRAND  = 0x0F;//«Simple Pairing Randomizer R»	Bluetooth Core Specification:​«Simple Pairing Randomizer R-192»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_DEVICEID        = 0x10;//«Device ID»	Device ID Profile v1.3 or later,«Security Manager TK Value»	Bluetooth Core Specification:
    static final int EBLE_SECURITYMANAGER = 0x11;//«Security Manager Out of Band Flags»	Bluetooth Core Specification:
    static final int EBLE_SLAVEINTERVALRA = 0x12;//«Slave Connection Interval Range»	Bluetooth Core Specification:
    static final int EBLE_16BitSSUUID     = 0x14;//«List of 16-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    static final int EBLE_128BitSSUUID    = 0x15;//«List of 128-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    static final int EBLE_SERVICEDATA     = 0x16;//«Service Data»	Bluetooth Core Specification:​«Service Data - 16-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_PTADDRESS       = 0x17;//«Public Target Address»	Bluetooth Core Specification:
    static final int EBLE_RTADDRESS       = 0x18;;//«Random Target Address»	Bluetooth Core Specification:
    static final int EBLE_APPEARANCE      = 0x19;//«Appearance»	Bluetooth Core Specification:
    static final int EBLE_DEVADDRESS      = 0x1B;//«​LE Bluetooth Device Address»	​Core Specification Supplement, Part A, section 1.16
    static final int EBLE_LEROLE          = 0x1C;//«​LE Role»	​Core Specification Supplement, Part A, section 1.17
    static final int EBLE_PAIRINGHASH     = 0x1D;//«​Simple Pairing Hash C-256»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_PAIRINGRAND     = 0x1E;//«​Simple Pairing Randomizer R-256»	​Core Specification Supplement, Part A, section 1.6
    static final int EBLE_32BitSSUUID     = 0x1F;//​«List of 32-bit Service Solicitation UUIDs»	​Core Specification Supplement, Part A, section 1.10
    static final int EBLE_32BitSERDATA    = 0x20;//​«Service Data - 32-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_128BitSERDATA   = 0x21;//​«Service Data - 128-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    static final int EBLE_SECCONCONF      = 0x22;//​«​LE Secure Connections Confirmation Value»	​Core Specification Supplement Part A, Section 1.6
    static final int EBLE_SECCONRAND      = 0x23;//​​«​LE Secure Connections Random Value»	​Core Specification Supplement Part A, Section 1.6​
    static final int EBLE_3DINFDATA       = 0x3D;//​​«3D Information Data»	​3D Synchronization Profile, v1.0 or later
    static final int EBLE_MANDATA         = 0xFF;//«Manufacturer Specific Data»	Bluetooth Core Specification:

    /*
    BLE Scan record parsing
    inspired by:
    http://stackoverflow.com/questions/22016224/ble-obtain-uuid-encoded-in-advertising-packet
     */
    static public  Map <Integer,String>  ParseRecord(byte[] scanRecord){
        Map <Integer,String> ret = new HashMap<Integer,String>();
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Zero value indicates that we are done with the record now
            if (length == 0) break;

            int type = scanRecord[index];
            //if the type is zero, then we are pass the significant section of the data,
            // and we are thud done
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            if(data != null && data.length > 0) {
                StringBuilder hex = new StringBuilder(data.length * 2);
                // the data appears to be there backwards
                for (int bb = data.length- 1; bb >= 0; bb--){
                    hex.append(String.format("%02X", data[bb]));
                }
                ret.put(type,hex.toString());
            }
            index += length;
        }

        return ret;
    }

    static public String getServiceUUID(Map<Integer,String> record){
        String ret = "";
        // for example: 0105FACB00B01000800000805F9B34FB --> 010510ee-0000-1000-8000-00805f9b34fb
        if(record.containsKey(EBLE_128BitUUIDCom)){
            String tmpString= record.get(EBLE_128BitUUIDCom).toString();
            ret = tmpString.substring(0, 8) + "-" + tmpString.substring(8,12)+ "-" + tmpString.substring(12,16)+ "-" + tmpString.substring(16,20)+ "-" + tmpString.substring(20,tmpString.length());
            //010510EE --> 010510ee-0000-1000-8000-00805f9b34fb
        }else if(record.containsKey(EBLE_32BitUUIDCom)){
            ret = record.get(EBLE_32BitUUIDCom).toString() + "-0000-1000-8000-00805f9b34fb";
        }
        return ret;
    }

    /////
    public void printScanRecord (byte[] scanRecord) {

        // Simply print all raw bytes
        try {
            String decodedRecord = new String(scanRecord,"UTF-8");
            Log.d("DEBUG","decoded String : " + ByteArrayToString(scanRecord));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Parse data bytes into individual records
        List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);


        // Print individual records
        if (records.size() == 0) {
            Log.i("DEBUG", "Scan Record Empty");
        } else {
            Log.i("DEBUG", "Scan Record: " + TextUtils.join(",", records));
        }

    }


    public static String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");

        return hex.toString();
    }

    public static void main(String[] args) {
        byte[] type9bed1 = {78, 111, 46, 56, 124, 66, 101, 100, 114, 111, 111, 109, 32, 32}; // 20206D6F6F726465427C382E6F4E
        byte[] type9bed2 = {78, 111, 46, 56, 124, 66, 101, 100, 114, 111, 111, 109, 32, 32}; //
        byte[] type9bed3 = {78, 111, 46, 56, 124, 66, 101, 100, 114, 111, 111, 109, 32, 32}; //
        byte[] type1bed1 = {6}; // 06
        byte[] type1bed2 = {6}; //
        byte[] type1bed3 = {6}; //
        byte[] type2bed1 = {15, 24, 10, 24}; // 180A180F
        byte[] type2bed2 = {15, 24, 10, 24}; //
        byte[] type2bed3 = {15, 24, 10, 24}; //
        byte[] typeMinus1bed1 = {51, 1, 1, -14, 0, -14, 0, -13, 0, 51, -63, 3, 57, -47, -78, -5, 96, -45, 7, 0}; // 0007D360FBB2D13903C13300F300F200F2010133
        byte[] typeMinus1bed2 = {51, 1, 1, -17, 0, -16, 0, -14, 0, 51, -62, 3, 57, -47, -78, -5, 96, -45, 7, 0}; //
        byte[] typeMinus1bed3 = {51, 1, 1, -64, 0, -64, 0, -14, 0, 49, -63, 3, 57, -47, -78, -5, 96, -45, 7, 0}; //
        byte[] typeMinus1bed4 = {51, 1, 1, -114, 0, -114, 0, -71, 0, 50, -63, 3, 57, -47, -78, -5, 96, -45, 7, 0}; // 961, 50, 14.3
        byte[] typeMinus1bed5 = {51, 1, 1, 119, 0, 119, 0, -71, 0, 50, -63, 3, 57, -47, -78, -5, 96, -45, 7, 0}; // 961, 50, 11.9
        byte[] typeMinus1bed6 = {51, 1, 1, 95, 0, 95, 0, -71, 0, 53, -63, 3, 57, -47, -78, -5, 96, -45, 7, 0}; // 961, 53, 9.4
        byte[] typeMinus1bed7 = {51, 1, 1, 66, 0, 66, 0, 69, 0, 57, -63, 3, 57, -47, -78, -5, 96, -45, 7, 0}; // 961, 57, 6.5
        byte[] typeMinus1bed8 = {51, 1, 1, 61, 0, -90, 0, -90, 0, 62, -63, 3, 57, -47, -78, -5, 96, -45, 7, 0}; // 961, 62, 17.1

        byte[] type9liv1 = {78, 111, 46, 57, 124, 76, 105, 118, 105, 110, 103, 32, 114, 46}; // 2E7220676E6976694C7C392E6F4E
        byte[] type9liv2 = {78, 111, 46, 57, 124, 76, 105, 118, 105, 110, 103, 32, 114, 46}; //
        byte[] type9liv3 = {78, 111, 46, 57, 124, 76, 105, 118, 105, 110, 103, 32, 114, 46}; //
        byte[] type1liv1 = {6}; // 06
        byte[] type1liv2 = {6}; //
        byte[] type1liv3 = {6}; //
        byte[] type2liv1 = {15, 24, 10, 24}; // 180A180F
        byte[] type2liv2 = {15, 24, 10, 24}; //
        byte[] type2liv3 = {15, 24, 10, 24}; //
        byte[] typeMinus1liv1 = {51, 1, 1, -16, 0, -16, 0, -13, 0, 53, -62, 3, -78, -41, 0, -111, 103, -6, 7, 0}; // 0007FA679100D7B203C23500F300F000F0010133
        byte[] typeMinus1liv2 = {51, 1, 1, -20, 0, -20, 0, -17, 0, 52, -62, 3, -78, -41, 0, -111, 103, -6, 7, 0}; //
        byte[] typeMinus1liv3 = {51, 1, 1, -20, 0, -20, 0, -19, 0, 52, -63, 3, -78, -41, 0, -111, 103, -6, 7, 0}; //
        byte[] typeMinus1liv4 = {51, 1, 1, -21, 0, -20, 0, -19, 0, 52, -63, 3, -78, -41, 0, -111, 103, -6, 7, 0}; // 961, 52, 23.6
        byte[] typeMinus1liv5 = {51, 1, 1, -21, 0, -20, 0, -19, 0, 52, -63, 3, -78, -41, 0, -111, 103, -6, 7, 0}; // 961, 52, 23.6
        byte[] typeMinus1liv6 = {51, 1, 1, -33, 0, -33, 0, -24, 0, 63, -63, 3, -78, -41, 0, -111, 103, -6, 7, 0}; // 961, 59, 21.5
        byte[] typeMinus1liv7 = {51, 1, 1, 114, 0, 114, 0, -24, 0, 56, -63, 3, -78, -41, 0, -111, 103, -6, 7, 0}; // 961, 56, 11.3
        byte[] typeMinus1liv8 = {51, 1, 1, 93, 0, -78, 0, -24, 0, 71, -63, 3, -78, -41, 0, -111, 103, -6, 7, 0}; // 961, 71, 18.0

//        System.out.println("LITTLE ENDIAN");
//        print(type9bed, type9liv, ByteOrder.LITTLE_ENDIAN, "type=9");
//        print(type1bed, type1liv, ByteOrder.LITTLE_ENDIAN, "type=1");
//        print(type2bed, type2liv, ByteOrder.LITTLE_ENDIAN, "type=2");
//        print(typeMinus1bed1, typeMinus1liv1, ByteOrder.LITTLE_ENDIAN, "type=-1");

//        System.out.println("BIG ENDIAN");
//        print(type9bed, type9liv, ByteOrder.BIG_ENDIAN, "type=9");
//        print(type1bed, type1liv, ByteOrder.BIG_ENDIAN, "type=1");
//        print(type2bed, type2liv, ByteOrder.BIG_ENDIAN, "type=2");
//        print(typeMinus1bed1, typeMinus1liv1, ByteOrder.BIG_ENDIAN, "type=-1");

//        System.out.println(ByteArrayToString(float2ByteArray(14.3f)));
//        System.out.println(ByteArrayToString(float2ByteArray(11.9f)));
//        System.out.println(ByteArrayToString(float2ByteArray(9.2f)));
//        System.out.println(ByteArrayToString(float2ByteArray(9.3f)));
//        System.out.println(ByteArrayToString(float2ByteArray(9.4f)));
//        System.out.println(ByteArrayToString(float2ByteArray(9.5f)));
//        System.out.println(ByteArrayToString(float2ByteArray(9.6f)));
//        System.out.println(ByteArrayToString(float2ByteArray(9.7f)));
//        System.out.println(ByteArrayToString(float2ByteArray(9.8f)));
        System.out.println(ByteArrayToString(char2ByteArray((char)5)));
        System.out.println(ByteBuffer.wrap(new byte[] {65,22,102,102}).order(ByteOrder.BIG_ENDIAN).getFloat());

        System.out.println(ByteBuffer.wrap(new byte[] {65,22,102,102}).order(ByteOrder.BIG_ENDIAN).getFloat());

        System.out.println(new Integer(1).byteValue());
    }

    public static byte [] char2ByteArray (char value)
    {
        return ByteBuffer.allocate(2).putChar(value).array();
    }
    public static byte [] short2ByteArray (short value)
    {
        return ByteBuffer.allocate(2).putShort(value).array();
    }
    public static byte [] long2ByteArray (long value)
    {
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    public static byte [] float2ByteArray (float value)
    {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }

    private static void print(byte[] bbed, byte[] bliv, ByteOrder byteOrder, String msg) {
        ByteBuffer bed = ByteBuffer.wrap(bbed).order(byteOrder);
        ByteBuffer liv = ByteBuffer.wrap(bliv).order(byteOrder);

        System.out.println(msg + " char   " + bed.getChar());
        System.out.println(msg + " double " + bed.getDouble());
        System.out.println(msg + " float  " + bed.getFloat());
        System.out.println(msg + " int    " + bed.getInt());
        System.out.println(msg + " long   " + bed.getLong());
        System.out.println(msg + " shor   " + bed.getShort());
    }

    public static class AdRecord {

        public AdRecord(int length, int type, byte[] data) {
            String decodedRecord = "";
            try {
                decodedRecord = new String(data,"UTF-8");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Log.d("DEBUG", "Length: " + length + " Type : " + type + " Data : " + ByteArrayToString(data));
        }

        // ...

        public static List<AdRecord> parseScanRecord(byte[] scanRecord) {
            List<AdRecord> records = new ArrayList<AdRecord>();

            int index = 0;
            while (index < scanRecord.length) {
                int length = scanRecord[index++];
                //Done once we run out of records
                if (length == 0) break;

                int type = scanRecord[index];
                //Done if our record isn't a valid type
                if (type == 0) break;

                byte[] data = Arrays.copyOfRange(scanRecord, index+1, index+length);

                records.add(new AdRecord(length, type, data));
                //Advance
                index += length;
            }

            return records;
        }

        // ...
    }

}

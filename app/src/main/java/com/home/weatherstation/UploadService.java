package com.home.weatherstation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class UploadService extends IntentService {

    private static final String TAG = UploadService.class.getSimpleName();

    private static final String ACTION_UPLOAD = "com.home.weatherstation.action.upload";

    private static final String EXTRA_TIMESTAMP = "com.home.weatherstation.extra.timestamp";
    private static final String EXTRA_SAMPLE_DEVICE8 = "com.home.weatherstation.extra.sampledevice8";
    private static final String EXTRA_SAMPLE_DEVICE9 = "com.home.weatherstation.extra.sampledevice9";

    private static final String TEMPERATURE_TABLE_ID = "1jQ_Jnnw26pWU05sGBNdXbXlvxB-66_W4fuJgsTG7";
    private static final String API_KEY = "AIzaSyC6bt0RnAVIDwdj3eiSJBmrEPqTmQGDNkM";


    public UploadService() {
        super("UploadService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startUpload(Context context, final Date timestamp, final Sample sampleDeviceNo8, final Sample sampleDeviceNo9) {
        if (sampleDeviceNo8 == null || sampleDeviceNo9 == null) {
            Log.w(TAG, "Not starting upload because not all parameters set. sampleDeviceNo8=" + sampleDeviceNo8 + ", sampleDeviceNo9=" + sampleDeviceNo9);
            return;
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_UPLOAD);
        intent.putExtra(EXTRA_TIMESTAMP, timestamp.getTime());
        intent.putExtra(EXTRA_SAMPLE_DEVICE8, sampleDeviceNo8);
        intent.putExtra(EXTRA_SAMPLE_DEVICE9, sampleDeviceNo9);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                final Date timestamp = new Date(intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis()));
                final Sample sampleDevice8 = intent.getParcelableExtra(EXTRA_SAMPLE_DEVICE8);
                final Sample sampleDevice9 = intent.getParcelableExtra(EXTRA_SAMPLE_DEVICE9);
                upload(timestamp, sampleDevice8, sampleDevice9);
            } else {
                Log.w(TAG, "Unknown action: " + action);
            }
        }
    }

    private void upload(Date timestamp, Sample deviceNo8, Sample deviceNo9) {
        try {
            insert(TEMPERATURE_TABLE_ID, timestamp, deviceNo8.getTempCurrent(), deviceNo9.getTempCurrent());
            Storage.storeLastUploadTime(getBaseContext(), System.currentTimeMillis());
        } catch (IOException e) {
            Log.e(TAG, "Could not insert temperature data!", e);
        }
    }

    public void insert(String tableId, Date timestamp, float temperatureDevice8, float temperatureDevice9) throws IOException {

        // Encode the query
        String query = URLEncoder.encode("INSERT INTO " + tableId + " (Date,DeviceNo8,DeviceNo9) "
                + "VALUES ('" + android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp) + "', " + temperatureDevice8 + ", " + temperatureDevice9 + ")");
        URL url = new URL("https://www.googleapis.com/fusiontables/v2/query?sql=" + query + "&key=" + API_KEY);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + getToken());

        // read the response
        System.out.println("Response Code: " + conn.getResponseCode());
        InputStream in = new BufferedInputStream(conn.getInputStream());
        String response = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
        System.out.println(response);

    }

    private String getToken() {
        AuthPreferences authPreferences = new AuthPreferences(this);
        return authPreferences.getToken();
    }

}

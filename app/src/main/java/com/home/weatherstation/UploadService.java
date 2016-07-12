package com.home.weatherstation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;

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
    private static final String API_KEY_GOOGLE = "AIzaSyC6bt0RnAVIDwdj3eiSJBmrEPqTmQGDNkM";

    private static final String API_KEY_WUNDERGROUND = "6ad6fa3bdb22276d"; // https://www.wunderground.com/weather/api/d/6ad6fa3bdb22276d/edit.html
    private static final String WUNDERGROUND_STATION_URL = "https://api.wunderground.com/api/" + API_KEY_WUNDERGROUND + "/conditions/q/ch/zuerich-kreis-4-hard/zmw:00000.71.06660.json";

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
                final Sample sampleOutside = fetchCurrentConditionsOutside();
                upload(timestamp, sampleDevice8, sampleDevice9, sampleOutside);
            } else {
                Log.w(TAG, "Unknown action: " + action);
            }
        }
    }

    private void upload(Date timestamp, Sample deviceNo8, Sample deviceNo9, Sample sampleOutside) {
        int tries = 0;
        while (tries < 4) {
            tries++;
            try {
                insert(timestamp, deviceNo8.getTempCurrent(), deviceNo9.getTempCurrent(), sampleOutside.getTempCurrent());
                Storage.storeLastUploadTime(getBaseContext(), System.currentTimeMillis());
                return;
            } catch (IOException e) {
                Log.e(TAG, "Could not insert temperature data!", e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private Sample fetchCurrentConditionsOutside() {
        try {
            URL url = new URL(WUNDERGROUND_STATION_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // read the response
            Log.i(TAG, "Response Code: " + conn.getResponseCode());
            InputStream in = new BufferedInputStream(conn.getInputStream());
            String response = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
            Log.v(TAG, response);

            JSONObject obj = new JSONObject(response);
            JSONObject currentObservation = obj.getJSONObject("current_observation");
            Date d = new Date(currentObservation.getLong("observation_epoch") * 1000);
            float tempCurrent = Float.valueOf(currentObservation.getString("temp_c"));
            int relHumid = Integer.valueOf(currentObservation.getString("relative_humidity").replaceAll("%", ""));
            int pressure = currentObservation.getInt("pressure_in");

            return new Sample(d, "Outside", tempCurrent, 0, 0, relHumid, pressure);
        } catch (Exception e) {
            e.printStackTrace();
            return new Sample(new Date(), "Outside", 0, 0, 0, 0, 0);
        }

    }

    /**
     * Make sure there is a valid token available. See @link{com.home.weatherstation.Authenticator}
     */
    private void insert(Date timestamp, float temperatureDevice8, float temperatureDevice9, float temperatureOutside) throws IOException {

        // Encode the query
        String query = URLEncoder.encode("INSERT INTO " + TEMPERATURE_TABLE_ID + " (Date,DeviceNo8,DeviceNo9,Outside) "
                + "VALUES ('" + android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp) + "', " + temperatureDevice8 + ", " + temperatureDevice9 + ", " + temperatureOutside + ")");
        URL url = new URL("https://www.googleapis.com/fusiontables/v2/query?sql=" + query + "&key=" + API_KEY_GOOGLE);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + getToken());

        // read the response
        Log.i(TAG, "Response Code: " + conn.getResponseCode());
        InputStream in = new BufferedInputStream(conn.getInputStream());
        String response = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
        Log.v(TAG, response);

    }

    private String getToken() {
        AuthPreferences authPreferences = new AuthPreferences(this);
        return authPreferences.getToken();
    }

}
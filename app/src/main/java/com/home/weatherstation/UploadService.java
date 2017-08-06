package com.home.weatherstation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
    private static final String EXTRA_SAMPLE_DEVICE10 = "com.home.weatherstation.extra.sampledevice10";

    private static final String TEMPERATURE_TABLE_ID = "1jQ_Jnnw26pWU05sGBNdXbXlvxB-66_W4fuJgsTG7";
    private static final String HUMIDITY_TABLE_ID = "1sJHjpA2ToIvRbY0eksYhS1hfctq8yg-1H1KPhvaJ";
    private static final String BATTERY_TABLE_ID = "13Oox5ACRRPJcaL8CigkkpveWUNV3ALDbEwWpmuvq";
    private static final String API_KEY_GOOGLE = "AIzaSyC6bt0RnAVIDwdj3eiSJBmrEPqTmQGDNkM";

    private static final String API_KEY_WUNDERGROUND = "6ad6fa3bdb22276d"; // https://www.wunderground.com/weather/api/d/6ad6fa3bdb22276d/edit.html
    private static final String WUNDERGROUND_STATION_URL = "https://api.wunderground.com/api/" + API_KEY_WUNDERGROUND + "/conditions/q/ch/zuerich-kreis-4-hard/zmw:00000.71.06660.json";
    private static final String SMN_STATION_URL = "http://data.netcetera.com:80/smn/smn/REH"; // http://data.netcetera.com/smn/

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.0");

    public UploadService() {
        super("UploadService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startUpload(Context context, final Date timestamp, final Sample sampleDeviceNo8, final Sample sampleDeviceNo9, final Sample sampleDeviceNo10) {
        if (sampleDeviceNo8 == null && sampleDeviceNo9 == null && sampleDeviceNo10 == null) {
            Log.w(TAG, "Not starting upload because all samples are null");
            return;
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_UPLOAD);
        intent.putExtra(EXTRA_TIMESTAMP, timestamp.getTime());
        intent.putExtra(EXTRA_SAMPLE_DEVICE8, getSample("Device8", sampleDeviceNo8));
        intent.putExtra(EXTRA_SAMPLE_DEVICE9, getSample("Device9", sampleDeviceNo9));
        intent.putExtra(EXTRA_SAMPLE_DEVICE10, getSample("Device10", sampleDeviceNo10));
        context.startService(intent);
    }

    private static Sample getSample(final String name, final Sample sample) {
        if (sample == null) {
            return new Sample(new Date(), name, Sample.NOT_SET_FLOAT, Sample.NOT_SET_INT, Sample.NOT_SET_INT);
        } else {
            return sample;
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                final Date timestamp = new Date(intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis()));
                final Sample sampleDevice8 = intent.getParcelableExtra(EXTRA_SAMPLE_DEVICE8);
                final Sample sampleDevice9 = intent.getParcelableExtra(EXTRA_SAMPLE_DEVICE9);
                final Sample sampleDevice10 = intent.getParcelableExtra(EXTRA_SAMPLE_DEVICE10);
//                final Sample sampleOutside = fetchCurrentConditionsOutsideWunderGround();
                final Sample sampleOutside = fetchCurrentConditionsOutsideSMN();
                Log.i(TAG, "" + sampleOutside);
                upload(timestamp, sampleDevice8, sampleDevice9, sampleDevice10, sampleOutside);
            } else {
                Log.w(TAG, "Unknown action: " + action);
            }
        }
    }

    private void upload(Date timestamp, Sample deviceNo8, Sample deviceNo9, Sample deviceNo10, Sample sampleOutside) {
        int tries = 0;
        while (tries < 4) {
            tries++;
            try {
                insert(timestamp, deviceNo8, deviceNo9, deviceNo10, sampleOutside);
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

    private Sample fetchCurrentConditionsOutsideWunderGround() {
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

            return new Sample(d, "Outside", tempCurrent, relHumid, Sample.NOT_SET_INT);
        } catch (Exception e) {
            e.printStackTrace();
            return new Sample(new Date(), "Outside", Sample.NOT_SET_FLOAT, Sample.NOT_SET_INT, Sample.NOT_SET_INT);
        }

    }

    private static Sample fetchCurrentConditionsOutsideSMN() {
        try {
            URL url = new URL(SMN_STATION_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // read the response
            Log.i(TAG, "Response Code: " + conn.getResponseCode());
            InputStream in = new BufferedInputStream(conn.getInputStream());
            String response = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
            Log.v(TAG, response);

            JSONObject currentObservation = new JSONObject(response);
            Date d = parseDate(currentObservation.getString("dateTime"));
            float tempCurrent = Float.valueOf(currentObservation.getString("temperature"));
            int relHumid = Integer.valueOf(currentObservation.getString("humidity"));
            int pressure = currentObservation.getInt("qfePressure");

            return new Sample(d, "Outside", tempCurrent, relHumid, Sample.NOT_SET_INT);
        } catch (Exception e) {
            e.printStackTrace();
            return getSample("Outside", null);
        }

    }

    private static Date parseDate(String dateString) {
        DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return utcFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }

    /**
     * Make sure there is a valid token available. See @link{com.home.weatherstation.Authenticator}
     */
    private void insert(Date timestamp, Sample device8, Sample device9, Sample device10, Sample outside) throws IOException {
        CharSequence timestampValue = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp);
        insert(TEMPERATURE_TABLE_ID, timestampValue, device8.hasTempCurrent(), device8.getTemperature(), device9.hasTempCurrent(), device9.getTemperature(), device10.hasTempCurrent(), device10.getTemperature(), outside.hasTempCurrent(), outside.getTemperature());
        insert(HUMIDITY_TABLE_ID, timestampValue, device8.hasRelativeHumidity(), device8.getRelativeHumidity(), device9.hasRelativeHumidity(), device9.getRelativeHumidity(), device10.hasRelativeHumidity(), device10.getRelativeHumidity(), outside.hasRelativeHumidity(), outside.getRelativeHumidity());
        insert(BATTERY_TABLE_ID, timestampValue, device8.hasBatteryLevelCurrent(), device8.getBatteryLevel(), device9.hasBatteryLevelCurrent(), device9.getBatteryLevel(), device10.hasBatteryLevelCurrent(), device10.getBatteryLevel(), outside.hasBatteryLevelCurrent(), outside.getBatteryLevel());
    }

    private void insert(String table, CharSequence timestamp, boolean device8HasValue, float device8Value, boolean device9HasValue, float device9Value, boolean device10HasValue, float device10Value, boolean outsideHasValue, float outsideValue) throws IOException {
        insert(table, timestamp.toString(), device8HasValue, DECIMAL_FORMAT.format(device8Value), device9HasValue, DECIMAL_FORMAT.format(device9Value), device10HasValue, DECIMAL_FORMAT.format(device10Value), outsideHasValue, DECIMAL_FORMAT.format(outsideValue));
    }

    private void insert(String table, CharSequence timestamp, boolean device8HasValue, int device8Value, boolean device9HasValue, int device9Value, boolean device10HasValue, int device10Value, boolean outsideHasValue, int outsideValue) throws IOException {
        insert(table, timestamp.toString(), device8HasValue, String.valueOf(device8Value), device9HasValue, String.valueOf(device9Value), device10HasValue, String.valueOf(device10Value), outsideHasValue, String.valueOf(outsideValue));
    }

    private void insert(String table, CharSequence timestamp, boolean device8HasValue, String device8Value, boolean device9HasValue, String device9Value, boolean device10HasValue, String device10Value, boolean outsideHasValue, String outsideValue) throws IOException {
        // build insert statements
        String rawInsertStatement =
                "INSERT INTO %s (" +
                        "Date" +
                        (device8HasValue ? ",DeviceNo8" : "") +
                        (device9HasValue ? ",DeviceNo9" : "") +
                        (device10HasValue ? ",DeviceNo10" : "") +
                        (outsideHasValue ? ",Outside" : "")
                        + ") VALUES (" +
                        "'%s'" +
                        (device8HasValue ? ", " + device8Value : "") +
                        (device9HasValue ? ", " + device9Value : "") +
                        (device10HasValue ? ", " + device10Value : "") +
                        (outsideHasValue ? ", " + outsideValue : "")
                        + ")";
        String insertStatement = String.format(rawInsertStatement, table, timestamp);

        Log.v(TAG, "Insert statement : " + insertStatement);

        // Encode the query
        String query = URLEncoder.encode(insertStatement);
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
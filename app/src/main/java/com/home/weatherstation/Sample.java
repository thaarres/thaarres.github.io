package com.home.weatherstation;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by thaarres on 12/06/16.
 */
public class Sample implements Parcelable {
    private Date timestamp;
    private String deviceName;
    private float tempCurrent;
    private float tempLow;
    private float tempHigh;
    private int relativeHumidity;
    private int pressure;

    public Sample(final Date timestamp, String deviceName, final float tempCurrent, final float tempLow, final float tempHigh, final int relativeHumidity, final int pressure) {
        this.timestamp = timestamp;
        this.deviceName = deviceName;
        this.tempCurrent = tempCurrent;
        this.tempLow = tempLow;
        this.tempHigh = tempHigh;
        this.relativeHumidity = relativeHumidity;
        this.pressure = pressure;
    }

    public Sample(Parcel in) {
        readFromParcel(in);
    }


    public static final Creator<Sample> CREATOR = new Creator<Sample>() {
        @Override
        public Sample createFromParcel(Parcel in) {
            return new Sample(in);
        }

        @Override
        public Sample[] newArray(int size) {
            return new Sample[size];
        }
    };

    public Date getTimestamp() {
        return timestamp;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public float getTempCurrent() {
        return tempCurrent;
    }

    public float getTempLow() {
        return tempLow;
    }

    public float getTempHigh() {
        return tempHigh;
    }

    public int getRelativeHumidity() {
        return relativeHumidity;
    }

    public int getPressure() {
        return pressure;
    }

    @Override
    public String toString() {
        return "Sample{" +
                "timestamp=" + timestamp +
                ", deviceName='" + deviceName + '\'' +
                ", tempCurrent=" + tempCurrent +
                ", tempLow=" + tempLow +
                ", tempHigh=" + tempHigh +
                ", relativeHumidity=" + relativeHumidity +
                ", pressure=" + pressure +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timestamp.getTime());
        dest.writeString(deviceName);
        dest.writeFloat(tempCurrent);
        dest.writeFloat(tempLow);
        dest.writeFloat(tempHigh);
        dest.writeInt(relativeHumidity);
        dest.writeInt(pressure);
    }

    private void readFromParcel(Parcel in) {
        // We just need to read back each
        // field in the order that it was
        // written to the parcel
        timestamp = new Date(in.readLong());
        deviceName = in.readString();
        tempCurrent = in.readFloat();
        tempLow = in.readFloat();
        tempHigh = in.readFloat();
        relativeHumidity = in.readInt();
        pressure = in.readInt();
    }
}

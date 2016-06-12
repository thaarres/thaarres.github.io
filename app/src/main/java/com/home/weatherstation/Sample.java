package com.home.weatherstation;

import java.util.Date;

/**
 * Created by thaarres on 12/06/16.
 */
public class Sample {
    private final Date timestamp;
    private final String deviceName;
    private final float tempCurrent;
    private final float tempLow;
    private final float tempHigh;
    private final short relativeHumidity;
    private final short pressure;

    public Sample(final Date timestamp, String deviceName, final float tempCurrent, final float tempLow, final float tempHigh, final short relativeHumidity, final short pressure) {
        this.timestamp = timestamp;
        this.deviceName = deviceName;
        this.tempCurrent = tempCurrent;
        this.tempLow = tempLow;
        this.tempHigh = tempHigh;
        this.relativeHumidity = relativeHumidity;
        this.pressure = pressure;
    }

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

    public short getRelativeHumidity() {
        return relativeHumidity;
    }

    public short getPressure() {
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
}

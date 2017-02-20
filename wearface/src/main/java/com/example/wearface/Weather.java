package com.example.wearface;

/**
 * Created by allu on 2/20/17.
 */

public class Weather {
    private String highTemp;
    private String lowTemp;
    private long timestamp;
    private int drawableRes;
    private int weatherId;

    public String getHighTemp() {
        return highTemp;
    }

    public void setHighTemp(String highTemp) {
        this.highTemp = highTemp;
    }

    public String getLowTemp() {
        return lowTemp;
    }

    public void setLowTemp(String lowTemp) {
        this.lowTemp = lowTemp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDrawableRes() {
        return drawableRes;
    }

    public void setDrawableRes(int drawableRes) {
        this.drawableRes = drawableRes;
    }

    public int getWeatherId() {
        return weatherId;
    }

    public void setWeatherId(int weatherId) {
        this.weatherId = weatherId;
    }

    @Override
    public String toString() {
        return "highTemp: " + highTemp +
                ", lowTemp: " + lowTemp +
                ", timestamp: " + timestamp +
                ", drawableRes: " + drawableRes +
                ", weatherId: " + weatherId;
    }
}

package com.example.wearface;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import org.greenrobot.eventbus.EventBus;

public class WearService extends WearableListenerService {
    public static final String ARG_WEATHER_MESSAGES_PATH = "/messages";
    public static final String ARG_HIGH_TEMP = "HIGH_TEMP";
    public static final String ARG_LOW_TEMP = "LOW_TEMP";
    public static final String ARG_WEATHER_ID = "WEATHER_ID";
    public static final String ARG_WEATHER_DRAWABLE = "WEATHER_DRAWABLE";
    public static final String ARG_TIMESTAMP = "TIMESTAMP";
    private static final String TAG = "MyService";

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
        for (DataEvent dataEvent : dataEventBuffer) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (TextUtils.equals(path, ARG_WEATHER_MESSAGES_PATH)) {

                    String highTemp = dataMap.getString(ARG_HIGH_TEMP);
                    String lowTemp = dataMap.getString(ARG_LOW_TEMP);
                    int weatherId = dataMap.getInt(ARG_WEATHER_ID);
                    long timestamp = dataMap.getLong(ARG_TIMESTAMP);

                    Weather weatherData = new Weather();
                    weatherData.setHighTemp(highTemp);
                    weatherData.setLowTemp(lowTemp);
                    weatherData.setTimestamp(timestamp);
                    weatherData.setDrawableRes(getIconResourceForWeatherCondition(weatherId));
                    weatherData.setWeatherId(weatherId);

                    EventBus.getDefault().post(weatherData);

                    Log.e(TAG, "onDataChanged: message: weatherData: " + weatherData.toString());
                }
            }
        }
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     *
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    private int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.weather_lightning;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.weather_rainy;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.weather_rainy;
        } else if (weatherId == 511) {
            return R.drawable.weather_snowy;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.weather_rainy;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.weather_snowy;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.weather_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.weather_lightning_rainy;
        } else if (weatherId == 800) {
            return R.drawable.weather_sunny;
        } else if (weatherId == 801) {
            return R.drawable.weather_partlycloudy;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.weather_cloudy;
        }
        return -1;
    }
}

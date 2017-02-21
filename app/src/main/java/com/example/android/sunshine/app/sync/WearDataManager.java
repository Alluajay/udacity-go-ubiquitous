package com.example.android.sunshine.app.sync;

import android.support.annotation.NonNull;
import android.util.Log;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;

/**
 * Created by allu on 2/21/17.
 */

public class WearDataManager {
    public static final String ARG_WEATHER_MESSAGES_PATH = "/messages";
    public static final String ARG_HIGH_TEMP = "HIGH_TEMP";
    public static final String ARG_LOW_TEMP = "LOW_TEMP";
    public static final String ARG_WEATHER_ID = "WEATHER_ID";
    public static final String ARG_TIMESTAMP = "TIMESTAMP";
    private static final String TAG = "WearDataManager";

    private WearDataManager() {

    }

    public static void sendDataToWear(GoogleApiClient googleApiClient,
                                      String highTemp,
                                      String lowTemp,
                                      int weatherId) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(ARG_WEATHER_MESSAGES_PATH);

        putDataMapRequest.getDataMap().putString(ARG_HIGH_TEMP, highTemp);
        putDataMapRequest.getDataMap().putString(ARG_LOW_TEMP, lowTemp);
        putDataMapRequest.getDataMap().putInt(ARG_WEATHER_ID, weatherId);
        putDataMapRequest.getDataMap().putLong(ARG_TIMESTAMP, Calendar.getInstance().getTimeInMillis());

        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e(TAG, "onSuccess: status: failed, message: " + dataItemResult.getStatus().getStatusMessage());
                        } else {
                            Log.e(TAG, "onSuccess: status: success, message: " + dataItemResult.getStatus().getStatusMessage());
                        }
                    }
                });
    }
}

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wearface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    private static final String TAG = MyWatchFace.class.getSimpleName();

    private static final String FONT_PATH_RUBIK_BOLD = "fonts/Rubik-Bold.ttf";
    private static final String FONT_PATH_RUBIK_MEDIUM = "fonts/Rubik-Medium.ttf";
    private static final String FONT_PATH_RUBIK_LIGHT = "fonts/Rubik-Light.ttf";

    private static final String DATE_FORMAT = "MMM d";
    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    Bitmap mBitmapWeatherIcon;
    String mHighTemp, mLowTemp;

    private Typeface getTypeface(TYPEFACE typeface) {
        switch (typeface) {
            case RUBIK_BOLD:
                return Typeface.createFromAsset(getAssets(), FONT_PATH_RUBIK_BOLD);
            case RUBIK_MEDIUM:
                return Typeface.createFromAsset(getAssets(), FONT_PATH_RUBIK_MEDIUM);
            case RUBIK_LIGHT:
                return Typeface.createFromAsset(getAssets(), FONT_PATH_RUBIK_LIGHT);
        }
        return Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onDataChanged(Weather weatherData) {
        mBitmapWeatherIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), weatherData.getDrawableRes()),
                48, 48, true);
        mHighTemp = weatherData.getHighTemp() + "\u00B0";
        mLowTemp = weatherData.getLowTemp() + "\u00B0";
    }

    private enum TYPEFACE {
        RUBIK_BOLD,
        RUBIK_MEDIUM,
        RUBIK_LIGHT
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint Bg;
        Paint Hrs_Text;
        Paint Min_Text;
        Paint Sec_Text;
        Paint Date_Text;
        Paint Hightemp_Text;
        Paint mTextPaintLowTemp;
        Paint Icon;
        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver TimeZoneBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffsetHours1, mXOffsetHours2,
                mXOffsetMinutes1, mXOffsetMinutes2,
                mXOffsetSeconds, mXOffsetDate,
                mXOffsetWeatherIcon, mXOffsetHighTemp,
                mXOffsetLowTemp;
        float mYOffsetHours1, mYOffsetHours2,
                mYOffsetMinutes1, mYOffsetMinutes2,
                mYOffsetSeconds, mYOffsetDate,
                mYOffsetWeatherIcon, mYOffsetHighTemp,
                mYOffsetLowTemp;
        float mYGap, mXGap;
        Bitmap mBitmapBackground;
        Bitmap mBackgroundScaledBitmap;


        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffsetHours1 = resources.getDimension(R.dimen.digital_y_offset);
            mYGap = resources.getDimension(R.dimen.digital_y_gap);
            mXGap = resources.getDimension(R.dimen.digital_x_gap);

            LoadAllGraphics(resources);

            mCalendar = Calendar.getInstance();

            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg, null);
            mBitmapBackground = ((BitmapDrawable) backgroundDrawable).getBitmap();
        }


        void LoadAllGraphics(Resources resources){
            Bg = new Paint();
            Bg.setColor(resources.getColor(R.color.background));

            Hrs_Text = new Paint();
            Hrs_Text = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_BOLD);
            Hrs_Text.setTextSize(resources.getDimension(R.dimen.text_size_hours));

            Min_Text = new Paint();
            Min_Text = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_BOLD);
            Min_Text.setTextSize(resources.getDimension(R.dimen.text_size_minutes));

            Sec_Text = new Paint();
            Sec_Text = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_LIGHT);
            Sec_Text.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            Date_Text = new Paint();
            Date_Text = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_LIGHT);
            Date_Text.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            Hightemp_Text = new Paint();
            Hightemp_Text = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_MEDIUM);
            Hightemp_Text.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            mTextPaintLowTemp = new Paint();
            mTextPaintLowTemp = createTextPaint(resources.getColor(R.color.digital_text), TYPEFACE.RUBIK_LIGHT);
            mTextPaintLowTemp.setColor(resources.getColor(R.color.card_grey_text_color));
            mTextPaintLowTemp.setTextSize(resources.getDimension(R.dimen.text_size_seconds));

            Icon = new Paint();
            Icon.setAntiAlias(true);
            Icon.setFilterBitmap(true);
            Icon.setDither(true);
        }
        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor, TYPEFACE typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(getTypeface(typeface));
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(TimeZoneBroadcast, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(TimeZoneBroadcast);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffsetHours1 = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mXOffsetMinutes1 = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            Hrs_Text.setTextSize(textSize);

            Min_Text.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    Hrs_Text.setAntiAlias(!inAmbientMode);
                    Min_Text.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            updateTimer();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    break;
                case TAP_TYPE_TOUCH_CANCEL:

                    break;
                case TAP_TYPE_TAP:
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                //canvas.drawRect(0, 0, bounds.width(), bounds.height(), Bg);
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String hours = String.format(Locale.getDefault(), "%02d", mCalendar.get(Calendar.HOUR));
            String minutes = String.format(Locale.getDefault(), "%02d", mCalendar.get(Calendar.MINUTE));

            char hour1 = hours.charAt(0);
            char hour2 = hours.charAt(1);

            char minutes1 = minutes.charAt(0);
            char minutes2 = minutes.charAt(1);

            String seconds = null;
            String date = null;
            if (!mAmbient) {
                seconds = String.format(Locale.getDefault(), "%02d", mCalendar.get(Calendar.SECOND));
                date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(mCalendar.getTime());
            }

            // draw hour1 and hour2
            canvas.drawText(String.valueOf(hour1), mXOffsetHours1, mYOffsetHours1, Hrs_Text);

            Rect rectBoundsHour1 = new Rect();
            Hrs_Text.getTextBounds(String.valueOf(hour1), 0, 1, rectBoundsHour1);

            mXOffsetHours2 = mXOffsetHours1 + rectBoundsHour1.width() + mXGap;
            mYOffsetHours2 = mYOffsetHours1;

            canvas.drawText(String.valueOf(hour2), mXOffsetHours2, mYOffsetHours2, Hrs_Text);

            Rect rectBoundsHours = new Rect();
            Hrs_Text.getTextBounds(hours, 0, hours.length() - 1, rectBoundsHours);
            //Log.e(TAG, "onDraw: "+ rectBoundsHours.toString());

            mYOffsetMinutes1 = mYOffsetHours1 + rectBoundsHours.height() + mYGap;

            // draw minute1 and minute2
            canvas.drawText(String.valueOf(minutes1), mXOffsetMinutes1, mYOffsetMinutes1, Min_Text);

            Rect rectBoundsMinute1 = new Rect();
            Min_Text.getTextBounds(String.valueOf(minutes1), 0, 1, rectBoundsMinute1);

            mXOffsetMinutes2 = mXOffsetMinutes1 + rectBoundsHour1.width() + mXGap;
            mYOffsetMinutes2 = mYOffsetMinutes1;

            canvas.drawText(String.valueOf(minutes2), mXOffsetMinutes2, mYOffsetMinutes2, Min_Text);

            Rect rectBoundsMinutes = new Rect();
            Min_Text.getTextBounds(minutes, 0, minutes.length() - 1, rectBoundsMinutes);

            if (!TextUtils.isEmpty(seconds) && !TextUtils.isEmpty(date)) {
                Rect rectBoundsSeconds = new Rect();
                Sec_Text.getTextBounds(seconds, 0, seconds.length() - 1, rectBoundsSeconds);

                mXOffsetSeconds = mXOffsetHours1;
                mYOffsetSeconds = mYOffsetMinutes1 + rectBoundsSeconds.height() + mYGap;

                //mXOffsetSeconds+=rectBoundsMinutes.width()+rectBoundsSeconds.right;

                canvas.drawText(seconds, mXOffsetSeconds, mYOffsetSeconds, Sec_Text);

                // draw date
                Rect rectBoundsDate = new Rect();
                Date_Text.getTextBounds(date, 0, date.length() - 1, rectBoundsDate);

                mXOffsetDate = bounds.width() - rectBoundsDate.width() - mXOffsetSeconds * 2;
                mYOffsetDate = mYOffsetSeconds;

                canvas.drawText(date, mXOffsetDate, mYOffsetDate, Date_Text);

                // draw weather icon
                mXOffsetWeatherIcon = mXOffsetDate;
                mYOffsetWeatherIcon = mYOffsetHours1 - rectBoundsHours.height();

                if (mBitmapWeatherIcon != null) {
                    canvas.drawBitmap(mBitmapWeatherIcon, mXOffsetWeatherIcon, mYOffsetWeatherIcon, Icon);
                }

                if (!TextUtils.isEmpty(mHighTemp) && !TextUtils.isEmpty(mLowTemp)) {
                    // draw weather temp
                    mXOffsetHighTemp = mXOffsetDate;
                    mYOffsetHighTemp = mYOffsetMinutes1;

                    canvas.drawText(mHighTemp, mXOffsetHighTemp, mYOffsetHighTemp, Hightemp_Text);

                    Rect rectBoundsHighTemp = new Rect();
                    Hightemp_Text.getTextBounds(mHighTemp, 0, mHighTemp.length() - 1, rectBoundsHighTemp);

                    mXOffsetLowTemp = mXOffsetHighTemp + rectBoundsHighTemp.width() + mXGap;
                    mYOffsetLowTemp = mYOffsetHighTemp;

                    canvas.drawText(mLowTemp, mXOffsetLowTemp, mYOffsetLowTemp, mTextPaintLowTemp);
                }
            }
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }


        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }


        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBitmapBackground,
                        width, height, true /* filter */);
            }
        }
    }
}

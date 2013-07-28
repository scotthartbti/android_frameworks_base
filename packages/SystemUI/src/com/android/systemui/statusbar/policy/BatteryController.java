/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter.BluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.BatteryController";

    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();

    private static final int BATTERY_STYLE_NORMAL         = 0;
    private static final int BATTERY_STYLE_PERCENT        = 1;
    private static final int BATTERY_STYLE_ICON_PERCENT   = 2;
    /***
     * BATTERY_STYLE_CIRCLE* cannot be handled in this controller, since we cannot get views from
     * statusbar here. Yet it is listed for completion and not to confuse at future updates
     * See CircleBattery.java for more info
     *
     * set to public to be reused by CircleBattery
     */
    public static final int BATTERY_STYLE_CIRCLE                = 3;
    public static final int BATTERY_STYLE_CIRCLE_PERCENT        = 4;
    public static final int BATTERY_STYLE_DOTTED_CIRCLE         = 5;
    public static final int BATTERY_STYLE_DOTTED_CIRCLE_PERCENT = 6;
    public static final int BATTERY_STYLE_BEANSTALK      	= 7;
    public static final int BATTERY_STYLE_BEANSTALK_PERCENT      = 8;
    public static final int BATTERY_STYLE_GEAR          	 = 9;
    public static final int BATTERY_STYLE_SQUARE          	 = 10;
    public static final int BATTERY_STYLE_ALT			 = 11;
    public static final int BATTERY_STYLE_RACING          	 = 12;
    public static final int BATTERY_STYLE_GAUGE          	 = 13;
    public static final int BATTERY_STYLE_PLANET		= 14;
    public static final int BATTERY_STYLE_SLIDER          	 = 15;
    public static final int BATTERY_STYLE_BRICK			= 16;
    public static final int BATTERY_STYLE_RUSH			= 17;
    public static final int BATTERY_STYLE_GONE                  = 18;

    private static final int BATTERY_ICON_STYLE_NORMAL      = R.drawable.stat_sys_battery;
    private static final int BATTERY_ICON_STYLE_CHARGE      = R.drawable.stat_sys_battery_charge;
    private static final int BATTERY_ICON_STYLE_NORMAL_MIN  = R.drawable.stat_sys_battery_min;
    private static final int BATTERY_ICON_STYLE_CHARGE_MIN  = R.drawable.stat_sys_battery_charge_min;
    private static final int BATTERY_ICON_STYLE_NORMAL_BEANSTALK = R.drawable.stat_sys_battery_bs;
    private static final int BATTERY_ICON_STYLE_CHARGE_BEANSTALK = R.drawable.stat_sys_battery_bs_charge;
    private static final int BATTERY_ICON_STYLE_NORMAL_BEANSTALK_PERCENT = R.drawable.stat_sys_battery_bsp;
    private static final int BATTERY_ICON_STYLE_CHARGE_BEANSTALK_PERCENT = R.drawable.stat_sys_battery_bsp_charge;
    private static final int BATTERY_ICON_STYLE_NORMAL_GEAR = R.drawable.stat_sys_battery_gear;
    private static final int BATTERY_ICON_STYLE_CHARGE_GEAR = R.drawable.stat_sys_battery_gear_charge;
    private static final int BATTERY_ICON_STYLE_NORMAL_SQUARE = R.drawable.stat_sys_battery_square;
    private static final int BATTERY_ICON_STYLE_CHARGE_SQUARE = R.drawable.stat_sys_battery_charge_square;
    private static final int BATTERY_ICON_STYLE_NORMAL_ALT = R.drawable.stat_sys_battery_altcircle;
    private static final int BATTERY_ICON_STYLE_CHARGE_ALT = R.drawable.stat_sys_battery_charge_altcircle;

    private static final int BATTERY_ICON_STYLE_NORMAL_RACING = R.drawable.stat_sys_battery_racing;
    private static final int BATTERY_ICON_STYLE_CHARGE_RACING = R.drawable.stat_sys_battery_charge_racing;
    private static final int BATTERY_ICON_STYLE_NORMAL_GAUGE = R.drawable.stat_sys_battery_gauge;
    private static final int BATTERY_ICON_STYLE_CHARGE_GAUGE = R.drawable.stat_sys_battery_charge_gauge;
    private static final int BATTERY_ICON_STYLE_NORMAL_SLIDER = R.drawable.stat_sys_battery_slider;
    private static final int BATTERY_ICON_STYLE_CHARGE_SLIDER = R.drawable.stat_sys_battery_charge_slider;
    private static final int BATTERY_ICON_STYLE_NORMAL_BRICK = R.drawable.stat_sys_battery_brick;
    private static final int BATTERY_ICON_STYLE_CHARGE_BRICK = R.drawable.stat_sys_battery_charge_brick;
    private static final int BATTERY_ICON_STYLE_NORMAL_PLANET = R.drawable.stat_sys_battery_planet;
    private static final int BATTERY_ICON_STYLE_CHARGE_PLANET = R.drawable.stat_sys_battery_charge_planet;
    private static final int BATTERY_ICON_STYLE_NORMAL_RUSH = R.drawable.stat_sys_battery_rush;
    private static final int BATTERY_ICON_STYLE_CHARGE_RUSH = R.drawable.stat_sys_battery_charge_rush;

    private static final int BATTERY_TEXT_STYLE_NORMAL  = R.string.status_bar_settings_battery_meter_format;
    private static final int BATTERY_TEXT_STYLE_MIN     = R.string.status_bar_settings_battery_meter_min_format;

    private boolean mBatteryPlugged = false;
    private int mLevel = 0;
    private int mTextColor = -2;
    private int mTextChargingColor = -2;
    private int mBatteryStyle;
    private int mBatteryIcon = BATTERY_ICON_STYLE_NORMAL;

    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PIE_DISABLE_STATUSBAR_INFO),
                    false, this);
	    resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PIE_CONTROLS),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANDED_DESKTOP_STATE),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR),
                    false, this);
        }

        @Override public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private ArrayList<BatteryStateChangeCallback> mChangeCallbacks =
            new ArrayList<BatteryStateChangeCallback>();

    public interface BatteryStateChangeCallback {
        public void onBatteryLevelChanged(int level, boolean pluggedIn);
    }

    public BatteryController(Context context) {
        mContext = context;

        mHandler = new Handler();

        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        updateSettings();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public void removeStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }

    public int getBatteryLevel() {
        return mLevel;
    }

    public boolean isBatteryStatusCharging() {
        return mBatteryPlugged;
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {

            mLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

            final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);

            mBatteryPlugged = false;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING: 
                case BatteryManager.BATTERY_STATUS_FULL:
                    mBatteryPlugged = true;
                    break;
            }


            int N = mIconViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageLevel(mLevel);
                v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                        mLevel));
            }
            N = mLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mLabelViews.get(i);
                if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
                    v.setText(mContext.getString(BATTERY_TEXT_STYLE_NORMAL,
                            mLevel));
                } else {
                    v.setText(mContext.getString(BATTERY_TEXT_STYLE_MIN,
                            mLevel));
                }
            }

            for (BatteryStateChangeCallback cb : mChangeCallbacks) {
                cb.onBatteryLevelChanged(mLevel, mBatteryPlugged);
            }
            updateBattery();
        }
    }

    private void updateBattery() {
        int mIcon = View.GONE;
        int mText = View.GONE;
        int mIconStyle = BATTERY_ICON_STYLE_NORMAL;
        int mTextStyle = BATTERY_TEXT_STYLE_NORMAL;
        int pxTextPadding = 0;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        float logicalDensity = metrics.density;

        if (mBatteryStyle == BATTERY_STYLE_NORMAL) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE
                    : BATTERY_ICON_STYLE_NORMAL;
        } else if (mBatteryStyle == BATTERY_STYLE_ICON_PERCENT) {
            pxTextPadding = 0;
            mIcon = (View.VISIBLE);
            mText = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_MIN
                    : BATTERY_ICON_STYLE_NORMAL_MIN;
            mTextStyle = BATTERY_TEXT_STYLE_MIN;
        } else if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
            pxTextPadding = (int) (4 * logicalDensity + 0.5);
            mText = (View.VISIBLE);
            mTextStyle = BATTERY_TEXT_STYLE_NORMAL;
	} else if (mBatteryStyle == 7) {
             mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_BEANSTALK
                    : BATTERY_ICON_STYLE_NORMAL_BEANSTALK;
	} else if (mBatteryStyle == 8) {
             mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_BEANSTALK_PERCENT
                    : BATTERY_ICON_STYLE_NORMAL_BEANSTALK_PERCENT; 
	} else if (mBatteryStyle == 9) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_GEAR
                    : BATTERY_ICON_STYLE_NORMAL_GEAR; 
	} else if (mBatteryStyle == 10) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_SQUARE
                    : BATTERY_ICON_STYLE_NORMAL_SQUARE;
	} else if (mBatteryStyle == 11) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_ALT
                    : BATTERY_ICON_STYLE_NORMAL_ALT;
	} else if (mBatteryStyle == 12) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_RACING
                    : BATTERY_ICON_STYLE_NORMAL_RACING;
	} else if (mBatteryStyle == 13) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_GAUGE
                    : BATTERY_ICON_STYLE_NORMAL_GAUGE;
	} else if (mBatteryStyle == 14) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_PLANET
                    : BATTERY_ICON_STYLE_NORMAL_PLANET;
	} else if (mBatteryStyle == 15) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_SLIDER
                    : BATTERY_ICON_STYLE_NORMAL_SLIDER;
	} else if (mBatteryStyle == 16) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_BRICK
                    : BATTERY_ICON_STYLE_NORMAL_BRICK;
	} else if (mBatteryStyle == 17) {
            mIcon = (View.VISIBLE);
            mIconStyle = mBatteryPlugged ? BATTERY_ICON_STYLE_CHARGE_RUSH
                    : BATTERY_ICON_STYLE_NORMAL_RUSH;
        }

        int N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            v.setVisibility(mIcon);
            v.setImageResource(mIconStyle);
        }
        N = mLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mLabelViews.get(i);
            v.setVisibility(mText);
            v.setText(mContext.getString(mTextStyle,
                    mLevel));
            v.setPadding(v.getPaddingLeft(),v.getPaddingTop(),pxTextPadding,v.getPaddingBottom());

            // turn text red at 14% when not on charger - same level android battery warning appears
            // if no custom color is defined && over 14% use system color
            // if charging turn to green or to custom user color
            if (mLevel <= 14 && !mBatteryPlugged) {
                v.setTextColor(Color.RED);
            } else if (mTextColor == -2  && !mBatteryPlugged) {
                v.setTextColor(mContext.getResources().getColor(com.android.internal.R.color.holo_blue_light));
            } else if (mTextChargingColor != -2  && mBatteryPlugged) {
                v.setTextColor(mTextChargingColor);
            } else if (mBatteryPlugged) {
                v.setTextColor(Color.GREEN);
            } else {
                v.setTextColor(mTextColor);
            }
        }
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mBatteryStyle = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY, 0));

        boolean disableStatusBarInfo = Settings.System.getInt(resolver,
                Settings.System.PIE_DISABLE_STATUSBAR_INFO, 0) == 1;
        if (disableStatusBarInfo) {
            // call only the settings if statusbar info is really hidden
            int pieMode = Settings.System.getInt(resolver,
                    Settings.System.PIE_CONTROLS, 0);
            boolean expandedDesktopState = Settings.System.getInt(resolver,
                    Settings.System.EXPANDED_DESKTOP_STATE, 0) == 1;

            if (pieMode == 2
                || pieMode == 1 && expandedDesktopState) {
                mBatteryStyle = BATTERY_STYLE_GONE;
            }
        }

        mTextColor = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, -2);

        mTextChargingColor = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, -2);

        updateBattery();
    }
}

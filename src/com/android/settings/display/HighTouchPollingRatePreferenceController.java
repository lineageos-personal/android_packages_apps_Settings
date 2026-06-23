/*
 * Copyright (C) 2026 The Infinity-X Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import lineageos.hardware.LineageHardwareManager;
import lineageos.providers.LineageSettings;

public class HighTouchPollingRatePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "HighTouchPollingRate";

    private static final int MODE_120HZ = 0;
    private static final int MODE_240HZ = 3;

    private static final String TOUCH_REPORT_RATE_PROP = "sys.touch.report_rate";

    private final LineageHardwareManager mHardwareManager;

    public HighTouchPollingRatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mHardwareManager = LineageHardwareManager.getInstance(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mHardwareManager.isSupported(
                LineageHardwareManager.FEATURE_HIGH_TOUCH_POLLING_RATE)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final ListPreference listPreference = screen.findPreference(getPreferenceKey());
        listPreference.setPersistent(false);
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final int mode = normalizeMode(SystemProperties.getInt(TOUCH_REPORT_RATE_PROP,
                MODE_120HZ));
        listPreference.setValue(String.valueOf(mode));
        listPreference.setSummary(listPreference.getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int mode;
        try {
            mode = normalizeMode(Integer.parseInt((String) newValue));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid touch polling mode: " + newValue, e);
            return false;
        }

        applyMode(mode);

        final ListPreference listPreference = (ListPreference) preference;
        final int index = listPreference.findIndexOfValue((String) newValue);
        if (index >= 0) {
            listPreference.setSummary(listPreference.getEntries()[index]);
        }
        return false;
    }

    private void applyMode(int mode) {
        final boolean enabled = mode != MODE_120HZ;
        try {
            SystemProperties.set(TOUCH_REPORT_RATE_PROP, String.valueOf(mode));
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to store touch polling mode: " + mode, e);
        }
        LineageSettings.System.putInt(mContext.getContentResolver(), getPreferenceKey(),
                enabled ? 1 : 0);
        mHardwareManager.set(LineageHardwareManager.FEATURE_HIGH_TOUCH_POLLING_RATE, enabled);
    }

    private int normalizeMode(int mode) {
        return mode == 1 ? MODE_240HZ : mode;
    }
}

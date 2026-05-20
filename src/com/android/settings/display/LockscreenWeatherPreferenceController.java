/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** Preference for the lockscreen weather display. */
public class LockscreenWeatherPreferenceController extends TogglePreferenceController {

    public LockscreenWeatherPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(), Settings.Secure.LOCK_SCREEN_WEATHER_ENABLED, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_WEATHER_ENABLED,
                isChecked ? 1 : 0);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(R.string.lockscreen_weather_summary);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}

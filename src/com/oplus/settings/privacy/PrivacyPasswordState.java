package com.oplus.settings.privacy;

import android.content.Context;
import android.text.TextUtils;
import com.oplus.settings.privacy.sdk.PrivacyCryptoUtils;

final class PrivacyPasswordState {
    private static final int DEFAULT_QUALITY = PrivacyCryptoUtils.PASSWORD_QUALITY_UNSPECIFIED;
    static final int SIMPLE_NUMERIC_QUALITY = PrivacyCryptoUtils.PASSWORD_QUALITY_NUMERIC_FOUR;

    private PrivacyPasswordState() {
    }

    static int getPasswordQuality(Context context) {
        if (context == null) {
            return DEFAULT_QUALITY;
        }
        return PrivacyCryptoUtils.getPrivacyInfo().passwordQuality;
    }

    static void setPasswordQuality(Context context, int quality) {
        // Password quality is owned by cryptoeng/RPMB and written with the password itself.
    }

    static boolean isPasswordSet(Context context) {
        return getPasswordQuality(context) != DEFAULT_QUALITY;
    }

    static boolean setPassword(Context context, String password) {
        if (context == null || TextUtils.isEmpty(password)) {
            return false;
        }

        return PrivacyCryptoUtils.savePassword(SIMPLE_NUMERIC_QUALITY, password);
    }

    static boolean checkPassword(Context context, String password) {
        if (context == null || TextUtils.isEmpty(password)) {
            return false;
        }

        return PrivacyCryptoUtils.checkPassword(password);
    }

    static boolean clearPassword(Context context, String password) {
        return context != null && PrivacyCryptoUtils.clearPassword(password);
    }
}

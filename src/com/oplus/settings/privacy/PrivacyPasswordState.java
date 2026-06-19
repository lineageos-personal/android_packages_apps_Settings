package com.oplus.settings.privacy;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

final class PrivacyPasswordState {
    private static final String KEY_PRIVACY_PASSWORD_QUALITY = "privacy_password_quality";
    private static final String KEY_PRIVACY_PASSWORD_SALT = "privacy_password_salt";
    private static final String KEY_PRIVACY_PASSWORD_HASH = "privacy_password_hash";
    private static final int DEFAULT_QUALITY = 0;
    static final int SIMPLE_NUMERIC_QUALITY = 2;

    private PrivacyPasswordState() {
    }

    static int getPasswordQuality(Context context) {
        if (context == null) {
            return DEFAULT_QUALITY;
        }
        return Settings.Secure.getInt(
                context.getContentResolver(), KEY_PRIVACY_PASSWORD_QUALITY, DEFAULT_QUALITY);
    }

    static void setPasswordQuality(Context context, int quality) {
        if (context == null) {
            return;
        }
        Settings.Secure.putInt(context.getContentResolver(), KEY_PRIVACY_PASSWORD_QUALITY, quality);
    }

    static boolean isPasswordSet(Context context) {
        return getPasswordQuality(context) != DEFAULT_QUALITY
                && !TextUtils.isEmpty(getSecureString(context, KEY_PRIVACY_PASSWORD_HASH));
    }

    static boolean setPassword(Context context, String password) {
        if (context == null || TextUtils.isEmpty(password)) {
            return false;
        }

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String encodedSalt = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hash = hashPassword(encodedSalt, password);
        if (TextUtils.isEmpty(hash)) {
            return false;
        }

        Settings.Secure.putString(context.getContentResolver(), KEY_PRIVACY_PASSWORD_SALT, encodedSalt);
        Settings.Secure.putString(context.getContentResolver(), KEY_PRIVACY_PASSWORD_HASH, hash);
        setPasswordQuality(context, SIMPLE_NUMERIC_QUALITY);
        return true;
    }

    static boolean checkPassword(Context context, String password) {
        if (context == null || TextUtils.isEmpty(password)) {
            return false;
        }

        String salt = getSecureString(context, KEY_PRIVACY_PASSWORD_SALT);
        String expectedHash = getSecureString(context, KEY_PRIVACY_PASSWORD_HASH);
        if (TextUtils.isEmpty(salt) || TextUtils.isEmpty(expectedHash)) {
            return false;
        }
        return TextUtils.equals(expectedHash, hashPassword(salt, password));
    }

    static void clearPassword(Context context) {
        if (context == null) {
            return;
        }
        Settings.Secure.putString(context.getContentResolver(), KEY_PRIVACY_PASSWORD_SALT, null);
        Settings.Secure.putString(context.getContentResolver(), KEY_PRIVACY_PASSWORD_HASH, null);
        setPasswordQuality(context, DEFAULT_QUALITY);
    }

    private static String getSecureString(Context context, String key) {
        if (context == null) {
            return null;
        }
        return Settings.Secure.getString(context.getContentResolver(), key);
    }

    private static String hashPassword(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest.digest(), Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}

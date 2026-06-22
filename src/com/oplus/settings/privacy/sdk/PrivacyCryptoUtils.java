package com.oplus.settings.privacy.sdk;

import android.util.Log;
import com.oplus.hardware.cryptoeng.CryptoEngManager;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class PrivacyCryptoUtils {
    public static final int PASSWORD_QUALITY_UNSPECIFIED = 0;
    public static final int PASSWORD_QUALITY_NUMERIC_FOUR = 1;

    private static final String TAG = "PrivacyCryptoUtils";
    private static final int PRIVACY_PASSWORD_SYSTEM_TYPE = 1;
    private static final int VERIFY_USE_PASSWORD = 1;
    private static final int SECURE_QUESTION_VERIFY_TYPE = 2;
    private static final int SECURE_QUESTION_HASH_LEN = 32;
    private static final int PMS_LATEST_SECURE_RULES_TAG = 10000;
    private static final int REQUEST_INDEX = 10000;
    private static final int RESULT_OK = 0;
    private static final int RESULT_SUPPORT_NFC_ESE = 10031;
    private static final int CHALLENGE_LENGTH = 64;

    private PrivacyCryptoUtils() {
    }

    public static PrivacyInfo getPrivacyInfo() {
        MethodBuffer request = new MethodBuffer(MethodType.PMS_GET_INFO)
                .appendInt(MethodParamType.PMS_TYPE, PRIVACY_PASSWORD_SYSTEM_TYPE)
                .appendInt(MethodParamType.PMS_LATEST_SECURE_RULES, PMS_LATEST_SECURE_RULES_TAG);
        ResultSummary result = send(request);
        PrivacyInfo info = new PrivacyInfo();
        if (!isSuccess(result, MethodType.PMS_GET_INFO)) {
            return info;
        }

        info.passwordQuality = result.getInt(MethodParamType.PMS_PWD_TYPE, PASSWORD_QUALITY_UNSPECIFIED);
        info.handler = result.getBytes(MethodParamType.PMS_HANDLER);
        info.retryCountLeft = result.getInt(MethodParamType.PMS_PWD_RETRY_COUNT_LEFT, 0);
        info.lockTimeLeft = result.getInt(MethodParamType.PMS_PWD_LOCK_TIME_LEFT, 0);
        return info;
    }

    public static boolean savePassword(int passwordQuality, String password) {
        if (!isValidPasswordQuality(passwordQuality) || password == null || password.isEmpty()) {
            return false;
        }

        MethodBuffer request = new MethodBuffer(MethodType.PMS_ENROLL)
                .appendInt(MethodParamType.PMS_TYPE, PRIVACY_PASSWORD_SYSTEM_TYPE)
                .appendInt(MethodParamType.PMS_PWD_TYPE, passwordQuality)
                .appendBytes(MethodParamType.PMS_PWD_INFO, sha256(password))
                .appendBytes(MethodParamType.PMS_SECURE_INFO_BUFFER, buildSecureInfo(password));

        ResultSummary result = send(request);
        if (result != null && result.methodType == MethodType.PMS_ENROLL
                && result.resultCode == RESULT_SUPPORT_NFC_ESE) {
            Log.d(TAG, "savePassword accepted NFC-eSE fallback result");
            return true;
        }
        return isSuccess(result, MethodType.PMS_ENROLL)
                && result.getBytes(MethodParamType.PMS_HANDLER) != null;
    }

    public static boolean checkPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        PrivacyInfo info = getPrivacyInfo();
        if (!info.isSecure() || info.handler == null) {
            return false;
        }

        byte[] passwordHash = sha256(password);
        if (passwordHash == null) {
            return false;
        }
        byte[] verifySeed = new byte[passwordHash.length + info.handler.length];
        System.arraycopy(passwordHash, 0, verifySeed, 0, passwordHash.length);
        System.arraycopy(info.handler, 0, verifySeed, passwordHash.length, info.handler.length);

        MethodBuffer request = new MethodBuffer(MethodType.PMS_VERIFY)
                .appendInt(MethodParamType.PMS_TYPE, PRIVACY_PASSWORD_SYSTEM_TYPE)
                .appendInt(MethodParamType.PMS_VERIFY_TYPE, VERIFY_USE_PASSWORD)
                .appendBytes(MethodParamType.PMS_VERIFY_BUFFER, sha256(verifySeed));

        ResultSummary result = send(request);
        byte[] challenge = result != null ? result.getBytes(MethodParamType.PMS_CHALLENGE) : null;
        return isSuccess(result, MethodType.PMS_VERIFY)
                && challenge != null
                && challenge.length == CHALLENGE_LENGTH;
    }

    public static boolean clearPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        PrivacyInfo info = getPrivacyInfo();
        if (!info.isSecure()) {
            return true;
        }
        byte[] challenge = getPasswordChallenge(password, info);
        if (challenge == null || challenge.length != CHALLENGE_LENGTH) {
            return false;
        }

        MethodBuffer request = new MethodBuffer(MethodType.PMS_DELETE)
                .appendInt(MethodParamType.PMS_TYPE, PRIVACY_PASSWORD_SYSTEM_TYPE)
                .appendBytes(MethodParamType.PMS_CHALLENGE, challenge);
        return isSuccess(send(request), MethodType.PMS_DELETE);
    }

    private static byte[] getPasswordChallenge(String password, PrivacyInfo info) {
        byte[] passwordHash = sha256(password);
        if (passwordHash == null || info.handler == null) {
            return null;
        }
        byte[] verifySeed = new byte[passwordHash.length + info.handler.length];
        System.arraycopy(passwordHash, 0, verifySeed, 0, passwordHash.length);
        System.arraycopy(info.handler, 0, verifySeed, passwordHash.length, info.handler.length);
        MethodBuffer request = new MethodBuffer(MethodType.PMS_VERIFY)
                .appendInt(MethodParamType.PMS_TYPE, PRIVACY_PASSWORD_SYSTEM_TYPE)
                .appendInt(MethodParamType.PMS_VERIFY_TYPE, VERIFY_USE_PASSWORD)
                .appendBytes(MethodParamType.PMS_VERIFY_BUFFER, sha256(verifySeed));
        ResultSummary result = send(request);
        return isSuccess(result, MethodType.PMS_VERIFY) ? result.getBytes(MethodParamType.PMS_CHALLENGE) : null;
    }

    private static byte[] buildSecureInfo(String password) {
        byte[] answerHash = sha256(password);
        if (answerHash == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, intBytes(SECURE_QUESTION_VERIFY_TYPE));
        write(out, intBytes(SECURE_QUESTION_HASH_LEN));
        write(out, answerHash);
        return out.toByteArray();
    }

    private static ResultSummary send(MethodBuffer request) {
        byte[] buffer = request.build();
        if (buffer == null) {
            return null;
        }
        try {
            return ResultSummary.parse(CryptoEngManager.getInstance().cryptoEngCommand(buffer));
        } catch (Exception e) {
            Log.e(TAG, "cryptoeng command failed", e);
            return null;
        }
    }

    private static boolean isSuccess(ResultSummary result, MethodType expectedType) {
        return result != null && result.methodType == expectedType && result.success;
    }

    private static boolean isValidPasswordQuality(int quality) {
        return quality >= 1 && quality <= 5;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static byte[] intBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

    private static int intValue(byte[] value) {
        if (value == null || value.length != 4) {
            return 0;
        }
        return ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private static void write(ByteArrayOutputStream out, byte[] value) {
        if (value != null && value.length > 0) {
            out.write(value, 0, value.length);
        }
    }

    public static final class PrivacyInfo {
        public byte[] handler;
        public int lockTimeLeft;
        public int passwordQuality;
        public int retryCountLeft;

        public boolean isSecure() {
            return passwordQuality != PASSWORD_QUALITY_UNSPECIFIED;
        }
    }

    private static final class MethodBuffer {
        private final MethodType mMethodType;
        private final ByteArrayOutputStream mParams = new ByteArrayOutputStream();
        private int mParamCount;

        MethodBuffer(MethodType methodType) {
            mMethodType = methodType;
        }

        MethodBuffer appendInt(MethodParamType type, int value) {
            return appendBytes(type, intBytes(value));
        }

        MethodBuffer appendBytes(MethodParamType type, byte[] value) {
            if (type == null || value == null || value.length == 0) {
                return this;
            }
            write(mParams, intBytes(type.code()));
            write(mParams, intBytes(value.length));
            write(mParams, value);
            mParamCount++;
            return this;
        }

        byte[] build() {
            if (mMethodType == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(out, intBytes(mMethodType.code()));
            write(out, intBytes(REQUEST_INDEX));
            write(out, intBytes(mParamCount));
            write(out, mParams.toByteArray());
            return out.toByteArray();
        }
    }

    private static final class ResultSummary {
        final MethodType methodType;
        final int resultCode;
        final boolean success;
        final Map<MethodParamType, byte[]> params;

        private ResultSummary(MethodType methodType, int resultCode, boolean success,
                Map<MethodParamType, byte[]> params) {
            this.methodType = methodType;
            this.resultCode = resultCode;
            this.success = success;
            this.params = params;
        }

        static ResultSummary parse(byte[] buffer) {
            if (buffer == null || buffer.length < 12) {
                return null;
            }

            int methodCode = intValue(Arrays.copyOfRange(buffer, 0, 4));
            MethodType methodType = MethodType.fromCode(methodCode);
            if (methodType == null) {
                return null;
            }

            int resultCode = intValue(Arrays.copyOfRange(buffer, 4, 8));
            int paramCount = intValue(Arrays.copyOfRange(buffer, 8, 12));
            Map<MethodParamType, byte[]> params = new HashMap<>();
            int offset = 12;
            for (int i = 0; i < paramCount; i++) {
                if (offset + 8 > buffer.length) {
                    return null;
                }
                MethodParamType paramType = MethodParamType.fromCode(
                        intValue(Arrays.copyOfRange(buffer, offset, offset + 4)));
                int paramLength = intValue(Arrays.copyOfRange(buffer, offset + 4, offset + 8));
                offset += 8;
                if (paramLength < 0 || offset + paramLength > buffer.length) {
                    return null;
                }
                if (paramType != null && paramLength > 0) {
                    params.put(paramType, Arrays.copyOfRange(buffer, offset, offset + paramLength));
                }
                offset += paramLength;
            }
            return new ResultSummary(methodType, resultCode, resultCode == RESULT_OK, params);
        }

        byte[] getBytes(MethodParamType type) {
            return params.get(type);
        }

        int getInt(MethodParamType type, int defaultValue) {
            byte[] value = getBytes(type);
            return value != null ? intValue(value) : defaultValue;
        }
    }
}

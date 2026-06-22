package com.oplus.settings.privacy.sdk;

enum MethodParamType {
    PMS_TYPE(201),
    PMS_PWD_INFO(202),
    PMS_SECURE_INFO_BUFFER(203),
    PMS_VERIFY_TYPE(204),
    PMS_VERIFY_BUFFER(205),
    PMS_CHALLENGE(206),
    PMS_HANDLER(209),
    PMS_PWD_TYPE(210),
    PMS_PWD_RETRY_COUNT_LEFT(211),
    PMS_PWD_LOCK_TIME_LEFT(212),
    PMS_HIDE_EMAIL(217),
    PMS_LATEST_SECURE_RULES(221);

    private final int mCode;

    MethodParamType(int code) {
        mCode = code;
    }

    static MethodParamType fromCode(int code) {
        for (MethodParamType type : values()) {
            if (type.mCode == code) {
                return type;
            }
        }
        return null;
    }

    int code() {
        return mCode;
    }
}

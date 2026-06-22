package com.oplus.settings.privacy.sdk;

enum MethodType {
    PMS_ENROLL(4001),
    PMS_VERIFY(4002),
    PMS_DELETE(4004),
    PMS_GET_INFO(4005);

    private final int mCode;

    MethodType(int code) {
        mCode = code;
    }

    static MethodType fromCode(int code) {
        for (MethodType type : values()) {
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

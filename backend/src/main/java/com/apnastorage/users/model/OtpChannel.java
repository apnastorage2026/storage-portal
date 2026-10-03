package com.apnastorage.users.model;

/**
 * How a one-time code was sent. The database stores the lowercase word, not the enum name.
 */
public enum OtpChannel {
    EMAIL("email"),
    MOBILE("mobile");

    private final String dbValue;

    OtpChannel(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static OtpChannel fromDbValue(String dbValue) {
        for (OtpChannel channel : values()) {
            if (channel.dbValue.equals(dbValue)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown OTP channel: " + dbValue);
    }
}

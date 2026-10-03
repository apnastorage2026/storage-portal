package com.apnastorage.users.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OtpChannelConverter implements AttributeConverter<OtpChannel, String> {

    @Override
    public String convertToDatabaseColumn(OtpChannel channel) {
        return channel == null ? null : channel.dbValue();
    }

    @Override
    public OtpChannel convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : OtpChannel.fromDbValue(dbValue);
    }
}

package com.frekanstan.asset_management.data.helpers;

import java.math.BigDecimal;

import io.objectbox.converter.PropertyConverter;

public class BigDecimalConverter implements PropertyConverter<BigDecimal, String> {

    @Override
    public BigDecimal convertToEntityProperty(String databaseValue) {
        return new BigDecimal(databaseValue);
    }

    @Override
    public String convertToDatabaseValue(BigDecimal entityProperty) {
        return entityProperty.toString();
    }
}

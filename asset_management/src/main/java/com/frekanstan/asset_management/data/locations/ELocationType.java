package com.frekanstan.asset_management.data.locations;

import io.objectbox.converter.PropertyConverter;

public enum ELocationType {
    NotSet(0),

    Unit(1),

    Warehouse(2),

    MainUnit(3),

    Service(4);

    public final int id;

    ELocationType(int id) {
        this.id = id;
    }

    public static ELocationType findByAbbr(int id) {
        for (ELocationType c : values()) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    public static class LocationTypeTypeConverter implements PropertyConverter<ELocationType, Integer> {
        @Override
        public ELocationType convertToEntityProperty(Integer databaseValue) {
            if (databaseValue == null) {
                return null;
            }
            for (ELocationType role : ELocationType.values()) {
                if (role.id == databaseValue) {
                    return role;
                }
            }
            return ELocationType.NotSet;
        }

        @Override
        public Integer convertToDatabaseValue(ELocationType entityProperty) {
            return entityProperty == null ? null : entityProperty.id;
        }
    }
}
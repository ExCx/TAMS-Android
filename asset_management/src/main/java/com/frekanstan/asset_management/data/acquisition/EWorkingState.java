package com.frekanstan.asset_management.data.acquisition;

import io.objectbox.converter.PropertyConverter;

public enum EWorkingState {
    Unknown(0),

    Active(1),

    Passive(2),

    Broken(3);

    public final int id;

    EWorkingState(int id) {
        this.id = id;
    }

    public static EWorkingState findByAbbr(int id) {
        for (EWorkingState c : values()) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    public static class WorkingStateTypeConverter implements PropertyConverter<EWorkingState, Integer> {
        @Override
        public EWorkingState convertToEntityProperty(Integer databaseValue) {
            if (databaseValue == null) {
                return null;
            }
            for (EWorkingState role : EWorkingState.values()) {
                if (role.id == databaseValue) {
                    return role;
                }
            }
            return EWorkingState.Unknown;
        }

        @Override
        public Integer convertToDatabaseValue(EWorkingState entityProperty) {
            return entityProperty == null ? null : entityProperty.id;
        }
    }
}
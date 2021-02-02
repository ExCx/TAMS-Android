package com.frekanstan.asset_management.data.people;

import io.objectbox.converter.PropertyConverter;

public enum EPersonType {
    NotSet(0),

    Employee(1),

    Recorder(2),

    UnitManager(3),

    TechnicalStaff(4),

    Purchaser(5),

    GeneralManager(6),

    Overseer(7);

    public final int id;

    EPersonType(int id) {
        this.id = id;
    }

    public static EPersonType findByAbbr(int id) {
        for (EPersonType c : values()) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    public static class PersonTypeTypeConverter implements PropertyConverter<EPersonType, Integer> {
        @Override
        public EPersonType convertToEntityProperty(Integer databaseValue) {
            if (databaseValue == null) {
                return null;
            }
            for (EPersonType role : EPersonType.values()) {
                if (role.id == databaseValue) {
                    return role;
                }
            }
            return EPersonType.NotSet;
        }

        @Override
        public Integer convertToDatabaseValue(EPersonType entityProperty) {
            return entityProperty == null ? null : entityProperty.id;
        }
    }
}

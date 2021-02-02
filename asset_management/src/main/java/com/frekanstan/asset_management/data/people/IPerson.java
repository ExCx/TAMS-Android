package com.frekanstan.asset_management.data.people;

import android.annotation.SuppressLint;

import com.frekanstan.asset_management.data.IEntity;

import java.util.Date;

public interface IPerson extends IEntity {
    String getNameSurname();

    EPersonType getPersonType();

    String getEmail();

    Date getLabelingDateTime();

    long getUserBoundId();

    String getIdentityNo();

    long getTenantId();

    boolean getHasPhoto();

    @SuppressLint("DefaultLocale")
    String getPersonCode();

    void setAsLabeled(boolean labeled);

    void setAsToBeLabeled();
}

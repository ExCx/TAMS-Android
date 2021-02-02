package com.frekanstan.gateway_mobil.app.people;

import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.data.Person;

import lombok.Getter;
import lombok.Setter;

public class PersonRepository
{
    @Getter @Setter
    private static Person currentPerson;

    @Setter
    private static long[] assignedPersonIds;
    public static long[] getAssignedPersonIds() {
        if (assignedPersonIds == null)
            assignedPersonIds = AssetDAO.getDao().getAllAssignedPersonIds();
        return assignedPersonIds;
    }
}
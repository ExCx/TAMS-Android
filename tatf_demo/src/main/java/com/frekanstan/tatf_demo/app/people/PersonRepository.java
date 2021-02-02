package com.frekanstan.tatf_demo.app.people;

import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.data.Person;

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
package com.frekanstan.gateway_mobil.app.people;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.gateway_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.gateway_mobil.data.LabeledStateChange;
import com.frekanstan.gateway_mobil.data.LabeledStateChange_;
import com.frekanstan.gateway_mobil.data.Person;
import com.frekanstan.gateway_mobil.data.Person_;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Date;

import io.objectbox.Box;
import io.objectbox.query.Query;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.StringExtensions.latinize;

public class PersonDAO extends DAO<Person>
{
    private static PersonDAO instance;

    private PersonDAO() { }

    public static PersonDAO getDao() {
        if (instance == null)
            instance = new PersonDAO();
        return instance;
    }

    @Override
    public Box<Person> getBox() {
        return ObjectBox.get().boxFor(Person.class);
    }

    public Person get(String email) {
        return getBox().query().equal(Person_.email, email).build().findFirst();
    }

    @Override
    public ArrayList<Person> getAll() {
        return new ArrayList<>(getBox().query().equal(Person_.tenantId, TenantRepository.getCurrentTenant().getId())
                .order(Person_.nameSurname).build().find());
    }

    protected LabeledStateChange getLabeledStateChange(long id) {
        return ObjectBox.get().boxFor(LabeledStateChange.class).query().equal(LabeledStateChange_.entityId, id).build().findFirst();
    }

    public void setLabeledStateChange(long id, Date labelingTime) {
        var lsc = getLabeledStateChange(id);
        if (lsc == null)
            lsc = new LabeledStateChange("person", id, labelingTime);
        else
            lsc.setLabelingDateTime(labelingTime);
        ObjectBox.get().boxFor(LabeledStateChange.class).put(lsc);
    }

    @Override
    public Query<Person> createFilteredQuery(Bundle input) {
        val q = input.getString("query");
        val personType = EPersonType.values()[input.getInt("personType")];
        val hasAssets = input.getByte("hasAssets");
        val sortBy = input.getString("sortBy");
        val listType = input.getString("listType");

        var builder = getBox().query().equal(Person_.tenantId, TenantRepository.getCurrentTenant().getId()); //tenant filter

        if (!Strings.isNullOrEmpty(listType)) {
            switch (listType) {
                case "labeled":
                    builder.notNull(Person_.labelingDateTime);
                    break;
                case "notlabeled":
                    builder.isNull(Person_.labelingDateTime);
                    break;
            }
        }

        if (personType != EPersonType.NotSet) //type filter
            builder.equal(Person_.personType, personType.id);

        if (!Strings.isNullOrEmpty(q)) { //word filter
            val qu = latinize(q);
            builder.startsWith(Person_.nameSurname, q).or().contains(Person_.nameSurname, " " + q).or()
                    .startsWith(Person_.nameSurname, qu).or().contains(Person_.nameSurname, " " + qu).or()
                    .startsWith(Person_.identityNo, q);
        }

        if (hasAssets == 1) //assignment filter
            builder.in(Person_.id, PersonRepository.getAssignedPersonIds());
        else if (hasAssets == -1)
            builder.notIn(Person_.id, PersonRepository.getAssignedPersonIds());

        if (sortBy != null) { //sort
            switch (sortBy) {
                case "nameSurname":
                    builder.order(Person_.nameSurname);
                    break;
            }
        }
        return builder.build();
    }
}
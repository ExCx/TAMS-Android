package com.frekanstan.kbs_mobil.app.locations;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.kbs_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.kbs_mobil.data.LabeledStateChange;
import com.frekanstan.kbs_mobil.data.LabeledStateChange_;
import com.frekanstan.kbs_mobil.data.Location;
import com.frekanstan.kbs_mobil.data.Location_;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Date;

import io.objectbox.Box;
import io.objectbox.query.Query;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.StringExtensions.latinize;

public class LocationDAO extends DAO<Location>
{
    private static LocationDAO instance;

    private LocationDAO() { }

    public static LocationDAO getDao() {
        if (instance == null)
            instance = new LocationDAO();
        return instance;
    }

    @Override
    public Box<Location> getBox() {
        return ObjectBox.get().boxFor(Location.class);
    }

    @Override
    public ArrayList<Location> getAll() {
        return new ArrayList<>(getBox().query().equal(Location_.tenantId, TenantRepository.getCurrentTenant().getId())
                .order(Location_.name).build().find());
    }

    protected LabeledStateChange getLabeledStateChange(long id) {
        return ObjectBox.get().boxFor(LabeledStateChange.class).query().equal(LabeledStateChange_.entityId, id).build().findFirst();
    }

    public void setLabeledStateChange(long id, Date labelingTime) {
        var lsc = getLabeledStateChange(id);
        if (lsc == null)
            lsc = new LabeledStateChange("location", id, labelingTime);
        else
            lsc.setLabelingDateTime(labelingTime);
        ObjectBox.get().boxFor(LabeledStateChange.class).put(lsc);
    }

    @Override
    public Query<Location> createFilteredQuery(Bundle input) {
        val q = input.getString("query");
        val locationType = ELocationType.values()[input.getInt("locationType")];
        val hasAssets = input.getByte("hasAssets");
        val sortBy = input.getString("sortBy");
        val listType = input.getString("listType");

        var builder = getBox().query().equal(Location_.tenantId, TenantRepository.getCurrentTenant().getId()); //tenant filter

        if (!Strings.isNullOrEmpty(listType)) {
            switch (listType) {
                case "labeled":
                    builder.notNull(Location_.labelingDateTime);
                    break;
                case "notlabeled":
                    builder.isNull(Location_.labelingDateTime);
                    break;
            }
        }

        if (locationType != ELocationType.NotSet) //type filter
            builder.equal(Location_.locationType, locationType.id);

        if (hasAssets == 1)  //assignment filter
            builder.in(Location_.id, LocationRepository.getAssignedLocationIds());
        else if (hasAssets == -1)
            builder.notIn(Location_.id, LocationRepository.getAssignedLocationIds());

        if (!Strings.isNullOrEmpty(q)) { //word filter
            val qu = latinize(q);
            builder.startsWith(Location_.name, q).or().contains(Location_.name, " " + q).or()
                    .startsWith(Location_.name, qu).or().contains(Location_.name, " " + qu);
        }

        if (sortBy != null) { //sort
            switch (sortBy) {
                case "name":
                    builder.order(Location_.name);
                    break;
            }
        }
        return builder.build();
    }
}
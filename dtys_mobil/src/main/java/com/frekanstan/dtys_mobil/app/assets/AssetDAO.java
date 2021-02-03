package com.frekanstan.dtys_mobil.app.assets;

import android.os.Bundle;

import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.data.assets.AssetDAOBase;
import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.dtys_mobil.app.acquisition.BudgetDAO;
import com.frekanstan.dtys_mobil.app.assettypes.AssetTypeDAO;
import com.frekanstan.dtys_mobil.app.locations.LocationDAO;
import com.frekanstan.dtys_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.dtys_mobil.app.settings.SettingDAO;
import com.frekanstan.dtys_mobil.app.sync.AssignmentChangeDAO;
import com.frekanstan.dtys_mobil.app.sync.CountedStateChangeDAO;
import com.frekanstan.dtys_mobil.app.sync.LabeledStateChangeDAO;
import com.frekanstan.dtys_mobil.app.tracking.CountingOpDAO;
import com.frekanstan.dtys_mobil.data.Asset;
import com.frekanstan.dtys_mobil.data.Asset_;
import com.frekanstan.dtys_mobil.data.AssignmentChange;
import com.frekanstan.dtys_mobil.data.AssignmentChange_;
import com.frekanstan.dtys_mobil.data.CountedStateChange;
import com.frekanstan.dtys_mobil.data.CountedStateChange_;
import com.frekanstan.dtys_mobil.data.LabeledStateChange;
import com.frekanstan.dtys_mobil.data.LabeledStateChange_;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.util.ArrayList;
import java.util.Date;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.Helpers.areAllTrue;
import static com.frekanstan.asset_management.app.helpers.StringExtensions.latinize;

public class AssetDAO extends AssetDAOBase<Asset> {
    private static AssetDAO instance;

    private AssetDAO() { }

    public static AssetDAO getDao() {
        if (instance == null)
            instance = new AssetDAO();
        return instance;
    }

    @Override
    public Box<Asset> getBox() {
        return ObjectBox.get().boxFor(Asset.class);
    }

    @Override
    protected QueryBuilder<Asset> buildRootQuery() {
        val storagesToFilterSetting = SettingDAO.getDao().getByName("Filters.StoragesOfUser"); //auto storage filter
        if (storagesToFilterSetting != null && !Strings.isNullOrEmpty(storagesToFilterSetting.getValue()))
        {
            var ids = new ArrayList<Long>();
            for(val id : storagesToFilterSetting.getValue().split("\\|"))
                if (!Strings.isNullOrEmpty(id))
                    ids.add(Long.parseLong(id));
            return getBox().query().equal(Asset_.tenantId, TenantRepository.getCurrentTenant().getId())
                    .in(Asset_.storageBelongsToId, Longs.toArray(ids));
        }
        else
            return getBox().query().equal(Asset_.tenantId, TenantRepository.getCurrentTenant().getId());
    }

    public Asset getByRemoteId(String remoteId) {
        return getBox().query().equal(Asset_.remoteId, remoteId).build().findFirst();
    }

    @Override
    public long[] getAllAssignedPersonIds() {
        var personIds = new ArrayList<Long>();
        for (val item : buildRootQuery().notEqual(Asset_.assignedPersonId, 0).build().find())
        {
            val personId = item.getAssignedPersonId();
            if (!personIds.contains(personId))
                personIds.add(personId);
        }
        return Longs.toArray(personIds);
    }

    @Override
    public long[] getAllAssignedLocationIds() {
        var locationIds = new ArrayList<Long>();
        for (val item : buildRootQuery().notEqual(Asset_.assignedLocationId, 0).build().find())
        {
            val locationId = item.getAssignedLocationId();
            if (!locationIds.contains(locationId))
                locationIds.add(locationId);
        }
        return Longs.toArray(locationIds);
    }

    @Override
    public long countByAssetCode(String assetCode, boolean onlyCounted, boolean onlyLabeled) {
        val builder = buildRootQuery();
        if (onlyCounted)
            return builder.startsWith(Asset_.registrationCode, assetCode + "-")
                    .notNull(Asset_.lastControlTime)
                    .build().count();
        else if (onlyLabeled)
            return builder.startsWith(Asset_.registrationCode, assetCode + "-")
                    .notNull(Asset_.labelingDateTime)
                    .build().count();
        else
            return builder.startsWith(Asset_.registrationCode, assetCode + "-")
                    .build().count();
    }

    @Override
    protected CountedStateChange getCountedStateChange(long assetId, long countingOpId) {
        return ObjectBox.get().boxFor(CountedStateChange.class).query().equal(CountedStateChange_.assetId, assetId).and().equal(CountedStateChange_.countingOpId, countingOpId).build().findFirst();
    }

    @Override
    public void setCountedStateChange(long assetId, Date lastControlTime, boolean isCounted, long countingOpId) {
        var csc = getCountedStateChange(assetId, countingOpId);
        if (csc == null) {
            val globalId = CountingOpDAO.getDao().get(countingOpId).getGlobalId();
            csc = new CountedStateChange(assetId, isCounted ? lastControlTime : null, countingOpId, globalId);
        }
        else
            csc.setLastControlTime(isCounted ? lastControlTime : null);
        CountedStateChangeDAO.getDao().put(csc);
    }

    @Override
    protected LabeledStateChange getLabeledStateChange(long assetId) {
        return LabeledStateChangeDAO.getDao().getBox().query().equal(LabeledStateChange_.entityId, assetId).build().findFirst();
    }

    @Override
    public void setLabeledStateChange(long assetId, boolean isLabeled) {
        val asset = get(assetId);
        asset.setAsLabeled(isLabeled);
        put(asset);
        var lsc = getLabeledStateChange(assetId);
        if (lsc == null)
            lsc = new LabeledStateChange("asset", assetId, asset.getLabelingDateTime());
        else
            lsc.setLabelingDateTime(asset.getLabelingDateTime());
        LabeledStateChangeDAO.getDao().put(lsc);
    }

    @Override
    public AssignmentChange getAssignmentChange(long assetId) {
        return ObjectBox.get().boxFor(AssignmentChange.class).query().equal(AssignmentChange_.assetId, assetId).build().findFirst();
    }

    @Override
    public void setAssignmentChange(long assetId, long personId, long locationId, boolean isRequest) {
        var ac = getAssignmentChange(assetId);
        if (ac == null)
            ac = new AssignmentChange(assetId, personId, locationId, isRequest);
        else {
            ac.setPersonId(personId);
            ac.setLocationId(locationId);
        }
        AssignmentChangeDAO.getDao().put(ac);
    }

    @Override
    public Query<Asset> createFilteredQuery(Bundle input) {
        val q = input.getString("query");
        val filterTagNoSelected = input.getBooleanArray("filterTagNoSelected");
        val filterLabelSelected = input.getBooleanArray("filterLabelSelected");
        val filterAssignmentSelected = input.getBooleanArray("filterAssignmentSelected");
        val filterDeploymentSelected = input.getBooleanArray("filterDeploymentSelected");
        val filterCountingSelected = input.getBooleanArray("filterCountingSelected");
        val filterBudgetSelected = input.getBooleanArray("filterBudgetSelected");
        val filterWSSelected = input.getBooleanArray("filterWSSelected");
        val filterStoragesSelected = input.getBooleanArray("filterStoragesSelected");
        val filterPrice = input.getDouble("filterPrice");
        val filterPriceType = input.getInt("filterPriceType");
        val assetTypeId = input.getLong("assetTypeId");
        val personId = input.getLong("personId");
        val locationId = input.getLong("locationId");
        val storageId = input.getLong("storageId");
        val sortBy = input.getString("sortBy");
        val isUpdated = input.getBoolean("isUpdated", false);
        val listType = input.getString("listType");

        //PREFILTER
        QueryBuilder<Asset> builder = buildRootQuery();

        if (assetTypeId != 0) //type filter
            builder.startsWith(Asset_.registrationCode, AssetTypeDAO.getDao().get(assetTypeId).getAssetCode() + "-");

        if (personId != 0) { //person filter
            if (!Strings.isNullOrEmpty(listType) && listType.equals("foreign"))
                builder.equal(Asset_.tempCounted, true).and().notEqual(Asset_.assignedPersonId, personId);
            else
                builder.equal(Asset_.assignedPersonId, personId);
        }

        if (locationId != 0) { //location filter
            if (!Strings.isNullOrEmpty(listType) && listType.equals("foreign"))
                builder.equal(Asset_.tempCounted, true).and().notEqual(Asset_.assignedLocationId, locationId);
            else
                builder.equal(Asset_.assignedLocationId, locationId);
        }

        if (storageId != 0) { //storage filter
            builder.equal(Asset_.assignedPersonId, 0).and().equal(Asset_.storageBelongsToId, storageId);
        }

        if (!Strings.isNullOrEmpty(listType)) {
            switch (listType) {
                case "counted":
                    builder.equal(Asset_.tempCounted, true);
                    break;
                case "notcounted":
                    builder.equal(Asset_.tempCounted, false);
                    break;
                case "labeled":
                    builder.notNull(Asset_.labelingDateTime);
                    break;
                case "notlabeled":
                    builder.isNull(Asset_.labelingDateTime);
                    break;
            }
        }

        //POSTFILTER
        if (filterLabelSelected != null && !areAllTrue(filterLabelSelected)) { //label filter
            if (filterLabelSelected[0])
                builder.isNull(Asset_.labelingDateTime);
            if (filterLabelSelected[1])
                builder.notNull(Asset_.labelingDateTime);
        }

        if (filterTagNoSelected != null && !areAllTrue(filterTagNoSelected)) { //tag no filter
            if (filterTagNoSelected[0])
                builder.isNull(Asset_.remoteId);
            else if (filterTagNoSelected[1])
                builder.notNull(Asset_.remoteId);
        }

        if (filterAssignmentSelected != null && !areAllTrue(filterAssignmentSelected)) { //assignment filter
            if (filterAssignmentSelected[0])
                builder.equal(Asset_.assignedPersonId, 0);
            if (filterAssignmentSelected[1])
                builder.notEqual(Asset_.assignedPersonId, 0);
        }

        if (filterDeploymentSelected != null && !areAllTrue(filterDeploymentSelected)) { //deployment filter
            if (filterDeploymentSelected[0])
                builder.equal(Asset_.assignedLocationId, 0);
            if (filterDeploymentSelected[1])
                builder.notEqual(Asset_.assignedLocationId, 0);
        }

        if (filterCountingSelected != null && !areAllTrue(filterCountingSelected)) { //deployment filter
            if (filterCountingSelected[0])
                builder.isNull(Asset_.lastControlTime);
            if (filterCountingSelected[1])
                builder.notNull(Asset_.lastControlTime);
        }

        if (filterWSSelected != null && !areAllTrue(filterWSSelected)) { //working state filter
            val wsList = new ArrayList<Integer>();
            if (filterWSSelected[0])
                wsList.add(0);
            if (filterWSSelected[1])
                wsList.add(1);
            if (filterWSSelected[2])
                wsList.add(2);
            if (filterWSSelected[3])
                wsList.add(3);
            builder.in(Asset_.workingState, Ints.toArray(wsList));
        }

        if (filterStoragesSelected != null && !areAllTrue(filterStoragesSelected)) { //storage filter
            var input2 = new Bundle();
            input2.putInt("locationType", ELocationType.Warehouse.id);
            val storageIds = LocationDAO.getDao().getAllIds(input2);
            var selectedIds = new ArrayList<Long>();
            for (int i = 0; i < filterStoragesSelected.length; i++) {
                if (filterStoragesSelected[i])
                    selectedIds.add(storageIds.get(i));
            }
            builder.in(Asset_.storageBelongsToId, Longs.toArray(selectedIds));
        }

        if (filterBudgetSelected != null && !areAllTrue(filterBudgetSelected)) { //budget filter
            val budgetIds = BudgetDAO.getDao().getAllIds(new Bundle());
            var selectedIds = new ArrayList<Long>();
            for (int i = 0; i < filterBudgetSelected.length; i++) {
                if (filterBudgetSelected[i])
                    selectedIds.add(budgetIds.get(i));
            }
            builder.in(Asset_.budgetTypeId, Longs.toArray(selectedIds));
        }

        if (filterPriceType != 0) { //price filter
            if (filterPriceType == 1)
                builder.equal(Asset_.price, filterPrice, 0);
            else if (filterPriceType == 2)
                builder.less(Asset_.price, filterPrice);
            else if (filterPriceType == 3)
                builder.greater(Asset_.price, filterPrice);
            else if (filterPriceType == 4)
                builder.equal(Asset_.price, filterPrice, 100);
            else if (filterPriceType == 5)
                builder.equal(Asset_.price, filterPrice, 1000);
        }

        if (isUpdated)
            builder.equal(Asset_.isUpdated, true);

        if (!Strings.isNullOrEmpty(q)) { //word filter
            val qu = latinize(q);
            builder.contains(Asset_.remoteId, q).or()
                    .startsWith(Asset_.brandNameDefinition, q).or().contains(Asset_.brandNameDefinition, " " + q).or()
                    .startsWith(Asset_.brandNameDefinition, qu).or().contains(Asset_.brandNameDefinition, " " + qu).or()
                    .startsWith(Asset_.modelNameDefinition, q).or().contains(Asset_.modelNameDefinition, " " + q).or()
                    .startsWith(Asset_.modelNameDefinition, qu).or().contains(Asset_.modelNameDefinition, " " + qu).or()
                    .startsWith(Asset_.features, q).or().contains(Asset_.features, " " + q).or()
                    .startsWith(Asset_.features, qu).or().contains(Asset_.features, " " + qu).or()
                    .contains(Asset_.serialNo, qu).or().contains(Asset_.serialNo, q).or()
                    .contains(Asset_.registrationCode, qu).or()
                    .startsWith(Asset_.assetTypeDefinition, qu).or().contains(Asset_.assetTypeDefinition, " " + qu).or()
                    .startsWith(Asset_.assetTypeDefinition, q).or().contains(Asset_.assetTypeDefinition, " " + q).or()
                    .startsWith(Asset_.assignedPersonNameSurname, qu).or().contains(Asset_.assignedPersonNameSurname, " " + qu).or()
                    .startsWith(Asset_.assignedPersonNameSurname, q).or().contains(Asset_.assignedPersonNameSurname, " " + q).or()
                    .startsWith(Asset_.assignedLocationName, qu).or().contains(Asset_.assignedLocationName, " " + qu).or()
                    .startsWith(Asset_.assignedLocationName, q).or().contains(Asset_.assignedLocationName, " " + q).or()
                    .startsWith(Asset_.biomedicalDefinition, qu).or().contains(Asset_.biomedicalDefinition, " " + qu).or()
                    .startsWith(Asset_.biomedicalDefinition, q).or().contains(Asset_.biomedicalDefinition, " " + q).or()
                    .startsWith(Asset_.biomedicalType, qu).or().contains(Asset_.biomedicalType, " " + qu).or()
                    .startsWith(Asset_.biomedicalType, q).or().contains(Asset_.biomedicalType, " " + q).or()
                    .startsWith(Asset_.biomedicalBranch, qu).or().contains(Asset_.biomedicalBranch, " " + qu).or()
                    .startsWith(Asset_.biomedicalBranch, q).or().contains(Asset_.biomedicalBranch, " " + q);
        }

        if (!Strings.isNullOrEmpty(sortBy)) { //sorting
            switch (sortBy) {
                case "last_control":
                    builder.order(Asset_.lastControlTime);
                    break;
                case "labeling":
                    builder.order(Asset_.labelingDateTime);
                    break;
                case "definition":
                    builder.order(Asset_.assetTypeDefinition);
                    break;
                case "person":
                    //TODO:bekleyen zimmet varsa?
                    builder.order(Asset_.assignedPersonNameSurname, QueryBuilder.NULLS_LAST);
                    break;
                case "location":
                    //TODO:bekleyen zimmet varsa?
                    builder.order(Asset_.assignedLocationName, QueryBuilder.NULLS_LAST);
                    break;
            }
        }
        return builder.build();
    }
}
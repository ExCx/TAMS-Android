package com.frekanstan.tatf_demo.app.assettypes;

import android.os.Bundle;
import android.text.TextUtils;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.data.assettypes.IAssetType;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.data.AssetType;
import com.frekanstan.tatf_demo.data.AssetType_;
import com.frekanstan.tatf_demo.data.CountingOp_;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.StringExtensions.latinize;

public class AssetTypeDAO extends DAO<AssetType>
{
    private static AssetTypeDAO instance;

    private AssetTypeDAO() { }

    public static AssetTypeDAO getDao() {
        if (instance == null)
            instance = new AssetTypeDAO();
        return instance;
    }

    @Override
    public Box<AssetType> getBox() {
        return ObjectBox.get().boxFor(AssetType.class);
    }

    public AssetType get(String assetCode) {
        return getBox().query().equal(AssetType_.assetCode, assetCode).build().findFirst();
    }

    @Override
    public long put(AssetType item) {
        item.setDepth(item.getAssetCode().split("-").length); //TODO: seperator
        return getBox().put(item);
    }

    @Override
    public void putAll(List<AssetType> items) {
        for (var x : items)
            x.setDepth(x.getAssetCode().split("-").length); //TODO: seperator
        getBox().put(items);
    }

    public IAssetType findTypeOfDepth(IAssetType lowestType, int depth) {
        val lowestDepth = lowestType.getDepth();
        if (lowestDepth <= depth)
            return lowestType;
        else {
            val splitted = lowestType.getAssetCode().split("-"); //TODO: seperator
            val subSplitted = Arrays.copyOfRange(splitted, 0, depth);
            val code = TextUtils.join("-", subSplitted);
            return get(code);
        }
    }

    @Override
    public Query<AssetType> createFilteredQuery(Bundle input) {
        val q = input.getString("query");
        val depth = input.getInt("depth");
        val assetCode = input.getString("assetCode");
        val hasAssets = input.getByte("hasAssets");
        val countingOpId = input.getLong("countingOpId");

        QueryBuilder<AssetType> builder = getBox().query();
        if (depth != 0) //depth filter
            builder.equal(AssetType_.depth, depth);

        if (hasAssets == 1)  //availability filter
            builder.in(AssetType_.id, AssetTypeRepository.getAvailableTypeIds());
        else if (hasAssets == -1)
            builder.notIn(AssetType_.id, AssetTypeRepository.getAvailableTypeIds());

        if (countingOpId != 0) {
            var typeIds = new ArrayList<Long>();
            val assets = AssetDAO.getDao().getBox().getRelationEntities(CountingOp_.countedAssets, countingOpId);
            for (val asset : assets)
                typeIds.add(asset.getAssetTypeId());
            builder.in(AssetType_.id, Longs.toArray(typeIds));
        }

        if (!Strings.isNullOrEmpty(assetCode))
            builder.equal(AssetType_.assetCode, assetCode).or().startsWith(AssetType_.assetCode, assetCode + "-"); //TODO: seperator
        if (!Strings.isNullOrEmpty(q)) {
            val qu = latinize(q);
            builder.startsWith(AssetType_.definition, q).or().contains(AssetType_.definition, " " + q).or()
                    .startsWith(AssetType_.definition, qu).or().contains(AssetType_.definition, " " + qu).or()
                    .contains(AssetType_.assetCode, q);
        }
        return builder.order(AssetType_.assetCode).build();
    }
}
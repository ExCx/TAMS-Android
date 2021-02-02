package com.frekanstan.dtys_mobil.data;

import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.assettypes.IAssetType;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

@Entity
public class AssetType extends EntityBase implements IAssetType {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private String definition;

    private String assetCode;

    @Getter
    private String remoteId;

    @Getter
    private long parentTypeId;
    @JsonIgnore
    public ToOne<AssetType> parentType;

    @Getter
    private Boolean isActive;

    @Getter @Setter
    private int depth;

    @Backlink(to = "relatedType")
    @JsonIgnore
    public ToMany<CountingOp> countingOps;

    public AssetType() {
    }

    public AssetType(long id, String definition, long parentTypeId, String remoteId, String assetCode, Boolean isActive) {
        this.id = id;
        this.definition = definition;
        this.parentTypeId = parentTypeId;
        this.parentType.setTargetId(parentTypeId);
        this.remoteId = remoteId;
        this.assetCode = assetCode;
        this.isActive = isActive;
    }

    @Override
    public AssetType getParentType() {
        return parentType.getTarget();
    }

    @Override
    public String getAssetCode() {
        val splitted = assetCode.split("-");
        for (int i = 1; i < splitted.length; i++) {
            if (splitted[i].length() == 1) {
                splitted[i] = "0" + splitted[i];
            }
        }
        return TextUtils.join("-", splitted);
    }

    public List<AssetType> getAncestorsAndSelf() {
        var typeList = new ArrayList<AssetType>();
        //typeList.add(this);
        var lastType = this;
        for (int i = depth; i > 1; i--) {
            typeList.add(lastType.getParentType());
            lastType = lastType.getParentType();
        }
        return typeList;
    }
}
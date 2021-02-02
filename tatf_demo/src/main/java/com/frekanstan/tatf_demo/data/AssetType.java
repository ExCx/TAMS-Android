package com.frekanstan.tatf_demo.data;

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
import lombok.var;

@Entity
public class AssetType extends EntityBase implements IAssetType {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private String definition;

    @Getter
    private String assetCode;

    @Getter
    private String remoteId;

    @Getter
    private long parentTypeId;
    public ToOne<AssetType> parentType;

    @Getter @Setter
    private int depth;

    @Backlink(to = "relatedType")
    public ToMany<CountingOp> countingOps;

    public AssetType() {
    }

    public AssetType(long id, String definition, long parentTypeId, String assetCode, String remoteId) {
        this.id = id;
        this.definition = definition;
        this.parentTypeId = parentTypeId;
        if (parentTypeId != 0)
            this.parentType.setTargetId(parentTypeId);
        this.remoteId = remoteId;
        this.assetCode = assetCode;
    }

    @Override
    public AssetType getParentType() {
        return parentType.getTarget();
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
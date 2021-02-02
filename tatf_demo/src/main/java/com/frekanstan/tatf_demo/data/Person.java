package com.frekanstan.tatf_demo.data;

import android.annotation.SuppressLint;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.asset_management.data.people.IPerson;
import com.frekanstan.asset_management.view.MainActivityBase;

import java.util.Date;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Person extends EntityBase implements IPerson {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Convert(converter = EPersonType.PersonTypeTypeConverter.class, dbType = Integer.class)
    @Getter
    private EPersonType personType;

    @Getter
    private String nameSurname;

    @Getter
    private String identityNo;

    @Getter
    private String email;

    @Getter
    private Date labelingDateTime;

    @Getter
    private long userBoundId;

    private boolean hasPhoto = false;
    @Override
    public boolean getHasPhoto() {
        return hasPhoto;
    }

    @Getter
    private long tenantId;
    public ToOne<Tenant> tenant;

    @Backlink(to = "relatedPerson")
    public ToMany<CountingOp> countingOps;

    public Person() {
    }

    public Person(long id, String nameSurname, String email, Date labelingDateTime, long userBoundId, int tenantId, EPersonType personType, String identityNo, boolean hasPhoto) {
        this.id = id;
        this.nameSurname = nameSurname;
        this.email = email;
        this.labelingDateTime = labelingDateTime;
        this.userBoundId = userBoundId;
        this.tenantId = tenantId;
        this.tenant.setTargetId(tenantId);
        this.personType = personType;
        this.identityNo = identityNo;
        this.hasPhoto = hasPhoto;
    }

    @Override
    @SuppressLint("DefaultLocale")
    public String getPersonCode() {
        return String.format("%012d", id);
    }

    @Override
    public void setAsLabeled(boolean labeled) {
        if (labeled)
            this.labelingDateTime = new Date();
        else
            this.labelingDateTime = null;
    }

    @Override
    public void setAsToBeLabeled() {
        this.labelingDateTime = MainActivityBase.nullDate.getTime();
    }
}
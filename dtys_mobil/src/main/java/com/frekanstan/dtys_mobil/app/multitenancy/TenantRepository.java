package com.frekanstan.dtys_mobil.app.multitenancy;

import com.frekanstan.dtys_mobil.data.Tenant;

import lombok.Getter;
import lombok.Setter;

public class TenantRepository
{
    @Getter
    @Setter
    private static Tenant currentTenant;
}
package com.frekanstan.tatf_demo.app.multitenancy;

import com.frekanstan.tatf_demo.data.Tenant;

import lombok.Getter;
import lombok.Setter;

public class TenantRepository
{
    @Getter
    @Setter
    private static Tenant currentTenant;
}
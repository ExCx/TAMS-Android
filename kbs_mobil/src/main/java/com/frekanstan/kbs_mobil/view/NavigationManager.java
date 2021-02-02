package com.frekanstan.kbs_mobil.view;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.navigation.ui.NavigationUI;

import com.frekanstan.kbs_mobil.R;
import com.google.android.material.navigation.NavigationView;

import lombok.val;
import lombok.var;

import static androidx.navigation.Navigation.findNavController;

public class NavigationManager {
    static void setupNavigation(MainActivity context) {
        context.nav = findNavController(context, R.id.nav_host_fragment);
        context.nav.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment || destination.getId() == R.id.splashFragment)
                context.getBinding().toolbar.setVisibility(View.GONE);
            else
                context.getBinding().toolbar.setVisibility(View.VISIBLE);
        });
        NavigationUI.setupActionBarWithNavController(context, context.nav, context.mAppBarConfiguration);
        NavigationView navView = context.findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(item -> {
            context.getBinding().navigationDrawer.closeDrawer(GravityCompat.START);
            val input = new Bundle();
            int itemId = item.getItemId();
            if (itemId == R.id.nav_main_menu)
                context.nav.navigate(R.id.mainMenuFragment);
            else if (itemId == R.id.nav_locate) {
                if (context.controlRfid())
                    context.nav.navigate(R.id.locateRfidFragment);
                else
                    context.nav.navigate(R.id.settingsFragment);
            } else if (itemId == R.id.nav_asset_labeling) {
                context.nav.navigate(R.id.mainMenuAssetLabelingFragment);
            } else if (itemId == R.id.nav_location_labeling) {
                input.putString("operation", "location_labeling");
                context.nav.navigate(R.id.labelingTabsFragment, input);
            } else if (itemId == R.id.nav_person_labeling) {
                input.putString("operation", "person_labeling");
                context.nav.navigate(R.id.labelingTabsFragment, input);
            } else if (itemId == R.id.nav_general_counting) {
                input.putString("operation", "counting");
                context.nav.navigate(R.id.countingTabsFragment, input);
            } else if (itemId == R.id.nav_by_type_counting) {
                input.putString("operation", "counting");
                context.nav.navigate(R.id.assetTypeListFragment, input);
            } else if (itemId == R.id.nav_by_location_counting) {
                input.putString("operation", "counting");
                context.nav.navigate(R.id.locationListFragment, input);
            } else if (itemId == R.id.nav_by_responsible_counting) {
                input.putString("operation", "counting");
                context.nav.navigate(R.id.personListFragment, input);
            } else if (itemId == R.id.nav_settings) {
                context.nav.navigate(R.id.settingsFragment);
            } else if (itemId == R.id.nav_logout) {
                new AlertDialog.Builder(context).setMessage(R.string.logging_out)
                        .setPositiveButton(context.getString(R.string.yes), (dialog, id) -> {
                            context.getBinding().toolbar.setVisibility(View.GONE);
                            context.nav.popBackStack(R.id.loginFragment, true);
                        })
                        .setNegativeButton(context.getString(R.string.no), (dialog, id) -> dialog.dismiss())
                        .show();
            }
            return true;
        });
        NavigationUI.setupWithNavController(context.getBinding().toolbar, context.nav, context.mAppBarConfiguration);
    }

    private static long mLastClickTime = 0;
    static void onBackPressed(MainActivity context) {
        //çift tıkı engelle
        if (SystemClock.elapsedRealtime() - mLastClickTime < 500)
            return;
        mLastClickTime = SystemClock.elapsedRealtime();

        if (context.getBinding().navigationDrawer.isDrawerOpen(GravityCompat.START))
            context.getBinding().navigationDrawer.closeDrawer(GravityCompat.START);
        else if (context.nav.getPreviousBackStackEntry() == null)
            new AlertDialog.Builder(context).setMessage(R.string.quitting_application)
                    .setPositiveButton(context.getString(R.string.yes), (dialog, id) -> context.finish())
                    .setNegativeButton(context.getString(R.string.no), (dialog, id) -> dialog.dismiss())
                    .show();
        else if (context.nav.getPreviousBackStackEntry().getDestination().getId() == R.id.loginFragment)
            new AlertDialog.Builder(context).setMessage(R.string.logging_out)
                    .setPositiveButton(context.getString(R.string.yes), (dialog, id) -> {
                        context.getBinding().toolbar.setVisibility(View.GONE);
                        context.nav.popBackStack();
                    })
                    .setNegativeButton(context.getString(R.string.no), (dialog, id) -> dialog.dismiss())
                    .show();
        else
            context.nav.popBackStack();
    }

    public static void goToAssetDetails(MainActivity context, long assetId, int actionId)
    {
        //context.homeShouldOpenDrawer = false;
        var bundle = new Bundle();
        bundle.putLong("assetId", assetId);
        context.nav.navigate(actionId, bundle);
    }
}

package com.frekanstan.tatf_demo.view;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.preference.PreferenceManager;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.webservice.LoginInput;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.view.MainActivityBase;
import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ICanScanRfid;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.connection.ServiceConnector;
import com.frekanstan.tatf_demo.app.sync.AssetDetailsSynchronizer;
import com.frekanstan.tatf_demo.app.sync.AssignmentChangeDAO;
import com.frekanstan.tatf_demo.app.sync.AssignmentSynchronizer;
import com.frekanstan.tatf_demo.app.sync.CountedStateChangeDAO;
import com.frekanstan.tatf_demo.app.sync.CountingOpSynchronizer;
import com.frekanstan.tatf_demo.app.sync.CountingSynchronizer;
import com.frekanstan.tatf_demo.app.sync.ImageSynchronizer;
import com.frekanstan.tatf_demo.app.sync.ImageToUploadDAO;
import com.frekanstan.tatf_demo.app.sync.LabelSynchronizer;
import com.frekanstan.tatf_demo.app.sync.LabeledStateChangeDAO;
import com.frekanstan.tatf_demo.app.tracking.CountingOpDAO;
import com.frekanstan.tatf_demo.data.MyObjectBox;
import com.frekanstan.tatf_demo.databinding.ActivityMainBinding;
import com.frekanstan.tatf_demo.view.settings.SettingsFragment;

import java.util.Set;

import io.objectbox.reactive.DataSubscription;
import lombok.Getter;
import lombok.val;
import lombok.var;

import static androidx.navigation.Navigation.findNavController;

public class MainActivity extends MainActivityBase
{
    @Getter
    private ActivityMainBinding binding;

    //----------------------------------------------------------------------------------------------
    // Initialization
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            //objectbox init
            ObjectBox.init(MyObjectBox.builder()
                    .androidContext(getApplicationContext())
                    .name("tatf_demo")
                    .build());

            //layout
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            initializeLayout(binding.getRoot(), binding.toolbar, binding.progressBar, binding.fab, binding.navigationDrawer, binding.rfidButton, binding.wifiButton);
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.mainMenuFragment)
                    .setOpenableLayout(binding.navigationDrawer)
                    .build();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        NavigationManager.setupNavigation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //----------------------------------------------------------------------------------------------
    // Triggered Events
    //----------------------------------------------------------------------------------------------

    DataSubscription assetSub, assignmentSub, labelingSub, countingSub, countingOpSub, imageSub;
    @Override
    public void onNetChecked(Boolean result) {
        if (result)
            wifiButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, com.frekanstan.asset_management.R.color.white)));
        else
            wifiButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, com.frekanstan.asset_management.R.color.red_500)));
        //first run
        if (NetManager.isOnline == null) {
            NetManager.isOnline = result;
            findNavController(this, R.id.nav_host_fragment)
                    .navigate(R.id.action_splashFragment_to_loginFragment);
        }
        //online-offline switch
        else if (NetManager.isOnline != result)
        {
            NetManager.isOnline = result;
            if (NetManager.isOnline) {
                Toast.makeText(this, getString(R.string.alert_established_connection), Toast.LENGTH_LONG).show();

                if (ServiceConnector.isLoggedIn) {
                    if (assignmentSub == null) {
                        onLogin(true);
                    } else {
                        AssetDetailsSynchronizer.getInstance(this).syncCache();
                        AssignmentSynchronizer.getInstance(this).syncCache();
                        LabelSynchronizer.getInstance(this).syncCache();
                        CountingSynchronizer.getInstance(this).syncCache();
                        CountingOpSynchronizer.getInstance(this).syncCache();
                        ImageSynchronizer.getInstance(this).syncCache();
                    }
                }
            }
            else {
                Toast.makeText(this, getString(R.string.alert_cannot_establish_connection), Toast.LENGTH_LONG).show();
            }
            //this.changeActionBarColor();
        }
    }

    public void onLogin(boolean reAuthenticate) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this);
        val loginInput = new LoginInput(prefs.getString("tenancyName", ""),
                prefs.getString("userName", ""),
                prefs.getString("password", ""));
        if (reAuthenticate) {
            ServiceConnector.getInstance(this).getAuthentication(loginInput, response -> {
                ServiceConnector.getInstance(this).setHeaders(response.getResult());
                subscribeSynchronizers();
            });
        }
        else
            subscribeSynchronizers();
    }

    private void subscribeSynchronizers() {
        if (assetSub == null) {
            var onlyUpdated = new Bundle();
            onlyUpdated.putBoolean("isUpdated", true);
            assetSub = AssetDAO.getDao().subscribe(AssetDetailsSynchronizer.getInstance(this), onlyUpdated);
            var input = new Bundle();
            input.putBoolean("sent", false);
            assignmentSub = AssignmentChangeDAO.getDao().subscribe(AssignmentSynchronizer.getInstance(this), input);
            labelingSub = LabeledStateChangeDAO.getDao().subscribe(LabelSynchronizer.getInstance(this), new Bundle());
            var readyToSend = new Bundle();
            readyToSend.putBoolean("readyToSend", true);
            countingSub = CountedStateChangeDAO.getDao().subscribe(CountingSynchronizer.getInstance(this), readyToSend);
            countingOpSub = CountingOpDAO.getDao().subscribe(CountingOpSynchronizer.getInstance(this), onlyUpdated);
            imageSub = ImageToUploadDAO.getDao().subscribe(ImageSynchronizer.getInstance(this), new Bundle());
        }
    }

    @Override
    public void onBarcodeScanned(String code) {
        val curFragment = getCurrentFragment();
        if (curFragment instanceof ICanScanCode)
            ((ICanScanCode)curFragment).onCodeScanned(code);
        else if (curFragment instanceof ISearchableListFragment)
            ((ISearchableListFragment) curFragment).setQuery(code);
    }

    @Override
    public void onBluetoothConnected(Set<BluetoothDevice> pairedDevices) {
        val curFragment = getCurrentFragment();
        if (curFragment instanceof SettingsFragment)
            ((SettingsFragment)curFragment).populateBluetoothDevices(pairedDevices);
    }

    @Override
    public void onRfidScanned(String code) {
        val curFragment = getCurrentFragment();
        if (curFragment instanceof ICanScanRfid)
            ((ICanScanRfid)curFragment).onRfidScanned(code);
    }

    public void showHideFooter(Boolean visibility) {
        if (!visibility)
            binding.footerArea.setVisibility(View.GONE);
        else
            binding.footerArea.setVisibility(View.VISIBLE);
    }

    public void setFooterText(String str) {
        binding.footerText.setText(str);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void onBackPressed() {
        NavigationManager.onBackPressed(this);
    }
}
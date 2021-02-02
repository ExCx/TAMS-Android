package com.frekanstan.kbs_mobil.view.login;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.multidex.BuildConfig;
import androidx.preference.PreferenceManager;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.multitenancy.TenantGetAllInput;
import com.frekanstan.asset_management.app.update.AppUpdater;
import com.frekanstan.asset_management.app.update.ILoginFragment;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.multitenancy.ITenant;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.connection.ServiceConnector;
import com.frekanstan.kbs_mobil.app.multitenancy.TenantDAO;
import com.frekanstan.kbs_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.kbs_mobil.app.people.PersonDAO;
import com.frekanstan.kbs_mobil.app.people.PersonRepository;
import com.frekanstan.kbs_mobil.app.sync.DataSynchronizer;
import com.frekanstan.kbs_mobil.data.Tenant;
import com.frekanstan.kbs_mobil.databinding.LoginFragmentBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Objects;

import lombok.SneakyThrows;
import lombok.val;
import lombok.var;

public class LoginFragment extends Fragment implements ILoginFragment {

    private MainActivity mContext;
    private ServiceConnector conn;
    private long mLastClickTime = 0;
    private String user, pass;
    private LoginFragmentBinding view;
    private TenantDAO tenantDAO = TenantDAO.getDao();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = (MainActivity)context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conn = ServiceConnector.getInstance(mContext);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = LoginFragmentBinding.inflate(inflater, container, false);
        setHasOptionsMenu(false);
        ServiceConnector.isLoggedIn = false;

        //user & pass
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        user = prefs.getString("userName", "");
        pass = prefs.getString("password", "");
        if (!user.equals("")) {
            view.userName.setText(user);
            if (pass.equals(""))
                view.password.requestFocus();
        }
        if (!pass.equals(""))
            view.password.setText(pass);

        view.password.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onLoadClick(view.loadDatas);
                return true;
            }
            return false;
        });

        //version
        view.version.setText(String.format(getString(R.string.version_prefix), BuildConfig.VERSION_NAME));
        if (AppUpdater.isBusy)
            return view.getRoot();
        //update control
        if (NetManager.isOnline != null && NetManager.isOnline) {
            ObjectRequest<AbpResult<ArrayList>> request = conn.getAllTenantsReq(new TenantGetAllInput());
            request.setResponseListener(response -> {
                val gson = new Gson();
                var list = new ArrayList<Tenant>();
                for (Object obj : response.getResult())
                    list.add(gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Tenant.class));
                tenantDAO.putAll(list);

                //populate spinner
                val adapter = new ArrayAdapter<ITenant>(
                        mContext,
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>(tenantDAO.getAll()));
                adapter.insert(new Tenant(0, null, mContext.getString(R.string.institution_name), true), 0);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                view.tenantSelect.setAdapter(adapter);
                String tenancyName = prefs.getString("tenancyName", "");
                val tenant = tenantDAO.get(tenancyName);
                int pos = adapter.getPosition(tenant);
                view.tenantSelect.setSelection(pos);
                mContext.progDialog.hide();
            });
            conn.addToRequestQueue(request);
        }
        else {
            //populate spinner
            var adapter = new ArrayAdapter<ITenant>(
                    mContext,
                    android.R.layout.simple_spinner_item,
                    new ArrayList<>(tenantDAO.getAll()));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            view.tenantSelect.setAdapter(adapter);
            mContext.progDialog.hide();
            val tenancyName = prefs.getString("tenancyName", "");
            if (tenancyName.equals(""))
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.title_no_internet_connection)
                        .setMessage(R.string.alert_should_load_online_on_first_run)
                        .setPositiveButton(R.string.ok, (dialog, id) -> mContext.finish())
                        .show();
            else
                view.tenantSelect.setSelection(adapter.getPosition(tenantDAO.get(tenancyName)));
        }

        view.loadDatas.setOnClickListener(this::onLoadClick);
        view.loadDatas.setOnLongClickListener(this::onLoadLongClick);
        return view.getRoot();
    }


    private boolean forceUpdate = false;
    @SneakyThrows
    private void onLoadClick(View v) {
        mContext.hideKeyboard();
        forceUpdate = false;
        validateAndSyncData();
        v.setEnabled(true);
    }

    private boolean onLoadLongClick(View v) {
        mContext.hideKeyboard();
        forceUpdate = true;
        validateAndSyncData();
        v.setEnabled(true);
        return true;
    }

    private void validateAndSyncData() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000)
            return;
        mLastClickTime = SystemClock.elapsedRealtime();
        //validation
        if (view.tenantSelect.getSelectedItem() == null)
            Toast.makeText(getActivity(), R.string.alert_tenant_not_selected, Toast.LENGTH_LONG).show();
        else if (view.tenantSelect.getSelectedItem().toString().equals(mContext.getString(R.string.institution_name)))
            Toast.makeText(getActivity(), R.string.alert_tenant_not_selected, Toast.LENGTH_LONG).show();
        else if (Objects.requireNonNull(view.userName.getText()).toString().equals("") || Objects.requireNonNull(view.password.getText()).toString().equals(""))
            Toast.makeText(getActivity(), R.string.alert_empty_username_password, Toast.LENGTH_LONG).show();
        else {
            TenantRepository.setCurrentTenant((Tenant) view.tenantSelect.getSelectedItem());
            if (NetManager.isOnline) {
                try {
                    val pName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).packageName;
                    if (Build.VERSION.SDK_INT <= 21) {
                        AppUpdater.installApp(mContext, this, "versiyongooglequicksearchbox",
                                "googlequicksearchbox", "com.google.android.googlequicksearchbox",
                                mContext.getString(R.string.web_service_url));
                    }
                    //initiateLogin();
                    AppUpdater.installApp(mContext, this, "versiyonkbsmobil", "kbs_mobil", pName, mContext.getString(R.string.web_service_url));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                if (view.userName.getText().toString().equals(user) && view.password.getText().toString().equals(pass))
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.title_no_internet_connection)
                            .setMessage(R.string.alert_data_on_device_will_be_loaded)
                            .setPositiveButton(R.string.yes, (dialog, id) -> {
                                dialog.dismiss();
                                PersonRepository.setCurrentPerson(PersonDAO.getDao().get(user));
                                new DataSynchronizer.DbInitializer(mContext, true).execute();
                            })
                            .setNegativeButton(R.string.no, (dialog, id) -> dialog.dismiss())
                            .show();
                else if (user.equals(""))
                    Toast.makeText(getActivity(), R.string.alert_should_load_online_on_first_run, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), R.string.wrong_username_or_password, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void initiateLogin() {
        login(forceUpdate);
    }

    private void login(boolean forceUpdate) {
        val ds = new DataSynchronizer(
                mContext,
                ((Tenant) view.tenantSelect.getSelectedItem()).getTenancyName(),
                Objects.requireNonNull(view.userName.getText()).toString(),
                Objects.requireNonNull(view.password.getText()).toString(),
                forceUpdate);
        mContext.progDialog.show();
        mContext.progDialog.setMessage(mContext.getString(com.frekanstan.asset_management.R.string.connecting_application_server));
        ds.authenticate();
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.actionButton.hide();
        mContext.showHideFooter(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        view = null;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        mContext.getBinding().toolbar.setNavigationIcon(null);
    }
}
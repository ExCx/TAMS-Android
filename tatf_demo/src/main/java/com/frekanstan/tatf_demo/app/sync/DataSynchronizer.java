package com.frekanstan.tatf_demo.app.sync;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.connection.ServiceConnectorBase;
import com.frekanstan.asset_management.app.helpers.JsonListParser;
import com.frekanstan.asset_management.app.locations.LocationTypeDeserializer;
import com.frekanstan.asset_management.app.maintenance.WorkingStateDeserializer;
import com.frekanstan.asset_management.app.people.PersonTypeDeserializer;
import com.frekanstan.asset_management.app.sync.DateDeserializer;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.app.webservice.LoginInput;
import com.frekanstan.asset_management.data.GetAllInput;
import com.frekanstan.asset_management.data.acquisition.EWorkingState;
import com.frekanstan.asset_management.data.assettypes.AssetTypeGetAllInput;
import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.acquisition.BudgetDAO;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.assets.BrandDAO;
import com.frekanstan.tatf_demo.app.assets.ModelDAO;
import com.frekanstan.tatf_demo.app.assettypes.AssetTypeDAO;
import com.frekanstan.tatf_demo.app.assettypes.AssetTypeRepository;
import com.frekanstan.tatf_demo.app.connection.ServiceConnector;
import com.frekanstan.tatf_demo.app.locations.LocationDAO;
import com.frekanstan.tatf_demo.app.locations.LocationRepository;
import com.frekanstan.tatf_demo.app.people.PersonDAO;
import com.frekanstan.tatf_demo.app.people.PersonRepository;
import com.frekanstan.tatf_demo.app.settings.SettingDAO;
import com.frekanstan.tatf_demo.app.tracking.CountingOpDAO;
import com.frekanstan.tatf_demo.data.Asset;
import com.frekanstan.tatf_demo.data.AssetType;
import com.frekanstan.tatf_demo.data.Asset_;
import com.frekanstan.tatf_demo.data.Brand;
import com.frekanstan.tatf_demo.data.Budget;
import com.frekanstan.tatf_demo.data.CountedStateChange;
import com.frekanstan.tatf_demo.data.CountingOp;
import com.frekanstan.tatf_demo.data.CountingOp_;
import com.frekanstan.tatf_demo.data.Location;
import com.frekanstan.tatf_demo.data.Model;
import com.frekanstan.tatf_demo.data.Person;
import com.frekanstan.tatf_demo.data.Setting;
import com.frekanstan.tatf_demo.view.MainActivity;
import com.google.common.primitives.Longs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.val;
import lombok.var;

public class DataSynchronizer {
    private final MainActivity mContext;
    private static String mUsername, mPassword, mTenancyName;
    private static ServiceConnector conn;
    private static int completedCount;
    private final boolean mForceUpdate;

    public DataSynchronizer(MainActivity context, String tenancyName, String username, String password, boolean forceUpdate) {
        mContext = context;
        mTenancyName = tenancyName;
        mUsername = username;
        mPassword = password;
        mForceUpdate = forceUpdate;
        conn = ServiceConnector.getInstance(mContext);
        completedCount = 0;
        NetManager.isBusy = true;
    }

    public void authenticate() {
        conn.getAuthentication(
                new LoginInput(mTenancyName, mUsername, mPassword),
                this::onAuthenticateResponse);
    }

    private void onAuthenticateResponse(AbpResult<String> response) {
        conn.setHeaders(response.getResult());
        //add user to prefs
        PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .putString("tenancyName", mTenancyName)
                .putString("userName", mUsername)
                .putString("password", mPassword).apply();
        mContext.progDialog.setMessage(mContext.getString(R.string.loading_data));
        conn.getAllSettings(this::onSettingsResponse);
    }

    private void onSettingsResponse(AbpResult<ArrayList> response) {
        val gson = new Gson();
        var list = new ArrayList<Setting>();
        var isUpToDate = false;
        val mEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        val settingDao = SettingDAO.getDao();
        for (val obj : response.getResult()) {
            val entity = gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Setting.class);
            if (entity.getValue() == null)
                continue;
            val oldEntity = settingDao.getByName(entity.getName());
            if (oldEntity != null) {
                if (entity.getName().equals("Synchronization.LastUpdateDateTime"))
                {
                    if (entity.getValue().equals(oldEntity.getValue()))
                        isUpToDate = true;
                }
                if (entity.getName().equals("Institution.InstitutionName"))
                    mEditor.putString("company_name_prefs", entity.getValue());
                if (entity.getName().equals("Institution.UnionName"))
                    mEditor.putString("main_company_name_prefs", entity.getValue());
                entity.setId(oldEntity.getId());
            }
            list.add(entity);
        }
        mEditor.apply();
        settingDao.putAll(list);
        if (mForceUpdate)
            loadAllToDb();
        else {
            if (isUpToDate) {
                PersonRepository.setCurrentPerson(PersonDAO.getDao().get(mUsername));
                new DbInitializer(mContext, false).execute();
            }
            else
                loadAllToDb();
        }
    }

    private void loadAllToDb() {
        conn.getAllAssetTypes(new AssetTypeGetAllInput(""), this::onAssetTypeResponse);
        conn.getAllEntities("person", new GetAllInput<Person>(), this::onPeopleResponse);
        conn.getAllEntities("location", new GetAllInput<Location>(), this::onLocationResponse);
        conn.getAllEntities("brand", new GetAllInput<Brand>(), this::onBrandResponse);
        conn.getAllEntities("model", new GetAllInput<Model>(), this::onModelResponse);
        conn.getAllEntities("budget", new GetAllInput<Budget>(), this::onBudgetResponse);
        conn.getAllEntities("countingOp", new GetAllInput<CountingOp>(), this::onCountingOpResponse);
    }

    private void onAssetTypeResponse(AbpResult<ArrayList> response) {
        new JsonListParser<>(new Gson(), AssetType.class, response, AssetTypeDAO.getDao()).execute();
        loadAssets();
    }

    private void onPeopleResponse(AbpResult<ArrayList> response) {
        val gson = new GsonBuilder()
                .registerTypeAdapter(EPersonType.class, new PersonTypeDeserializer())
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
        new JsonListParser<>(gson, Person.class, response, PersonDAO.getDao()).execute();
        val request = conn.getCurrentPersonId();
        request.setResponseListener(r -> {
            PersonRepository.setCurrentPerson(PersonDAO.getDao().get(r.getResult().longValue()));
            loadAssets();
        });
        conn.addToRequestQueue(request);
    }

    private void onLocationResponse(AbpResult<ArrayList> response) {
        val gson = new GsonBuilder()
                .registerTypeAdapter(ELocationType.class, new LocationTypeDeserializer())
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
        new JsonListParser<>(gson, Location.class, response, LocationDAO.getDao()).execute();
        loadAssets();
    }

    private void onBrandResponse(AbpResult<ArrayList> response) {
        new JsonListParser<>(new Gson(), Brand.class, response, BrandDAO.getDao()).execute();
        loadAssets();
    }

    private void onModelResponse(AbpResult<ArrayList> response) {
        new JsonListParser<>(new Gson(), Model.class, response, ModelDAO.getDao()).execute();
        loadAssets();
    }

    private void onBudgetResponse(AbpResult<ArrayList> response) {
        new JsonListParser<>(new Gson(), Budget.class, response, BudgetDAO.getDao()).execute();
        loadAssets();
    }

    private void onCountingOpResponse(AbpResult<ArrayList> response) {
        val gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
        var list = new ArrayList<CountingOp>();
        val box = CountingOpDAO.getDao().getBox();
        for (val obj : response.getResult()) {
            var copremote = gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), CountingOp.class);
            var coplocal = box.query().equal(CountingOp_.globalId, copremote.getId()).build().findFirst();
            copremote.setGlobalId(copremote.getId());
            if (coplocal == null)
                copremote.setId(0);
            else
                copremote.setId(coplocal.getId());
            list.add(copremote);
        }
        CountingOpDAO.getDao().putAll(list);
        loadAssets();
    }

    private void loadAssets() {
        if (++completedCount < 7)
            return;
        mContext.progDialog.setMessage(mContext.getString(R.string.loading_asset_details_from_server));
        conn.getAllEntities("asset", new GetAllInput<Asset>(), this::onAssetResponse);
    }

    private void onAssetResponse(AbpResult<ArrayList> response) {
        val gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(EWorkingState.class, new WorkingStateDeserializer())
                .create();
        mContext.progDialog.setMessage(mContext.getString(R.string.loading_asset_details));
        new JsonListParser<>(gson, Asset.class, response, AssetDAO.getDao()).execute();
        val opIds = CountingOpDAO.getDao().getAllIds(new Bundle());
        if (opIds.size() == 0)
            lastOpId = 0;
        else
            lastOpId = opIds.get(opIds.size() - 1);
        loadCountedAssetIds(1);
    }

    long lastOpId;
    private void loadCountedAssetIds(long opId) {
        val op = CountingOpDAO.getDao().get(opId);
        if (opId > lastOpId)
            new DbInitializer(mContext, true).execute();
        else if (op == null)
            loadCountedAssetIds(opId + 1);
        else {
            val input = new CountedStateChange(0, null, 0, op.getGlobalId());
            ObjectRequest<AbpResult<ArrayList<Double>>> request = conn.getCountedAssetIdsReq(input);
            request.setResponseListener(response -> {
                if (response.getSuccess()) {
                    op.countedAssets.clear();
                    op.countedAssets.addAll(AssetDAO.getDao().getBox().get(Longs.toArray(response.getResult())));
                    CountingOpDAO.getDao().put(op);
                    loadCountedAssetIds(opId + 1);
                }
            });
            conn.addToRequestQueue(request);
        }
    }

    public static class DbInitializer extends AsyncTask<Void, Void, Boolean>
    {
        private final boolean populateExtendedProps;
        WeakReference<MainActivity> context;
        List<Asset> list;

        public DbInitializer(MainActivity activity, boolean populateExtendedProps) {
            //this.list = list;
            this.populateExtendedProps = populateExtendedProps;
            context = new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            List<Asset> list2 = new ArrayList<>();
            var personIds = new ArrayList<Long>();
            var locationIds = new ArrayList<Long>();
            var typeIds = new ArrayList<Long>();
            try {
                for (val asset : AssetDAO.getDao().getAll()) {
                    if (populateExtendedProps) {
                        if (asset.getAssetTypeId() != 0)
                            asset.setAssetTypeDefinition(asset.getAssetType().getDefinition());
                        if (asset.getBrandNameId() != 0)
                            asset.setBrandNameDefinition(asset.getBrandName().getDefinition());
                        if (asset.getModelNameId() != 0)
                            asset.setModelNameDefinition(asset.getModelName().getDefinition());
                    }
                    for (val typeId : asset.getAllTypeIds()) {
                        if (!typeIds.contains(typeId))
                            typeIds.add(typeId);
                    }
                    val personId = asset.getAssignedPersonId();
                    if (personId != 0) {
                        if (populateExtendedProps) {
                            val person = asset.getAssignedPerson();
                            if (person == null)
                                asset.setAssignedPersonNameSurname("ESKİ PERSONEL");
                            else
                                asset.setAssignedPersonNameSurname(person.getNameSurname());
                        }
                        if (!personIds.contains(personId))
                            personIds.add(personId);
                    }
                    val locationId = asset.getAssignedLocationId();
                    if (locationId != 0) {
                        if (populateExtendedProps) {
                            val location = asset.getAssignedLocation();
                            if (location == null)
                                asset.setAssignedLocationName("BİLİNMİYOR");
                            else
                                asset.setAssignedLocationName(location.getName());
                        }
                        if (!locationIds.contains(locationId))
                            locationIds.add(locationId);
                    }
                    if (populateExtendedProps)
                        list2.add(asset);
                }
                list = null;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            if (populateExtendedProps)
                AssetDAO.getDao().putAll(list2);
            var listToUpdate = new ArrayList<Asset>();
            for (val asset : AssetDAO.getDao().getBox().query().equal(Asset_.tempCounted, true).build().find()) {
                asset.setTempCounted(false);
                listToUpdate.add(asset);
            }
            AssetDAO.getDao().putAll(listToUpdate);
            PersonRepository.setAssignedPersonIds(Longs.toArray(personIds)); //cache assigned people
            LocationRepository.setAssignedLocationIds(Longs.toArray(locationIds)); //cache assigned locations
            AssetTypeRepository.setAvailableTypeIds(Longs.toArray(typeIds)); //cache available types
            AssetTypeRepository.buildInitialTree(); //cache type tree
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result)
                goToMainScreen(context.get());
            else {
                context.get().progDialog.hide();
                Toast.makeText(context.get(), R.string.alert_initialize_db_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static void goToMainScreen(MainActivity context) {
        ServiceConnectorBase.isLoggedIn = true;
        NetManager.isBusy = false;
        context.onLogin(false);
        Navigation.findNavController(context, R.id.nav_host_fragment).navigate(R.id.mainMenuFragment);
    }
}
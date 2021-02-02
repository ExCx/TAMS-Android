package com.frekanstan.dtys_mobil.app.sync;

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
import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.app.acquisition.BudgetDAO;
import com.frekanstan.dtys_mobil.app.assets.AssetDAO;
import com.frekanstan.dtys_mobil.app.assets.BrandDAO;
import com.frekanstan.dtys_mobil.app.assets.ModelDAO;
import com.frekanstan.dtys_mobil.app.assettypes.AssetTypeDAO;
import com.frekanstan.dtys_mobil.app.assettypes.AssetTypeRepository;
import com.frekanstan.dtys_mobil.app.connection.ServiceConnector;
import com.frekanstan.dtys_mobil.app.locations.LocationDAO;
import com.frekanstan.dtys_mobil.app.locations.LocationRepository;
import com.frekanstan.dtys_mobil.app.people.PersonDAO;
import com.frekanstan.dtys_mobil.app.people.PersonRepository;
import com.frekanstan.dtys_mobil.app.settings.SettingDAO;
import com.frekanstan.dtys_mobil.app.tracking.CountingOpDAO;
import com.frekanstan.dtys_mobil.data.Asset;
import com.frekanstan.dtys_mobil.data.AssetType;
import com.frekanstan.dtys_mobil.data.Asset_;
import com.frekanstan.dtys_mobil.data.Brand;
import com.frekanstan.dtys_mobil.data.Budget;
import com.frekanstan.dtys_mobil.data.CountedStateChange;
import com.frekanstan.dtys_mobil.data.CountingOp;
import com.frekanstan.dtys_mobil.data.CountingOp_;
import com.frekanstan.dtys_mobil.data.GetAllPersons;
import com.frekanstan.dtys_mobil.data.Location;
import com.frekanstan.dtys_mobil.data.Model;
import com.frekanstan.dtys_mobil.data.Person;
import com.frekanstan.dtys_mobil.data.Setting;
import com.frekanstan.dtys_mobil.view.MainActivity;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        /*conn.getAllAssetTypes(new AssetTypeGetAllInput("255"), this::onAssetTypeResponse);
        conn.getAllAssetTypes(new AssetTypeGetAllInput("253"), this::onAssetTypeResponse);
        conn.getAllAssetTypes(new AssetTypeGetAllInput("254"), this::onAssetTypeResponse);*/
        conn.getAllAssetTypes(new AssetTypeGetAllInput(""), this::onAssetTypeResponse);
        var personInput = new GetAllPersons();
        personInput.setHasLeftJob(false);
        personInput.setMaxResultCount(10000);
        personInput.setSkipCount(0);
        conn.getAllPersons("person", personInput, this::onPeopleResponse);
        //conn.getAllEntities("person", personInput, this::onPeopleResponse);
        var locationInput = new GetAllInput<Location>();
        locationInput.setMaxResultCount(10000);
        locationInput.setSkipCount(0);
        conn.getAllEntities("location", locationInput, this::onLocationResponse);
        conn.getAllEntities("brand", new GetAllInput<Brand>(), this::onBrandResponse);
        conn.getAllEntities("model", new GetAllInput<Model>(), this::onModelResponse);
        conn.getAllEntities("budget", new GetAllInput<Budget>(), this::onBudgetResponse);
        conn.getAllEntities("countingOp", new GetAllInput<CountingOp>(), this::onCountingOpResponse);
    }

    private void onAssetTypeResponse(AbpResult<ArrayList> response) {
        new JsonListParser<>(new Gson(), AssetType.class, response, AssetTypeDAO.getDao()).execute();
        loadAssets();
    }

    private int personSkipCount = 0;
    private void onPeopleResponse(AbpResult<ArrayList> response) {
        val gson = new GsonBuilder()
                .registerTypeAdapter(EPersonType.class, new PersonTypeDeserializer())
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
        new JsonListParser<>(gson, Person.class, response, PersonDAO.getDao()).execute();
        if (response.getResult().size() < 10000) {
            personSkipCount = 0;
            val request = conn.getCurrentPersonId();
            request.setResponseListener(r -> {
                PersonRepository.setCurrentPerson(PersonDAO.getDao().get(r.getResult().longValue()));
                loadAssets();
            });
            conn.addToRequestQueue(request);
        }
        else {
            var personInput = new GetAllPersons();
            personInput.setHasLeftJob(false);
            personInput.setMaxResultCount(10000);
            personSkipCount += 10000;
            personInput.setSkipCount(personSkipCount);
            conn.getAllPersons("person", personInput, this::onPeopleResponse);
        }
    }

    private int locationSkipCount = 0;
    private void onLocationResponse(AbpResult<ArrayList> response) {
        val gson = new GsonBuilder()
                .registerTypeAdapter(ELocationType.class, new LocationTypeDeserializer())
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
        new JsonListParser<>(gson, Location.class, response, LocationDAO.getDao()).execute();
        if (response.getResult().size() < 10000) {
            locationSkipCount = 0;
            loadAssets();
        }
        else {
            var locationInput = new GetAllInput<Location>();
            locationInput.setMaxResultCount(10000);
            locationSkipCount += 10000;
            locationInput.setSkipCount(locationSkipCount);
            conn.getAllEntities("location", locationInput, this::onLocationResponse);
        }
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
        var assetInput = new GetAllInput<Asset>();
        assetInput.setSkipCount(0);
        assetInput.setMaxResultCount(10000);
        conn.getAllEntities("asset", assetInput, this::onAssetResponse);
    }

    private int assetSkipCount = 0;
    private void onAssetResponse(AbpResult<ArrayList> response) {
        val gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(EWorkingState.class, new WorkingStateDeserializer())
                .create();
        mContext.progDialog.setMessage(mContext.getString(R.string.loading_asset_details));
        new JsonListParser<>(gson, Asset.class, response, AssetDAO.getDao()).execute();
        if (response.getResult().size() < 10000) {
            assetSkipCount = 0;
            val opIds = CountingOpDAO.getDao().getAllIds(new Bundle());
            if (opIds.size() == 0)
                lastOpId = 0;
            else
                lastOpId = opIds.get(opIds.size() - 1);
            loadCountedAssetIds(1);
        }
        else {
            var assetInput = new GetAllInput<Asset>();
            assetInput.setMaxResultCount(10000);
            assetSkipCount += 10000;
            assetInput.setSkipCount(assetSkipCount);
            conn.getAllEntities("asset", assetInput, this::onAssetResponse);
        }
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
                            else if (Strings.isNullOrEmpty(location.getShortName()))
                                asset.setAssignedLocationName(location.getName());
                            else
                                asset.setAssignedLocationName(location.getShortName());
                        }
                        if (!locationIds.contains(locationId))
                            locationIds.add(locationId);
                    }
                    if (asset.getRfidCode() == null)
                        asset.setRfidCode("C05E" + String.format(Locale.ENGLISH, "%020d", asset.getId()));
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
            //AssetTypeRepository.buildInitialTree(); //cache type tree
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
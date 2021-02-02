package com.frekanstan.asset_management.view;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.multidex.BuildConfig;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.preference.PreferenceManager;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.helpers.SoundGenerator;
import com.frekanstan.asset_management.app.tracking.IMountedBarcodeReaderManager;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceListener;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.app.tracking.MountedBarcodeReaderManager;
import com.frekanstan.asset_management.app.tracking.hopeland.HopelandDeviceManager;
import com.frekanstan.asset_management.app.tracking.nur.NurDeviceManager;
import com.frekanstan.asset_management.app.tracking.tsl.TslDeviceManager;
import com.frekanstan.asset_management.data.helpers.TryParsers;
import com.frekanstan.asset_management.view.shared.ICanScanRfid;
import com.frekanstan.asset_management.view.shared.ISearchableFragment;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.asset_management.view.tracking.ICanScanRange;
import com.frekanstan.asset_management.view.tracking.TagFinderDialogFragment;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.uk.tsl.rfid.DeviceListActivity;

import org.reactivestreams.Subscription;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.data.helpers.TryParsers.tryParseLong;

//import io.reactivex.disposables.CompositeDisposable;

public abstract class MainActivityBase extends AppCompatActivity implements NetManager.NetCheckedListener {
    private static final int REQUEST_ENABLE_BT = 0x2122;
    private static final int REQUEST_SPEECH = 0x2121;

    public static DateFormat dateFormat, dateTimeFormat;
    public static final Calendar nullDate = Calendar.getInstance();
    public static File assetPhotosFolder, personPhotosFolder, templatesFolder;

    public int width, height;
    public ProgressDialog progDialog;
    public FloatingActionButton actionButton;
    public AppBarConfiguration mAppBarConfiguration;
    public NavController nav;

    //rfid
    public IRfidDeviceManager rfidManager = null;
    public IMountedBarcodeReaderManager barcodeManager;

    private Toolbar toolbar;
    private ContentLoadingProgressBar progBar;
    private CompositeDisposable compositeDisposable;

    @Getter
    @Setter
    private TagFinderDialogFragment tfDialog;
    public ImageButton rfidButton, wifiButton;
    private SoundGenerator soundGenerator;

    //----------------------------------------------------------------------------------------------
    // Initialization
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) { //first run
            checkPermissions(); //izinleri runtime esnasında kontrol et
            setupFolders(); //gerekli klasörleri aç
            initialize();
        }
    }

    private Subscription onlineSubscription;
    long nbNoodles;
    protected void initialize() {
        compositeDisposable = new CompositeDisposable();
        /*Nodle.onStart(this, "6b56f362-15ed-48a6-ad1b-6784c8613ec4");
        //Nodle.setTrackingModeStatic(this, true);
        compositeDisposable.add(
                Nodle.sdkEvent()
                        .subscribe(sdkevent -> Log.d("Nodle", "SDKEvent: " + sdkevent.toString())));*/

        //locale defaults
        val configuration = getResources().getConfiguration();
        configuration.setLocale(getLocale());
        createConfigurationContext(configuration);
        dateFormat = new SimpleDateFormat("dd.MM.yyyy", getLocale());
        dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", getLocale());
        nullDate.set(1986, 1, 24); //nulldate
        soundGenerator = new SoundGenerator(this);

        val onlineSubs = ReactiveNetwork
                .observeNetworkConnectivity(this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectivity -> {
                    if (connectivity.state().equals(NetworkInfo.State.CONNECTED))
                        new Handler().post(() -> NetManager.checkNet(this));
                    else if (connectivity.state().equals(NetworkInfo.State.DISCONNECTED))
                        onNetChecked(false);
                }, Throwable::printStackTrace);
        compositeDisposable.add(onlineSubs);
    }

    protected void initializeLayout(View root, Toolbar toolbar, ContentLoadingProgressBar progBar, FloatingActionButton actionButton, DrawerLayout navigationDrawer, ImageButton rfidButton, ImageButton wifiButton) {
        setContentView(root);

        //window size
        var size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        width = size.x;
        height = size.y;

        //toolbar
        this.toolbar = toolbar;
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false); //hide title bar

        //prog
        this.progBar = progBar;
        progDialog = new ProgressDialog(this); //to be destroyed
        progDialog.setCancelable(false);

        //action button
        this.actionButton = actionButton;

        //rfid button
        this.rfidButton = rfidButton;
        rfidButton.setOnClickListener(v -> {
            if (rfidManager == null)
                Toast.makeText(this, R.string.rfid_device_model_is_not_selected, Toast.LENGTH_LONG).show();
            else if (rfidManager.isDeviceOnline()) {
                rfidManager.triggerButton();
                if (rfidManager.isScanning()) {
                    rfidListener.ReceiveData(IRfidDeviceListener.DataType.Trigger, "ON");
                    rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_500)));
                }
                else {
                    rfidListener.ReceiveData(IRfidDeviceListener.DataType.Trigger, "OFF");
                    rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
                }
            }
            else {
                val rfidModel = PreferenceManager.getDefaultSharedPreferences(this).getString("rfid_device_model", "");
                val rfidDevice = PreferenceManager.getDefaultSharedPreferences(this).getString("select_rfid_device", "");
                if ((!rfidDevice.equals("") || rfidModel.equals("HH53")) && rfidManager.connect(rfidDevice)) {
                    rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
                    rfidManager.setLastAntennaPower(PreferenceManager.getDefaultSharedPreferences(this).getInt("last_antenna_power", 100));
                }
            }
        });
        rfidButton.setOnLongClickListener(v -> {
            if (rfidManager == null)
                Toast.makeText(this, R.string.rfid_device_model_is_not_selected, Toast.LENGTH_LONG).show();
            else if (rfidManager.isDeviceOnline())
            {
                View viewInflated = LayoutInflater.from(this).inflate(R.layout.rfid_device_dialog, (ViewGroup) root, false);
                SeekBar antennaPower = viewInflated.findViewById(R.id.antenna_power);
                antennaPower.setProgress(rfidManager.getAntennaPower());
                NumberProgressBar batteryPerc = viewInflated.findViewById(R.id.battery_percentage);
                batteryPerc.setProgress(rfidManager.getBatteryPercentage());
                new AlertDialog.Builder(this)
                        .setView(viewInflated)
                        .setPositiveButton(R.string.confirm, (dialog, id) -> {
                            val power = antennaPower.getProgress();
                            PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("last_antenna_power", power).apply();
                            rfidManager.setLastAntennaPower(power);
                            rfidManager.setAntennaPower(power);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel_title, (dialog, id) -> {
                            dialog.dismiss();
                        })
                        .show();
            }
            return true;
        });

        //wifi button
        this.wifiButton = wifiButton;

        val rfidModel = PreferenceManager.getDefaultSharedPreferences(this).getString("rfid_device_model", "");
        val rfidDevice = PreferenceManager.getDefaultSharedPreferences(this).getString("select_rfid_device", "");
        if (!rfidModel.equals("")) {
            initializeRfid(rfidModel);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if ((!rfidDevice.equals("") || rfidModel.equals("HH53")) && rfidManager.connect(rfidDevice)) {
                    rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
                    rfidManager.setLastAntennaPower(PreferenceManager.getDefaultSharedPreferences(this).getInt("last_antenna_power", 100));
                }
            }, 1000);
        }

        val barcodeModel = PreferenceManager.getDefaultSharedPreferences(this).getString("barcode_device_model", "");
        if (!barcodeModel.equals(""))
            initializeBarcode(barcodeModel);

        //nav button hides keyboard
        navigationDrawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }
            @Override
            public void onDrawerOpened(@NonNull View drawerView) { }
            @Override
            public void onDrawerClosed(@NonNull View drawerView) { }
            @Override
            public void onDrawerStateChanged(int newState) {
                val inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null && getCurrentFocus() != null)
                    inputMethodManager.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
            }
        });
    }

    //----------------------------------------------------------------------------------------------
    // Public Functions
    //----------------------------------------------------------------------------------------------

    public void showProgBar() {
        progBar.show();
    }

    public void hideProgBar() {
        progBar.hide();
    }

    public void changeTitle(String title) {
        toolbar.setTitle(title);
    }

    public void hideKeyboard() {
        val view = getCurrentFocus();
        val imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void openBluetooth() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled())
            onBluetoothConnected(bluetoothAdapter.getBondedDevices());
        else {
            val enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    //----------------------------------------------------------------------------------------------
    // Triggered Events
    //----------------------------------------------------------------------------------------------

    public abstract void onNetChecked(Boolean result);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SPEECH:
                    if (getCurrentFragment() instanceof ISearchableListFragment && data != null) {
                        var query = data.getStringExtra(SearchManager.QUERY);
                        if (query != null) {
                            if (tryParseLong(query.replace(" ", "")) != null)
                                query = query.replace(" ", "");
                            ((ISearchableListFragment) getCurrentFragment()).setQuery(query.toUpperCase());
                        }
                    }
                    break;
                case DeviceListActivity.SELECT_DEVICE_REQUEST:
                    ((TslDeviceManager)rfidManager).onDeviceSelected(data);
                    break;
                case IntentIntegrator.REQUEST_CODE:
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    if (scanResult != null)
                        onBarcodeScanned(scanResult.getContents());
                    break;
                case REQUEST_ENABLE_BT:
                    onBluetoothConnected(BluetoothAdapter.getDefaultAdapter().getBondedDevices());
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    break;
            }
        }
    }

    public boolean controlRfid() {
        val rfidModel = PreferenceManager.getDefaultSharedPreferences(this).getString("rfid_device_model", "");
        val rfidDevice = PreferenceManager.getDefaultSharedPreferences(this).getString("select_rfid_device", "");
        if (rfidManager == null || rfidModel.equals("")) {
            //Toast.makeText(this, getString(R.string.rfid_device_model_is_not_selected), Toast.LENGTH_LONG).show();
            rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_500)));
            return false;
        }
        else if (rfidDevice.equals("") && !rfidModel.equals("HH53")) {
            initializeRfid(rfidModel);
            Toast.makeText(this, getString(R.string.rfid_device_must_be_paired), Toast.LENGTH_LONG).show();
            rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_500)));
            return false;
        }
        else if (!rfidManager.isDeviceOnline()) {
            if (rfidManager.connect(rfidDevice)) {
                rfidManager.setLastAntennaPower(PreferenceManager.getDefaultSharedPreferences(this).getInt("last_antenna_power", 100));
                rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
                return true;
            }
            else {
                Toast.makeText(this, getString(R.string.cannot_reach_rfid_device), Toast.LENGTH_LONG).show();
                rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_500)));
                return false;
            }
        }
        else {
            rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            return true;
        }
    }

    public void initializeBarcode(String model) {
        barcodeManager = null;
        switch (model) {
            case "KAMERA":
                actionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_qrcode));
                actionButton.setOnClickListener(v -> new IntentIntegrator(this).initiateScan()); //set fab as camera qrcode reader
                break;
            case "HL7202K8":
                actionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.rfid_signal_48px));
                actionButton.setOnClickListener(v -> rfidManager.triggerButton());
                break;
            case "MIO":
                actionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_qrcode));
                barcodeManager = new MountedBarcodeReaderManager(this); //mounted scanner
                actionButton.setOnClickListener(v -> barcodeManager.scan(this));
                break;
            case "HH53":
                actionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_qrcode));
                actionButton.setOnClickListener(v -> rfidManager.scanBarcode());
                break;
        }
    }

    public void initializeRfid(String model) {
        switch (model) {
            case "HL7202K8":
                rfidManager = new HopelandDeviceManager(this, rfidListener);
                break;
            case "HH53":
                rfidManager = new NurDeviceManager(this, rfidListener);
                break;
            case "1128":
                rfidManager = new TslDeviceManager(this, rfidListener);
                break;
        }
    }

    public IRfidDeviceListener rfidListener = new IRfidDeviceListener() {
        @Override
        public void ReceiveData(DataType type, String data) {
            switch (type) {
                case RFID:
                    Log.d("rfid_epc", data);
                    onRfidScanned(data);
                    //soundGenerator.success();
                    break;
                case Barcode:
                    Log.d("rfid_barcode", data);
                    onBarcodeScanned(data);
                    break;
                case Proximity:
                    Log.d("rfid_range", data);
                    val range = TryParsers.tryParseInt(data);
                    soundGenerator.setRate(range);
                    onRangeScanned(range);
                    break;
                case Trigger:
                    Log.d("rfid_gpi", data);
                    if (data.equals("ON")) {
                        if (rfidManager.getMode() == IRfidDeviceManager.OperationType.TagFinder)
                            soundGenerator.playLoop();
                    }
                    else if (data.equals("OFF")) {
                        if (rfidManager.getMode() == IRfidDeviceManager.OperationType.TagFinder)
                            soundGenerator.stopLoop();
                        onReaderStopped();
                    }
                    break;
                case Info:
                    Log.d("rfid_info", data);
                    break;
                case Error:
                    Log.d("rfid_error", data);
                    break;
            }
        }

        @Override
        public void onConnectionStateChanged(ConnectionState state, String connectedDeviceName) {
            if (state == ConnectionState.Connecting) {
                Log.d("rfid_connection", "Connecting to: " + connectedDeviceName);
                rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivityBase.this, R.color.yellow_500)));
            }
            else if (state == ConnectionState.Connected) {
                Log.d("rfid_connection", "Connected to: " + connectedDeviceName);
                rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivityBase.this, R.color.white)));
            }
            else if (state == ConnectionState.Disconnected) {
                Log.d("rfid_connection", "Disconnected from: " + connectedDeviceName);
                rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivityBase.this, R.color.red_500)));
            }
        }
    };

    private void onReaderStopped() {
        if (getCurrentFragment() instanceof ICanScanRfid)
            ((ICanScanRfid)getCurrentFragment()).onReaderStopped();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null)
                ((ISearchableFragment) getCurrentFragment()).setQuery(query.toUpperCase());
        }
    }

    public abstract void onRfidScanned(String code);

    public abstract void onBarcodeScanned(String code);

    public abstract void onBluetoothConnected(Set<BluetoothDevice> pairedDevices);

    public void onRangeScanned(int range) {
        if (tfDialog != null)
            tfDialog.onRangeScanned(range);
        else if (getCurrentFragment() instanceof ICanScanRange)
            ((ICanScanRange)getCurrentFragment()).onRangeScanned(range);
    }

    //----------------------------------------------------------------------------------------------
    // Lifecycle Events
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rfidManager != null)
            rfidManager.onDestroy();
        if (onlineSubscription != null)
            onlineSubscription.cancel();
        compositeDisposable.dispose();
    }

    //----------------------------------------------------------------------------------------------
    // Misc
    //----------------------------------------------------------------------------------------------

    protected void checkPermissions() {
        final String[] permissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.VIBRATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA
        };
        val listPermissionsNeeded = new ArrayList<String>();
        for (val p : permissions) {
            int result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(p);
        }
        if (!listPermissionsNeeded.isEmpty())
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 100);
    }

    protected void setupFolders() {
        val rootFolder = getExternalFilesDir(null);
        if (rootFolder == null)
            return;
        if (!rootFolder.exists())
            //noinspection ResultOfMethodCallIgnored
            rootFolder.mkdirs();
        val photosFolder = new File(rootFolder, getString(R.string.photos_folder_name));
        if (!photosFolder.exists())
            //noinspection ResultOfMethodCallIgnored
            photosFolder.mkdirs();
        assetPhotosFolder = new File(photosFolder, getString(R.string.asset_photos_folder_name));
        if (!assetPhotosFolder.exists())
            //noinspection ResultOfMethodCallIgnored
            assetPhotosFolder.mkdirs();
        personPhotosFolder = new File(photosFolder, getString(R.string.person_photos_folder_name));
        if (!personPhotosFolder.exists())
            //noinspection ResultOfMethodCallIgnored
            personPhotosFolder.mkdirs();
        templatesFolder = new File(rootFolder, getString(R.string.templates_folder_name));
        if (!templatesFolder.exists())
            //noinspection ResultOfMethodCallIgnored
            templatesFolder.mkdirs();
    }

    protected Fragment getCurrentFragment() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        val navHost = getSupportFragmentManager().getFragments().get(count > 0 ? count - 1 : count);
        return navHost.getChildFragmentManager().getFragments().get(0);
    }

    public Locale getLocale() {
        return new Locale("tr", "TR");
    }
}
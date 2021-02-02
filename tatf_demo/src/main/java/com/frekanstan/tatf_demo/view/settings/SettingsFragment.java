package com.frekanstan.tatf_demo.view.settings;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.labeling.LabelPrinter;
import com.frekanstan.tatf_demo.view.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.val;

public class SettingsFragment extends PreferenceFragmentCompat {

    private MainActivity context;

    public SettingsFragment() { }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity){
            this.context = (MainActivity) context;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        //initialize prefs
        EditTextPreference kurumAdi = findPreference("company_name_prefs");
        if (kurumAdi != null) {
            kurumAdi.setOnBindEditTextListener(editText -> { //company name edittext init
                editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                editText.setSingleLine(true);
            });
        }
        EditTextPreference birlikAdi = findPreference("main_company_name_prefs");
        if (birlikAdi != null) {
            birlikAdi.setOnBindEditTextListener(editText -> { //main company name edittext init
                editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                editText.setSingleLine(true);
            });
        }
        ListPreference printerModelPrefs = findPreference("printer_model_prefs");
        val prefs = PreferenceManager.getDefaultSharedPreferences(context);
        refreshPrinterSettings(prefs.getString("printer_model_prefs", ""), prefs.getString("label_size_prefs", ""));
        if (printerModelPrefs != null) {
            printerModelPrefs.setOnPreferenceChangeListener((preference, newValue) -> //choosing printer model refreshes sizes
                refreshPrinterSettings(newValue.toString(), prefs.getString("label_size_prefs", "")));
        }
        ListPreference labelSizePrefs = findPreference("label_size_prefs");
        if (labelSizePrefs != null) {
            labelSizePrefs.setOnPreferenceChangeListener((preference, newValue) -> //choosing size refreshes formats
                refreshPrinterSettings(prefs.getString("printer_model_prefs", ""), newValue.toString()));
        }
        Preference labelPreview = findPreference("label_preview");
        if (labelPreview != null) {
            labelPreview.setOnPreferenceClickListener(preference -> {
                val settingsDialog = new Dialog(context);
                settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                settingsDialog.setContentView(View.inflate(context, R.layout.image_layout, null));
                val image = (ImageView)settingsDialog.getWindow().findViewById(R.id.preview_image);
                image.setImageBitmap(LabelPrinter.infoOverlay(AssetDAO.getDao().getRandom(), context));
                settingsDialog.show();
                return false;
            });
        }
        ListPreference barcodeModel = findPreference("barcode_device_model");
        if (barcodeModel != null) {
            if (barcodeModel.getValue() == null)
                barcodeModel.setValue("KAMERA");
            barcodeModel.setOnPreferenceChangeListener((preference, newValue) -> {
                context.initializeBarcode(newValue.toString());
                return true;
            });
        }
        ListPreference rfidModel = findPreference("rfid_device_model");
        if (rfidModel != null) {
            if (rfidModel.getValue() != null && !rfidModel.getValue().equals("HH53"))
                context.openBluetooth();
            rfidModel.setOnPreferenceChangeListener((preference, newValue) -> {
                context.initializeRfid(newValue.toString());
                if (!newValue.toString().equals("HH53"))
                    context.openBluetooth();
                return true;
            });
        }
    }

    public void populateBluetoothDevices(Set<BluetoothDevice> pairedDevices) {
        ListPreference selectRfidDevice = findPreference("select_rfid_device");
        if (selectRfidDevice != null) {
            selectRfidDevice.setVisible(true);
            List<String> deviceNames = new ArrayList<>();
            for (val device : pairedDevices)
                deviceNames.add(device.getName());
            selectRfidDevice.setEntries(deviceNames.toArray(new CharSequence[0]));
            selectRfidDevice.setEntryValues(deviceNames.toArray(new CharSequence[0]));
            selectRfidDevice.setOnPreferenceChangeListener((preference, newValue) -> {
                if (context.rfidManager.connect(newValue.toString())) {
                    context.rfidButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
                    context.rfidManager.setLastAntennaPower(PreferenceManager.getDefaultSharedPreferences(context).getInt("last_antenna_power", 100));
                }
                //context.controlRfid();
                return true;
            });
        }
    }

    private boolean refreshPrinterSettings(String printerName, String labelSize) {
        if (printerName.isEmpty())
            return false;
        ListPreference labelSizePrefs = findPreference("label_size_prefs");
        ListPreference labelTypePrefs = findPreference("label_type_prefs");
        if (labelSizePrefs != null) {
            if (printerName.equals("PT_E550W") || printerName.equals("PT_P750W")) {
                    labelSizePrefs.setEntries(getResources().getStringArray(R.array.label_size_list_mobil));
                labelSizePrefs.setEntryValues(getResources().getStringArray(R.array.label_size_values_mobil));
            }
            else if (printerName.equals("PT_P900W") || printerName.equals("PT_P950NW")) {
                labelSizePrefs.setEntries(getResources().getStringArray(R.array.label_size_list_standart));
                labelSizePrefs.setEntryValues(getResources().getStringArray(R.array.label_size_values_standart));
            }
        }
        if (labelTypePrefs != null) {
            switch (printerName) {
                case "PT_E550W":
                case "PT_P750W":
                    switch (labelSize) {
                        case "W18":
                            labelTypePrefs.setEntries(getResources().getStringArray(R.array.label_type_list_18_mobil));
                            labelTypePrefs.setEntryValues(getResources().getStringArray(R.array.label_type_values_18_mobil));
                            break;
                        case "W24":
                            labelTypePrefs.setEntries(getResources().getStringArray(R.array.label_type_list_24_mobil));
                            labelTypePrefs.setEntryValues(getResources().getStringArray(R.array.label_type_values_24_mobil));
                            break;
                    }
                    break;
                case "PT_P900W":
                case "PT_P950NW":
                    switch (labelSize) {
                        case "W18":
                            labelTypePrefs.setEntries(getResources().getStringArray(R.array.label_type_list_18_standart));
                            labelTypePrefs.setEntryValues(getResources().getStringArray(R.array.label_type_values_18_standart));
                            break;
                        case "W24":
                            labelTypePrefs.setEntries(getResources().getStringArray(R.array.label_type_list_24_standart));
                            labelTypePrefs.setEntryValues(getResources().getStringArray(R.array.label_type_values_24_standart));
                            break;
                        case "W36":
                            labelTypePrefs.setEntries(getResources().getStringArray(R.array.label_type_list_36_standart));
                            labelTypePrefs.setEntryValues(getResources().getStringArray(R.array.label_type_values_36_standart));
                            break;
                    }
                    break;
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        context.actionButton.hide();
        context.showHideFooter(true);
    }
}
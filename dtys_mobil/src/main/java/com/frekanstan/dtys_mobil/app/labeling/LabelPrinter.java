package com.frekanstan.dtys_mobil.app.labeling;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.paging.PagedListAdapter;
import androidx.preference.PreferenceManager;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.helpers.WifiSwitcher;
import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.app.assets.AssetDAO;
import com.frekanstan.dtys_mobil.app.locations.LocationDAO;
import com.frekanstan.dtys_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.dtys_mobil.app.people.PersonDAO;
import com.frekanstan.dtys_mobil.data.Asset;
import com.frekanstan.dtys_mobil.view.MainActivity;
import com.google.common.base.Strings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Objects;

import lombok.SneakyThrows;
import lombok.val;
import lombok.var;

import static android.content.Context.WIFI_SERVICE;

public class LabelPrinter {
    private MainActivity mContext;
    private static ArrayList<Long> mIdList;
    private static Boolean mCancelAction = false;
    private static String mType;
    private static PagedListAdapter mAdapter;
    private static String wifiSSID = "";

    public LabelPrinter(MainActivity context, ArrayList<Long> idList, String type, PagedListAdapter adapter) {
        mIdList = idList;
        mContext = context;
        mType = type;
        mAdapter = adapter;
    }

    public void print() {
        val wifiManager = ((WifiManager)mContext.getApplicationContext()
                .getSystemService(WIFI_SERVICE));
        if (wifiManager == null || !wifiManager.isWifiEnabled())
        {
            new AlertDialog.Builder(mContext)
                    .setMessage(R.string.wifi_is_disabled_dialog_message)
                    .setPositiveButton(mContext.getString(R.string.wireless_settings),
                            (dialog1, id) -> mContext.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                    .setNegativeButton(mContext.getString(R.string.cancel_title),
                            (dialog12, id) -> dialog12.dismiss())
                    .show();
        }
        else
            new Print(mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*public void preview() {
        var asset = AssetDAO.getBox().query().build().findFirst();
        var labelSize = prefs.getString("label_size_prefs", "");
        var labelType = prefs.getString("label_type_prefs", "");
        var template = BitmapFactory.decodeFile(templatesFolder + File.separator
                + labelSize.substring(1) + "mm_" + labelType + ".bmp");
        var label = infoOverlay(template, asset, labelSize, labelType);
        if (labelSize.equals("W18")) {
            var qrBitmap = qrCreator(mContext.getString(R.string.barcode_prefix) +
                    asset.getRemoteId(), 72);
            label = qrOverlay(label, qrBitmap, labelSize, labelType);
        } else if (labelSize.equals("W24") || labelSize.equals("W36")) {
            switch (labelType) {
                case "A_0":
                case "B_0":
                    label = qrOverlay(label,
                                qrCreator(mContext.getString(R.string.barcode_prefix) + asset.getRemoteId(), 92),
                            labelSize, labelType);
                    break;
                case "K_0":
                    label = qrOverlay(label,
                                qrCreator(mContext.getString(R.string.barcode_prefix) + asset.getRemoteId(), 120),
                            labelSize, labelType);
                    break;
                case "K_1":
                    label = qrOverlay(label,
                            qrCreator(mContext.getString(R.string.barcode_prefix) + asset.getRemoteId(), 110),
                            labelSize, labelType);
                    break;
            }
        }
        pngSaver(label, rootFolder + "/etiket.png");
    }*/

    private static class Print extends AsyncTask<Void, Integer, LPResult> {
        private static WeakReference<MainActivity> context;
        private Printer mPrinter;
        private static PrinterInfo mPrintSettings;
        private static String printerName;
        PrinterStatus mPrintResult;
        private static SharedPreferences pref;
        private Dialog mProgDialog;

        Print(MainActivity activity) {
            context = new WeakReference<>(activity);
            pref = PreferenceManager.getDefaultSharedPreferences(context.get());
        }

        @SneakyThrows
        @Override
        protected LPResult doInBackground(Void... params) {
            publishProgress(0);
            var result = connectToPrinterWifi();
            if (result != LPResult.SUCCESS)
                return result;
            result = pairWithPrinter();
            if (result != LPResult.SUCCESS)
                return result;
            result = loadPrinterSettings();
            if (result != LPResult.SUCCESS)
                return result;
            if (mCancelAction)
                return LPResult.CANCELED;
            if (!mPrinter.startCommunication())
                return LPResult.ERROR_CANNOT_FIND_PRINTER;
            switch (mType) {
                case "asset":
                    result = printAssetLabel();
                    if (result != LPResult.SUCCESS)
                        return result;
                    break;
                case "person": {
                    result = printPersonLabel();
                    if (result != null)
                        return result;
                    break;
                }
                case "location": {
                    result = printLocationLabel();
                    if (result != null)
                        return result;
                    break;
                }
            }
            mPrinter.endCommunication();
            return LPResult.SUCCESS;
        }

        private LPResult connectToPrinterWifi() {
            val mPrinterModel = PreferenceManager.getDefaultSharedPreferences(context.get()).getString("printer_model_prefs", "");
            if (Strings.isNullOrEmpty(mPrinterModel))
                return LPResult.ERROR_NO_PRINTER_SELECTED;
            printerName = mPrinterModel.replace("_", "-");
            val wifiManager = ((WifiManager)context.get().getApplicationContext().getSystemService(WIFI_SERVICE));
            assert wifiManager != null;
            val currSSID = wifiManager.getConnectionInfo().getSSID();
            if (currSSID.contains(printerName))
                return LPResult.SUCCESS;
            if (NetManager.isOnline)
                wifiSSID = currSSID;
            val printerWifiConfig = NetManager.findKnownWifiConfig(wifiManager, printerName);
            if (printerWifiConfig == null)
                return LPResult.ERROR_CANNOT_FIND_PRINTER;
            if (wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId())) {
                if (wifiManager.enableNetwork(printerWifiConfig.networkId, true)) {
                    if (wifiManager.reconnect())
                        return LPResult.SUCCESS;
                    else
                        return LPResult.ERROR_CANNOT_FIND_PRINTER;
                }
                else
                    return LPResult.ERROR_CANNOT_FIND_PRINTER;
            }
            else
                return LPResult.ERROR_CANNOT_FIND_PRINTER;
        }

        private LPResult pairWithPrinter() {
            mPrinter = new Printer();
            NetPrinter[] netPrinters = mPrinter.getNetPrinters(printerName);
            for (int i = 0; i < 5; i++) {
                if (netPrinters.length == 0) {
                    if (i == 2)
                        return LPResult.ERROR_CANNOT_FIND_PRINTER;
                    else {
                        try {
                            Thread.sleep(1000);
                            if (mCancelAction)
                                return LPResult.CANCELED;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return LPResult.ERROR_CANNOT_FIND_PRINTER;
                        }
                        netPrinters = mPrinter.getNetPrinters(printerName);
                    }
                }
                else
                    break;
            }
            mPrintSettings = mPrinter.getPrinterInfo();
            mPrintSettings.printerModel = PrinterInfo.Model.valueOf(printerName.replace("-", "_"));
            mPrintSettings.port = PrinterInfo.Port.NET;
            mPrintSettings.ipAddress = netPrinters[0].ipAddress;
            mPrintSettings.macAddress = netPrinters[0].macAddress;
            return LPResult.SUCCESS;
        }

        private LPResult loadPrinterSettings() {
            val labelSize = pref.getString("label_size_prefs", "");
            if (labelSize.equals(""))
                return LPResult.ERROR_NO_LABEL_SELECTED;
            mPrintSettings.labelNameIndex = LabelInfo.PT.valueOf(labelSize).ordinal();
            val labelCut = pref.getString("label_cut_prefs", "");
            mPrintSettings.isAutoCut = labelCut.equals("TEK");
            mPrintSettings.isCutAtEnd = labelCut.equals("TEK");
            mPrintSettings.isHalfCut = true;
            mPrintSettings.orientation = PrinterInfo.Orientation.LANDSCAPE;
            mPrintSettings.printQuality = PrinterInfo.PrintQuality.HIGH_RESOLUTION;
            mPrintSettings.halftone = PrinterInfo.Halftone.THRESHOLD;
            mPrintSettings.workPath = MainActivity.templatesFolder.getAbsolutePath();
            mPrinter.setPrinterInfo(mPrintSettings);
            return LPResult.SUCCESS;
        }

        private LPResult printAssetLabel() {
            val labelTypeKey = Integer.parseInt(pref.getString("label_type_prefs", "-1"));
            if (labelTypeKey == -1)
                return LPResult.ERROR_NO_LABEL_SELECTED;
            for (Long id : mIdList) {
                publishProgress(1, mIdList.indexOf(id), mIdList.size());
                var asset = AssetDAO.getDao().get(id);
                //pngSaver(infoOverlay(asset, context.get()), MainActivity.assetPhotosFolder + File.separator + "a.png");
                mPrintResult = mPrinter.printImage(infoOverlay(asset, context.get()));
                if (mPrintResult.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) { //if error
                    mPrinter.endCommunication();
                    return LPResult.ERROR_BROTHER_ERROR;
                }
                if (mCancelAction) { //if canceled
                    mPrinter.endCommunication();
                    return LPResult.CANCELED;
                }
            }
            return LPResult.SUCCESS;
        }

        private LPResult printPersonLabel() {
            for (Long id : mIdList) {
                var person = PersonDAO.getDao().get(id);
                val qrBitmap = qrCreator(context.get().getString(R.string.person_qr_prefix) +
                        person.getPersonCode(), 120);
                if (qrBitmap == null) return LPResult.CANCELED;
                val label = Bitmap.createBitmap(qrBitmap.getWidth(), qrBitmap.getHeight(), qrBitmap.getConfig());
                val canvas = new Canvas(label);
                canvas.drawBitmap(qrBitmap, new Matrix(), null);
                mPrintResult = mPrinter.printImage(label);

                if (mPrintResult.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                    mPrinter.endCommunication();
                    return LPResult.ERROR_BROTHER_ERROR;
                }
                if (mCancelAction) {
                    mPrinter.endCommunication();
                    return LPResult.CANCELED;
                }
            }
            return null;
        }

        private LPResult printLocationLabel() {
            for (Long id : mIdList) {
                var location = LocationDAO.getDao().get(id);
                val qrBitmap = qrCreator(context.get().getString(R.string.location_qr_prefix) +
                        location.getLocationCode(), 120);
                if (qrBitmap == null) return LPResult.CANCELED;
                val label = Bitmap.createBitmap(qrBitmap.getWidth(), qrBitmap.getHeight(), qrBitmap.getConfig());
                val canvas = new Canvas(label);
                canvas.drawBitmap(qrBitmap, new Matrix(), null);
                mPrintResult = mPrinter.printImage(label);

                if (mPrintResult.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                    mPrinter.endCommunication();
                    return LPResult.ERROR_BROTHER_ERROR;
                }
                if (mCancelAction) {
                    mPrinter.endCommunication();
                    return LPResult.CANCELED;
                }
            }
            return null;
        }

        private static void pngSaver(Bitmap bmp, String path) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(path);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            int state = progress[0];
            if (state == 0) {
                mProgDialog = new Dialog(context.get());
                mProgDialog.setContentView(R.layout.progress_dialog_indeterminate);
                final ProgressBar progBar = mProgDialog.findViewById(R.id.progress);
                progBar.setIndeterminate(true);
                final TextView title = mProgDialog.findViewById(R.id.alertTitle);
                title.setText(context.get().getString(R.string.wait_reaching_printer));
                final Button cancelButton = mProgDialog.findViewById(R.id.cancelButton);
                cancelButton.setOnClickListener(v -> {
                    cancelButton.setEnabled(false);
                    title.setText(context.get().getString(R.string.wait_canceling));
                    mCancelAction = true;
                });
                mProgDialog.show();
            } else if (state == 1) {
                TextView title = mProgDialog.findViewById(R.id.alertTitle);
                title.setText(String.format(context.get().getString(R.string.wait_printing_labels),
                        (progress[1] + 1), progress[2]));
            }
        }

        private void markAsLabeled() {
            val assetDao = AssetDAO.getDao();
            for (val assetId : mIdList)
                assetDao.setLabeledStateChange(assetId, true);
            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();
        }

        private void markLocationAsLabeled() {
            val locationDao = LocationDAO.getDao();
            for (val locationId : mIdList) {
                var location = LocationDAO.getDao().get(locationId);
                location.setAsLabeled(true);
                locationDao.put(location);
                locationDao.setLabeledStateChange(location.getId(), location.getLabelingDateTime());
            }
            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();
            /*if (mIdList.size() == 1 && context.get().rfidManager != null && context.get().rfidManager.isDeviceOnline())
                context.get().onBarcodeScanned();*/
        }

        private void markPersonAsLabeled() {
            val personDao = PersonDAO.getDao();
            for (val personId : mIdList) {
                var person = PersonDAO.getDao().get(personId);
                person.setAsLabeled(true);
                personDao.put(person);
                personDao.setLabeledStateChange(person.getId(), person.getLabelingDateTime());
            }
            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(LPResult result) {
            mProgDialog.hide();
            switch (result) {
                case SUCCESS:
                    if (!Strings.isNullOrEmpty(wifiSSID)) {
                        val wifiManager = ((WifiManager)context.get().getApplicationContext()
                                .getSystemService(WIFI_SERVICE));
                        new WifiSwitcher(wifiManager, new WifiSwitcher.WifiSwitcedListener() {
                            @Override
                            public void onFail() {
                                Log.d("WifiSwitcher","Could not connect to old wifi");
                            }
                            @Override
                            public void onSuccess() {
                                Log.d("WifiSwitcher","Connected to old wifi");
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wifiSSID);
                    }
                    switch (mType) {
                        case "asset":
                            new AlertDialog.Builder(context.get())
                                    .setMessage(R.string.selected_assets_will_be_marked_as_labeled)
                                    .setPositiveButton(context.get().getString(R.string.yes), (dialog, id) ->
                                            markAsLabeled())
                                    .setNegativeButton(context.get().getString(R.string.no), (dialog, id) -> dialog.dismiss())
                                    .show();
                            break;
                        case "location":
                            new AlertDialog.Builder(context.get())
                                    .setMessage(R.string.selected_locations_will_be_marked_as_labeled)
                                    .setPositiveButton(context.get().getString(R.string.yes), (dialog, id) ->
                                            markLocationAsLabeled())
                                    .setNegativeButton(context.get().getString(R.string.no), (dialog, id) -> dialog.dismiss())
                                    .show();
                            break;
                        case "person":
                            new AlertDialog.Builder(context.get())
                                    .setMessage(R.string.selected_persons_will_be_marked_as_labeled)
                                    .setPositiveButton(context.get().getString(R.string.yes), (dialog, id) ->
                                            markPersonAsLabeled())
                                    .setNegativeButton(context.get().getString(R.string.no), (dialog, id) -> dialog.dismiss())
                                    .show();
                            break;
                    }
                    break;
                case CANCELED:
                    mCancelAction = false;
                    break;
                case ERROR_CANNOT_FIND_PRINTER:
                    new AlertDialog.Builder(context.get())
                            .setMessage(R.string.cannot_find_printer_dialog_message)
                            .setPositiveButton(context.get().getString(R.string.wireless_settings),
                                    (dialog, id) -> context.get().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                            .setNegativeButton(context.get().getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                            .show();
                    break;
                case ERROR_NO_PRINTER_SELECTED:
                    new AlertDialog.Builder(context.get())
                            .setMessage(R.string.no_printer_selected_dialog_message)
                            .setPositiveButton(context.get().getString(R.string.goto_preferences),
                                    (dialog, id) -> context.get().nav.navigate(R.id.settingsFragment))
                            .setNegativeButton(context.get().getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                            .show();
                    break;
                case ERROR_NO_LABEL_SELECTED:
                    new AlertDialog.Builder(context.get())
                            .setMessage(R.string.no_label_selected_dialog_message)
                            .setPositiveButton(context.get().getString(R.string.goto_preferences),
                                    (dialog, id) -> context.get().nav.navigate(R.id.settingsFragment))
                            .setNegativeButton(context.get().getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                            .show();
                    break;
                case ERROR_BROTHER_ERROR:
                    String eMessage = "";
                    switch (mPrintResult.errorCode) {
                        case ERROR_NONE:
                            break;
                        case ERROR_NOT_SAME_MODEL:
                            eMessage = "Seçilen yazıcı modeli hatalı gözüküyor";
                            break;
                        case ERROR_BROTHER_PRINTER_NOT_FOUND:
                            eMessage = "Yazıcı tespit edilemedi";
                            break;
                        case ERROR_PAPER_EMPTY:
                            eMessage = "Yetersiz kartuş";
                            break;
                        case ERROR_BATTERY_EMPTY:
                            eMessage = "Yazıcının şarjı yetersiz";
                            break;
                        case ERROR_COMMUNICATION_ERROR:
                            eMessage = "Yazıcıya bağlantı sağlanamadı";
                            break;
                        case ERROR_OVERHEAT:
                            eMessage = "Yazıcınız çok ısınmış. Bir süre bekleyin";
                            break;
                        case ERROR_PAPER_JAM:
                            eMessage = "Kartuş sıkışmış olabilir. Kontrol ediniz";
                            break;
                        case ERROR_HIGH_VOLTAGE_ADAPTER:
                            eMessage = "Yazıcıya yüksek voltajlı adaptör bağlanmış";
                            break;
                        case ERROR_CHANGE_CASSETTE:
                            eMessage = "Yazıcıdaki kasedi değiştirin";
                            break;
                        case ERROR_FEED_OR_CASSETTE_EMPTY:
                            eMessage = "Etiket sıkıştı ya da kaset bitmiş. Kontrol edin";
                            break;
                        case ERROR_NO_CASSETTE:
                            eMessage = "Yazıcıda kaset takılı değil";
                            break;
                        case ERROR_WRONG_CASSETTE_DIRECT:
                            eMessage = "Yazıcıya hatalı kaset takılmış";
                            break;
                        case ERROR_SYSTEM_ERROR:
                        case ERROR_CREATE_SOCKET_FAILED:
                        case ERROR_CONNECT_SOCKET_FAILED:
                        case ERROR_GET_OUTPUT_STREAM_FAILED:
                        case ERROR_GET_INPUT_STREAM_FAILED:
                        case ERROR_CLOSE_SOCKET_FAILED:
                        case ERROR_FILE_NOT_SUPPORTED:
                        case ERROR_EVALUATION_TIMEUP:
                        case ERROR_WRONG_CUSTOM_INFO:
                        case ERROR_NO_ADDRESS:
                        case ERROR_NOT_MATCH_ADDRESS:
                        case ERROR_FILE_NOT_FOUND:
                        case ERROR_TEMPLATE_FILE_NOT_MATCH_MODEL:
                        case ERROR_TEMPLATE_NOT_TRANS_MODEL:
                        case ERROR_PORT_NOT_SUPPORTED:
                        case ERROR_WRONG_TEMPLATE_KEY:
                        case ERROR_TEMPLATE_NOT_PRINT_MODEL:
                        case ERROR_PRINTER_SETTING_NOT_SUPPORTED:
                        case ERROR_INVALID_PARAMETER:
                        case ERROR_INTERNAL_ERROR:
                        case ERROR_TEMPLATE_NOT_CONTROL_MODEL:
                        case ERROR_TEMPLATE_NOT_EXIST:
                        case ERROR_BUFFER_FULL:
                        case ERROR_TUBE_EMPTY:
                        case ERROR_UPDATE_FRIM_NOT_SUPPORTED:
                        case ERROR_OS_VERSION_NOT_SUPPORTED:
                        case ERROR_RESOLUTION_MODE:
                        case ERROR_POWER_CABLE_UNPLUGGING:
                        case ERROR_UNSUPPORTED_MEDIA:
                        case ERROR_TUBE_CUTTER:
                        case ERROR_UNSUPPORTED_TWO_COLOR:
                        case ERROR_UNSUPPORTED_MONO_COLOR:
                        case ERROR_MINIMUM_LENGTH_LIMIT:
                            eMessage = "Yazıcı sistem hatası veriyor";
                            break;
                        case ERROR_OUT_OF_MEMORY:
                            eMessage = "Yazıcı hafıza hatası veriyor";
                            break;
                        case ERROR_SET_OVER_MARGIN:
                            eMessage = "Etiket taşmış";
                            break;
                        case ERROR_NO_SD_CARD:
                            eMessage = "Yazıcı sd kart hatası veriyor";
                            break;
                        case ERROR_COVER_OPEN:
                            eMessage = "Yazıcının kapağı açık. Kontrol edin";
                            break;
                        case ERROR_WRONG_LABEL:
                            eMessage = "Hatalı kaset takılmış. Kontrol edin";
                            break;
                        case ERROR_BUSY:
                            eMessage = "Yazıcı meşgul";
                            break;
                        case ERROR_CANCEL:
                            eMessage = "İşlem yazıcı tarafından iptal edildi";
                            break;
                        case ERROR_TUBE_RIBBON_EMPTY:
                            eMessage = "Kartuş bitmiş. Kontrol edin";
                            break;
                        case ERROR_BATTERY_TROUBLE:
                            eMessage = "Yazıcının şarjında sıkıntı var. Kontrol edin";
                            break;
                    }
                    Toast.makeText(context.get(), eMessage, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    /*private static void infoOverlay(Asset asset, Printer label) {
        val l = Integer.parseInt(pref.getString("label_type_prefs", ""));
        label.replaceTextName("https://sbu2.saglik.gov.tr/QR/QR.aspx?kno=" + asset.getRemoteId(), "qr");
        label.replaceTextName(String.valueOf(asset.getRemoteId()), "kunyeNo");
        if (l == 1 || l == 2 || l == 3 || l == 4 || l == 5 || l == 17 || l == 6 || l == 7 || l == 8 || l == 9 || l == 10 || l == 11 || l == 12 || l == 13 || l == 14) {
            String bt = " ";
            if (asset.getBudgetBelongsTo() == EBudgetType.GeneralBudget)
                bt = "GB";
            else if (asset.getBudgetBelongsTo() == EBudgetType.RevolvingFunds)
                bt = "DS";
            else if (asset.getBudgetBelongsTo() == EBudgetType.PrivateBudget)
                bt = "OB";
            else if (asset.getBudgetBelongsTo() == EBudgetType.ForeignBudget)
                bt = "YB";
            label.replaceTextName(bt, "butceTuru");
        }
        if (l == 1 || l == 4 || l == 5 || l == 17 || l == 6 || l == 7 || l == 8 || l == 9 || l == 11 || l == 12 || l == 13) {
            String kurumAdi = pref.getString("company_name_prefs", TenantRepository.getCurrentTenant().getName());
            label.replaceTextName(makePrintable(kurumAdi, null), "kurumAdi");
        }
        if (l == 1 || l == 2 || l == 4 || l == 5 || l == 17 || l == 6 || l == 7 || l == 8 || l == 9 || l == 11 || l == 12 || l == 13) {
            label.replaceTextName(asset.getRegistrationCode(), "sicilNo");
        }
        if (l == 4 || l == 5 || l == 17 || l == 6 || l == 7 || l == 8 || l == 9 || l == 11 || l == 12 || l == 13) {
            String birlikAdi = pref.getString("main_company_name_prefs", "T.C. SAGLIK BAKANLIGI");
            label.replaceTextName(makePrintable(birlikAdi, null), "birlikAdi");
        }
        String tasinirAdi = asset.getAssetType().getDefinition();
        if (l == 1 || l == 16) {
            label.replaceTextName(makePrintable(tasinirAdi, 30), "tasinirAdi");
        }
        if (l == 4 || l == 5 || l == 17 || l == 6 || l == 7 || l == 8 || l == 9 || l == 11 || l == 12) {
            if (asset.getFeatures() == null)
                label.replaceTextName(makePrintable(tasinirAdi, 40), "tasinirTanimi");
            else {
                val tasinirTanimi = tasinirAdi + ", " + asset.getFeatures();
                label.replaceTextName(makePrintable(tasinirTanimi, 40), "tasinirTanimi");
            }
        }
        if (l == 1 || l == 2 || l == 3 || l == 5 || l == 17 || l == 6 || l == 7 || l == 8 || l == 9 || l == 10 || l == 11 || l == 12 || l == 13 || l == 14) {
            if (asset.getAcquisitionYear() != null)
                label.replaceTextName(String.valueOf(asset.getAcquisitionYear()), "edinmeYili");
            else
                label.replaceTextName(" ", "edinmeYili");
        }
        if (l == 2 || l == 5 || l == 17 || l == 6 || l == 7 || l == 8 || l == 9 || l == 12 || l == 13) {
            label.replaceTextName(makePrintable(asset.getBrand(), 16), "marka");
            label.replaceTextName(makePrintable(asset.getModel(), 16), "model");
            label.replaceTextName(makePrintable(asset.getSerialNo(), 16), "seriNo");
        }
        if (l == 2 || l == 7 || l == 8 || l == 9 || l == 13)
            label.replaceTextName(makePrintable(asset.getBiomedicalType(), 30), "biyomedikalTur");
        if (l == -1)
            label.replaceTextName(makePrintable(asset.getBiomedicalDefinition(), 20), "biyomedikalTanim");
        if (l == -1)
            label.replaceTextName(makePrintable(asset.getBiomedicalBranch(), 20), "biyomedikalBrans");
        if (l == 8)
            label.replaceTextName(makePrintable(asset.getPlaceOfUse(), 20), "kullanimAlani");
        if (l == -1) {
            if (asset.getAssignedPersonId() != 0)
                label.replaceTextName(makePrintable(asset.getAssignedPerson().getNameSurname(), 30), "zimmetSahibi");
            else
                label.replaceTextName(" ", "zimmetSahibi");
        }
        if (l == 6 || l == 9) {
            if (asset.getAssignedLocationId() != 0)
                label.replaceTextName(makePrintable(asset.getAssignedLocation().getName(), 30), "zimmetYeri");
            else
                label.replaceTextName(" ", "zimmetYeri");
        }
        if (l == 17)
            label.replaceTextName(String.format(context.get().getLocale(), "%1$.2f\u20BA", asset.getPrice()), "fiyat");
    }*/

    public static Bitmap infoOverlay(Asset asset, MainActivity context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context);
        val labelSize = prefs.getString("label_size_prefs", "");
        val l = Integer.parseInt(Objects.requireNonNull(prefs.getString("label_type_prefs", "-1")));

        //create empty bitmap
        int width = 0, height = 0;
        if (labelSize.equals("W18")) { //find dimensions
            width = 362;
            height = 111;
        } else if (labelSize.equals("W24")) {
            width = 462;
            height = 136;
        }
        if (l == 5 || l == 6 || l == 7)
            width = 562;
        if (l == 8)
            width = 168;

        val labelLayoutId = context.getResources().getIdentifier("asset_label_" + l, "layout", context.getPackageName());
        View view = LayoutInflater.from(context).inflate(labelLayoutId, null);
        TextView unionName = view.findViewById(R.id.asset_label_union_name);
        if (unionName != null)
            unionName.setText(prefs.getString("main_company_name_prefs", ""));
        TextView instName = view.findViewById(R.id.asset_label_institution_name);
        if (instName != null)
            instName.setText(prefs.getString("company_name_prefs", TenantRepository.getCurrentTenant().getName()));

        String assetDefinition = asset.assetType.getTarget().getDefinition();
        TextView assetDef = view.findViewById(R.id.asset_label_asset_def);
        if (assetDef != null) {
            String features = asset.getFeatures();
            if (!Strings.isNullOrEmpty(features))
                assetDefinition = asset.assetType.getTarget().getDefinition() + ", " + features;
            assetDef.setText(assetDefinition);
        }

        TextView typeDef = view.findViewById(R.id.asset_label_asset_type_def);
        if (typeDef != null)
            typeDef.setText(assetDefinition);

        TextView budgetType = view.findViewById(R.id.asset_label_budget_type);
        if (budgetType != null) {
            val splittedBudget = asset.getBudgetType().getDefinition().split(" ");
            budgetType.setText(String.format("%s%s", splittedBudget[0].charAt(0), splittedBudget[1].charAt(0)));
        }

        TextView brand = view.findViewById(R.id.asset_label_brand);
        if (brand != null)
            brand.setText(asset.getBrandNameDefinition());

        TextView model = view.findViewById(R.id.asset_label_model);
        if (model != null)
            model.setText(asset.getModelNameDefinition());

        TextView serialNo = view.findViewById(R.id.asset_label_serial_no);
        if (serialNo != null)
            serialNo.setText(asset.getSerialNo());

        TextView assignedLoc = view.findViewById(R.id.asset_label_assigned_location);
        if (assignedLoc != null && asset.getAssignedLocationId() != 0)
            assignedLoc.setText(asset.getAssignedLocation().getName());

        /*TextView assignedPer = view.findViewById(R.id.asset_label_assigned_person);
        if (assignedPer != null && asset.getAssignedLocation() != null)
            assignedPer.setText(asset.getAssignedPerson().getNameSurname());*/

        TextView acqYear = view.findViewById(R.id.asset_label_acq_year);
        if (acqYear != null)
            acqYear.setText(String.valueOf(asset.getAcquisitionYear()));

        TextView bioType = view.findViewById(R.id.asset_label_bio_type);
        if (bioType != null)
            bioType.setText(asset.getBiomedicalType());

        TextView regNo = view.findViewById(R.id.asset_label_reg_no);
        if (regNo != null)
            regNo.setText(asset.getRegistrationCode());

        TextView tagNo = view.findViewById(R.id.asset_label_tag_no);
        if (tagNo != null)
            tagNo.setText(String.valueOf(asset.getRemoteId()));

        ImageView qrImage = view.findViewById(R.id.asset_label_qr_image);
        if (qrImage != null)
            qrImage.setImageBitmap(qrCreator(context.getString(R.string.barcode_prefix) + asset.getRemoteId(), 88));

        view.layout(0, 0, width, height);
        //pngSaver(bmp, MainActivity.rootFolder + File.separator + "a.png");
        return getViewBitmap(view);
    }

    private static Bitmap getViewBitmap(View view)
    {
        int width = view.getWidth();
        int height = view.getHeight();
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        view.measure(measuredWidth, measuredHeight);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(b));
        return b;
    }

    private static Bitmap qrCreator(String code, int size) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            EnumMap<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix bitMatrix = writer.encode(code , BarcodeFormat.QR_CODE, size, size, hints);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return qrBitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public enum LPResult {
        SUCCESS,
        CANCELED,
        ERROR_NO_PRINTER_SELECTED,
        ERROR_NO_LABEL_SELECTED,
        ERROR_CANNOT_FIND_PRINTER,
        ERROR_BROTHER_ERROR
    }
}

package com.frekanstan.tatf_demo.app.labeling;

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
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.locations.LocationDAO;
import com.frekanstan.tatf_demo.app.multitenancy.TenantRepository;
import com.frekanstan.tatf_demo.app.people.PersonDAO;
import com.frekanstan.tatf_demo.data.Asset;
import com.frekanstan.tatf_demo.view.MainActivity;
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

    public static Bitmap infoOverlay(Asset asset, MainActivity context) {
        val l = Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(context).getString("label_type_prefs", "-1")));

        int width = 0, height = 0;
        switch (l) {
            case 1:
            case 2:
                width = 880;
                height = 290;
                break;
            case 3:
                width = 290;
                height = 290;
                break;
            case 4:
            case 5:
                width = 1155;
                height = 340;
                break;
            case 6:
                width = 340;
                height = 340;
                break;
        }

        val labelLayoutId = context.getResources().getIdentifier("asset_label_" + l, "layout", context.getPackageName());
        View view = LayoutInflater.from(context).inflate(labelLayoutId, null);
        TextView instName = view.findViewById(R.id.asset_label_institution_name);
        if (instName != null)
            instName.setText(PreferenceManager.getDefaultSharedPreferences(context).getString("company_name_prefs", TenantRepository.getCurrentTenant().getName()));

        String assetDefinition = asset.getAssetTypeDefinition();
        TextView assetDef = view.findViewById(R.id.asset_label_asset_def);
        if (assetDef != null) {
            String features = asset.getFeatures();
            if (!Strings.isNullOrEmpty(features))
                assetDefinition = asset.getAssetTypeDefinition() + ", " + features;
            assetDef.setText(assetDefinition);
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

        TextView assignedPer = view.findViewById(R.id.asset_label_assigned_person);
        if (assignedPer != null && asset.getAssignedPersonId() != 0)
            assignedPer.setText(asset.getAssignedPerson().getNameSurname());

        ImageView qrImage = view.findViewById(R.id.asset_label_qr_image);
        if (qrImage != null)
            qrImage.setImageBitmap(qrCreator(context.getString(R.string.barcode_prefix) + asset.getAssetCode(), 40));

        view.layout(0, 0, width, height);
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

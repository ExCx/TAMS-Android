package com.frekanstan.asset_management.app.update;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.view.MainActivityBase;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import lombok.val;
import lombok.var;

import static android.content.Context.WIFI_SERVICE;
import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class AppUpdater
{
    public static boolean isBusy = false;

    public static void installApp(MainActivityBase context, ILoginFragment fragment, String versionFileName, String apkFileName, String packageName, String rootUri) {
        if (isBusy)
            return;
        if (!appInstalledOrNot(context, packageName))
        {
            isBusy = true;
            val builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.update_title);
            builder.setMessage(String.format(context.getString(R.string.alert_message_new_version_found_for_), packageName));
            builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                new UserResourceDownloader(context).execute(apkFileName, rootUri);
                dialog.dismiss();
            });
            builder.show();
        }
        else
            new VersionControl(context, fragment).execute(versionFileName, apkFileName, packageName, rootUri);
    }

    private static class VersionControl extends AsyncTask<String, Integer, UAResult>
    {
        WeakReference<MainActivityBase> context;
        private ILoginFragment fragment;
        private String apkFileName, packageUri, rootUri;

        VersionControl(MainActivityBase activity, ILoginFragment fragment)
        {
            context = new WeakReference<>(activity);
            this.fragment = fragment;
        }

        @Override
        protected UAResult doInBackground(String... params) {
            publishProgress(0);
            apkFileName = params[1];
            packageUri = params[2];
            rootUri = params[3];
            val wifiManager = ((WifiManager)context.get().getApplicationContext()
                    .getSystemService(WIFI_SERVICE));
            if (wifiManager == null)
                return UAResult.NO_INTERNET;

            //versiyon kontrolü
            try {
                val setupPath = params[3] + "files/userresources/" + params[0] + ".txt";
                val c = (HttpURLConnection) new URL(setupPath).openConnection();
                c.setRequestMethod("GET");
                c.connect();
                val bo = new ByteArrayOutputStream();
                byte[] buffer = new byte[128];
                //noinspection ResultOfMethodCallIgnored
                c.getInputStream().read(buffer); // Read from Buffer.
                bo.write(buffer); // Write Into Buffer.
                var curVer = getVersionOfApp(context.get(), packageUri);
                if (curVer != null && bo.toString().startsWith(curVer))
                    return UAResult.UPDATE_UNAVAILABLE;
                else
                    return UAResult.UPDATE_AVAILABLE;
            } catch (final Exception ex) {
                ex.printStackTrace();
                return UAResult.ERROR;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            context.get().progDialog.setMessage(context.get().getString(R.string.looking_for_updates));
            context.get().progDialog.show();
        }

        @Override
        protected void onPostExecute(UAResult result) {
            context.get().progDialog.hide();
            switch (result)
            {
                case NO_INTERNET:
                    Toast.makeText(context.get(), R.string.no_internet, Toast.LENGTH_LONG).show();
                    break;
                case NO_SERVER:
                    Toast.makeText(context.get(), R.string.no_server, Toast.LENGTH_LONG).show();
                    break;
                case ERROR:
                    Toast.makeText(context.get(), R.string.version_control_error, Toast.LENGTH_LONG).show();
                    break;
                case UPDATE_AVAILABLE:
                    isBusy = true;
                    AlertDialog.Builder builder = new AlertDialog.Builder(context.get());
                    builder.setTitle(R.string.update_title);
                    builder.setMessage(String.format(context.get().getString(R.string.alert_message_new_version_found_for_), packageUri));
                    builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                        new UserResourceDownloader(context.get()).execute(apkFileName, rootUri);
                        dialog.dismiss();
                    });
                    builder.show();
                    break;
                case UPDATE_UNAVAILABLE:
                    if (apkFileName.contains("_mobil") && !isBusy)
                        fragment.initiateLogin();
                case IGNORE:
                    break;
            }
        }
    }

    private static class UserResourceDownloader extends AsyncTask<String, Integer, UAResult>
    {
        WeakReference<MainActivityBase> context;
        File outputFile;

        UserResourceDownloader(MainActivityBase activity)
        {
            context = new WeakReference<>(activity);
        }

        @Override
        protected UAResult doInBackground(String... params) {
            byte[] data = new byte[1024];
            var byteCount = 0F;
            int count;
            outputFile = new File(context.get().getExternalFilesDir(DIRECTORY_DOWNLOADS), params[0]);
            publishProgress(-1);
            try {
                val url = new URL(params[1] + "files/userresources/" + params[0] + ".apk");
                val conn = url.openConnection();
                if(conn instanceof HttpURLConnection) {
                    ((HttpURLConnection)conn).setRequestMethod("HEAD");
                }
                conn.getInputStream();
                val totalBytes = (float)conn.getContentLength();
                conn.connect();
                val input = new BufferedInputStream(url.openStream(), 8192);
                val outputStream = new FileOutputStream(outputFile.getAbsolutePath());
                while ((count = input.read(data)) != -1) {
                    byteCount += count;
                    float percent = (byteCount / totalBytes) * 100F;
                    if (Math.abs(percent - (int)percent) < .1F)
                        publishProgress((int)percent);
                    outputStream.write(data, 0, count);
                }
                outputStream.flush();
                outputStream.close();
                input.close();
                return UAResult.UPDATE_COMPLETE;
            } catch (final Exception ex) {
                ex.printStackTrace();
                return UAResult.ERROR;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            val progDialog = context.get().progDialog;
            if (progress[0].equals(-1)) {
                progDialog.setMessage(context.get().getString(R.string.downloading_updates));
                progDialog.setIndeterminate(false);
                progDialog.show();
            }
            else {
                progDialog.setProgress(progress[0]);
                progDialog.setMessage(context.get().getString(R.string.downloading_updates) +
                        " (%" + progress[0] + ")");
            }
        }

        @Override
        protected void onPostExecute(UAResult result) {
            context.get().progDialog.hide();
            switch (result)
            {
                case ERROR:
                    isBusy = false;
                    Toast.makeText(context.get(), R.string.setup_download_error, Toast.LENGTH_LONG).show();
                    break;
                case UPDATE_COMPLETE:
                    isBusy = false;
                    val promptInstall = new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.fromFile(outputFile), "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.get().startActivity(promptInstall);
                    break;
            }
        }
    }

    private static String getVersionOfApp(Context context, String uri) {
        try {
            return context.getPackageManager().getPackageInfo(uri, PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean appInstalledOrNot(Context context, String uri) {
        try {
            context.getPackageManager().getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
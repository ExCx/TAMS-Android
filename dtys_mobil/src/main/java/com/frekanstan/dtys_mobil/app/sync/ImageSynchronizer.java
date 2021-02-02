package com.frekanstan.dtys_mobil.app.sync;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.connection.ServiceConnectorBase;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.photo.ChangePhotoAvailabilityInput;
import com.frekanstan.dtys_mobil.app.connection.ServiceConnector;
import com.frekanstan.dtys_mobil.data.ImageToUpload;
import com.frekanstan.dtys_mobil.view.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.reactive.DataObserver;
import lombok.val;
import lombok.var;

public class ImageSynchronizer implements DataObserver<List<ImageToUpload>> {
    private static ImageSynchronizer ourInstance;
    private static boolean isBusy;
    private static ServiceConnectorBase conn;
    private static List<ImageToUpload> cache = new ArrayList<>();

    private ImageSynchronizer(MainActivity context) {
        isBusy = false;
        conn = ServiceConnector.getInstance(context);
    }

    public static synchronized ImageSynchronizer getInstance(MainActivity context){
        if (ourInstance == null)
            ourInstance = new ImageSynchronizer(context);
        return ourInstance;
    }

    @Override
    public void onData(@NonNull List<ImageToUpload> data) {
        if (data.size() == 0)
            return;
        if (!NetManager.isOnline || isBusy)
            cache = data;
        if (!isBusy) {
            isBusy = true;
            new ImageUploader(data).execute();
        }
    }

    public void syncCache() {
        onData(cache);
    }

    private static class ImageUploader extends AsyncTask<Void, Void, Boolean>
    {
        private List<ImageToUpload> list;

        ImageUploader(List<ImageToUpload> list) {
            this.list = list;
        }

        protected Boolean doInBackground(Void... voids) {
            try {
                val input = new Bundle();
                input.putByte("delete", (byte)-1);
                input.putByte("makeMain", (byte)-1);
                val imagesToUpload = ImageToUploadDAO.getDao().getAll(input);
                if (imagesToUpload.size() > 0) {
                    var assetImages = new ArrayList<File>();
                    var personImages = new ArrayList<File>();
                    for (val ito : imagesToUpload) {
                        if (ito.getType().equals("asset"))
                            assetImages.add(new File(ito.getPath()));
                        else if (ito.getType().equals("person"))
                            personImages.add(new File(ito.getPath()));
                    }
                    if (assetImages.size() > 0)
                        conn.uploadImages(assetImages, "asset");
                    if (personImages.size() > 0)
                        conn.uploadImages(personImages, "person");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                ObjectRequest<AbpResult<Boolean>> request = conn.getSyncPhotosReq(new ChangePhotoAvailabilityInput(new ArrayList<>(list)));
                request.setResponseListener(response -> {
                    if (response.getSuccess()) {
                        ImageToUploadDAO.getDao().removeAll(list);
                        cache.clear();
                    }
                });
                conn.addToRequestQueue(request);
            }
            isBusy = false;
        }
    }
}
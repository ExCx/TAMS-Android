package com.frekanstan.asset_management.app.helpers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.MediaStore;

import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import lombok.var;

public class PictureTaker {
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static void dispatchTakePictureIntent(File imageFile, Fragment f) {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        var cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        f.startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
    }

    public static void saveBitmapToFile(File file){
        try {
            var o = new BitmapFactory.Options(); // BitmapFactory options to downsize the image
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(file), null, o);
            System.gc();
            var o2 = new BitmapFactory.Options();
            o2.inSampleSize = 2;
            var selectedBitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, o2);
            file.createNewFile();
            var outputStream = new FileOutputStream(file);
            if (selectedBitmap != null)
                selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

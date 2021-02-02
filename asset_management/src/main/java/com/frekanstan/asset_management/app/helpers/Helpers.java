package com.frekanstan.asset_management.app.helpers;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Arrays;

import lombok.val;

public class Helpers {
    public static boolean areAllTrue(boolean[] array) {
        for(boolean b : array) if(!b) return false;
        return true;
    }

    public static void setAllTrue(boolean[] array) {
        Arrays.fill(array, true);
    }

    public static void downloadFile(String fileUrl, String outputPath){
        int count;
        byte[] data = new byte[1024];
        try {
            val url = new URL(fileUrl);
            url.openConnection().connect();
            val input = new BufferedInputStream(url.openStream(), 8192);
            val output = new FileOutputStream(outputPath);
            while ((count = input.read(data)) != -1)
                output.write(data, 0, count);
            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
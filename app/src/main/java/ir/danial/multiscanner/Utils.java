package ir.danial.multiscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class Utils {

    Utils() {

    }

    private Uri save(Context context,Bitmap bitmap,int compressValue){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getApplicationContext().getPackageName()
                + "/Files");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        File file = new File(mediaStorageDir, UUID.randomUUID().toString()+".jpg");
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressValue, out);
            //String imgSaved=MediaStore.Images.Media.insertImage(context.getContentResolver(),mediaStorageDir.getAbsolutePath(), UUID.randomUUID().toString()+".jpeg", "drawing");
        } catch (FileNotFoundException e) {
            Log.d("saving", "File not found: " + e.getMessage());
        }
        return Uri.fromFile(file);
    }


    Uri getUri(Context context, Bitmap bitmap,int compressValue) {
        return save(context,bitmap,compressValue);
    }

    ArrayList<Uri> getUris(Context context, ArrayList<Bitmap> bitmaps,int compressValue) {

        ArrayList<Uri> Uris = new ArrayList<>();
        for (Bitmap bitmap:bitmaps) {
            Uris.add(save(context,bitmap,compressValue));
        }
        return Uris;
    }


    Bitmap getBitmap(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

    ArrayList<Bitmap> getBitmaps(Context context, ArrayList<Uri> uris) throws IOException {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        for (Uri uri : uris) {
            bitmaps.add(MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri));
        }
        return bitmaps;
    }
}



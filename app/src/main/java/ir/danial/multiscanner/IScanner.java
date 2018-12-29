package ir.danial.multiscanner;

import android.net.Uri;

import java.util.ArrayList;


public interface IScanner {

    void onBitmapSelect(Uri uri);

    void onScanFinish(ArrayList<Uri> uri);
}

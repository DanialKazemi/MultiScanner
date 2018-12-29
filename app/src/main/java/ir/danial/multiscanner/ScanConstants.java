package ir.danial.multiscanner;

import android.os.Environment;


class ScanConstants {

    final static int PICKFILE_REQUEST_CODE = 1;
    final static int START_CAMERA_REQUEST_CODE = 2;
    final static String OPEN_INTENT_PREFERENCE = "selectContent";
    public final static String IMAGE_BASE_PATH_EXTRA = "ImageBasePath";
    final static int OPEN_CAMERA = 4;
    final static int OPEN_MEDIA = 5;
    final static int GET_MEDIA = 6;
    final static String SCANNED_RESULT = "scannedResult";
    final static String IMAGE_PATH = Environment
            .getExternalStorageDirectory().getPath() + "/MStemp";

    final static String SELECTED_BITMAP = "selectedBitmap";
}

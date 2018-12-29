package ir.danial.multiscanner;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Created by Danial on 10/8/2018.
 */

class MessageBox {
    static void send(String method, String message, Context context) {
        AlertDialog.Builder messageBox = new AlertDialog.Builder(context);
        messageBox.setTitle(method);
        messageBox.setMessage(message);
        messageBox.setCancelable(false);
        messageBox.setNeutralButton("OK", null);
        messageBox.show();
    }
}

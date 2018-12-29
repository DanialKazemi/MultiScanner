package ir.danial.multiscanner;

import android.view.MotionEvent;

/**
 * Created by Danial on 4/4/2018.
 */

class RxBus{

    private static RxBus instance;

    static RxBus getInstance() {
        if (instance == null) {
            instance = new RxBus();
        }
        return instance;
    }

    void sendMotionEvent(MotionEvent event) {
        ScanFragment.sourceImageView.onTouchEvent(event);
    }
}
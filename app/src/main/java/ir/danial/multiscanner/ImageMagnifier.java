package ir.danial.multiscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Danial on 4/3/2018.
 */

public class ImageMagnifier extends AppCompatImageView {
    private PointF zoomPos;
    private boolean zooming = false;
    private Matrix matrix;
    private Paint paint;
    Bitmap bitmap;
    BitmapShader shader;
    int sizeOfMagnifier = 100;//28sdp
    private boolean sideLeft=false;

    public ImageMagnifier(Context context) {
        super(context);
        init();
    }

    public ImageMagnifier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ImageMagnifier(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        zoomPos = new PointF(0, 0);
        matrix = new Matrix();
        paint = new Paint();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        zoomPos.x = event.getX();
        zoomPos.y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                buildDrawingCache();
                if (zoomPos.x<getWidth()/2 && zoomPos.y<getHeight()/3) sideLeft=true;else sideLeft=false;
            case MotionEvent.ACTION_MOVE:
                zooming = true;
                if (zoomPos.x<getWidth()/2 && zoomPos.y<getHeight()/3) sideLeft=true;else sideLeft=false;
                this.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                zooming = false;
                this.invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                zooming = false;
                this.invalidate();
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!zooming) {
            //buildDrawingCache();
        } else {
            bitmap = getDrawingCache();
            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            paint = new Paint();
            paint.setShader(shader);

            int radius=sizeOfMagnifier;
            int distance;
            if (sideLeft) distance=getWidth()-220;
            else distance=20; //220 , 20 //63,7 sdp

            matrix.reset();
            matrix.postScale(2f, 2f, zoomPos.x, zoomPos.y);
            paint.getShader().setLocalMatrix(matrix);

            RectF src = new RectF(zoomPos.x-60, zoomPos.y-60, zoomPos.x+60, zoomPos.y+60);
            RectF dst;
            dst=null;
            if (sideLeft) dst = new RectF(getWidth()/2 - 120 ,0,getWidth()/2,120);
            else dst = new RectF(0, 0, 120, 120);

            matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
            matrix.postScale(2f, 2f);
            paint.getShader().setLocalMatrix(matrix);
            paint.setStrokeWidth(5);
            canvas.drawCircle(radius + distance, radius + 20, radius, paint);//120,120,100

            paint = new Paint();
            paint.setColor(Color.parseColor("#FAFFD1"));
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(radius + distance, radius+  20, radius, paint); //120,120,100

            paint = new Paint();
            paint.setColor(Color.parseColor("#A1FFCE"));
            paint.setTextSize(72);
            if (sideLeft) canvas.drawText("+",getWidth()-140,radius+40,paint);
            else canvas.drawText("+",radius,radius+40,paint); //100 , 140

        }
    }


}
package ir.danial.multiscanner;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.github.florent37.viewtooltip.ViewTooltip;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.imgproc.Imgproc.contourArea;

public class LiveActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String    TAG = "MOAAP_CHP5";

    static int                      REQUEST_CAMERA = 0;
    static boolean                  camera_granted = false;

    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;

    MatOfPoint2f prevFeatures, nextFeatures;
    MatOfPoint features;

    MatOfByte status;
    MatOfFloat err;

    private JavaCameraView   mOpenCvCameraView;
    private Mat res;

    Bitmap sample;
    List<Point> source;
    Mat mat;
    Mat prevRes;
    public static final int PICK_IMAGE = 1;
    boolean chosenGallery=false;
    Bitmap bmpSave;

    boolean isFlashOn=false;
    boolean isCropEnabled=true;

    ImageView imgSicon;

    boolean running=true;

    public double warpRatio=1;
    public double ratioSave=1;

    Thread worker;
    List<Point> target;

    public Button dlgYes, dlgNo;
    public ImageView dlgLeft,dlgRight,dlgImage,dlgFlip;
    public LinearLayout linBar;
    public SeekBar dlgSeek;

    Mat matSave;
    List<Point> sourceSave;

    ImageView imgLines;
    ImageView imgFocus;

    boolean clickedToFocus=false;
    float focusX;
    float focusY;
    Bitmap linesBmp;
    Bitmap focusBitmap;
    Paint zoomPaint;

    Paint linesPaint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_live);

        Intent intent = getIntent();
        String path = intent.getStringExtra("bmpPath");
        Bitmap bmp = BitmapFactory.decodeFile(path);
        if (bmp!=null) {
            sample=bmp;
            chosenGallery=true;
            ImageView imgPick=(ImageView)findViewById(R.id.imgPick);
            imgPick.setImageResource(R.drawable.minus);
        }

        if (ContextCompat.checkSelfPermission(LiveActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(LiveActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }else {
            Log.i("permission", "READ_EXTERNAL_STORAGE already granted");
            camera_granted = true;
        }

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.main_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);


        imgLines=(ImageView)findViewById(R.id.imgLines);
        imgFocus=(ImageView)findViewById(R.id.imgFocus);

        imgFocus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!clickedToFocus) {
                    focusX = event.getX();
                    focusY = event.getY();
                }

                clickedToFocus=true;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clickedToFocus=false;
                    }
                }, 1500);

                return false;
            }
        });

        final Dialog dialog = new Dialog(LiveActivity.this, R.style.Theme_Dialog);
        dialog.setContentView(R.layout.bmp_save_dialog);

        dlgYes = (Button) dialog.findViewById(R.id.btnYes);
        dlgNo = (Button) dialog.findViewById(R.id.btnNo);
        dlgLeft = (ImageView) dialog.findViewById(R.id.imgLeft);
        dlgRight = (ImageView) dialog.findViewById(R.id.imgRight);
        dlgImage = (ImageView) dialog.findViewById(R.id.imgDialog);
        dlgFlip = (ImageView) dialog.findViewById(R.id.imgFlip);
        linBar=(LinearLayout) dialog.findViewById(R.id.linBar);
        dlgSeek = (SeekBar) dialog.findViewById(R.id.dlgSeek);

        dlgNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dlgYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                saveImage(bmpSave,timeStamp);
                setResult(Activity.RESULT_OK);
                dialog.dismiss();
            }
        });
        dlgRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mat matBitmap=new Mat(bmpSave.getHeight(),bmpSave.getWidth(),matSave.type());
                matBitmap.setTo(new Scalar(0));
                Utils.bitmapToMat(bmpSave,matBitmap);
                Core.rotate(matBitmap,matBitmap,Core.ROTATE_90_CLOCKWISE);
                Bitmap presave=Bitmap.createBitmap(matBitmap.width(),matBitmap.height(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(matBitmap,presave);

                bmpSave=Bitmap.createBitmap(presave);

                dlgImage.setImageBitmap(bmpSave);
            }
        });
        dlgLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Mat matBitmap=new Mat(bmpSave.getHeight(),bmpSave.getWidth(),matSave.type());
                matBitmap.setTo(new Scalar(0));
                Utils.bitmapToMat(bmpSave,matBitmap);
                Core.rotate(matBitmap,matBitmap,Core.ROTATE_90_COUNTERCLOCKWISE);
                Bitmap presave=Bitmap.createBitmap(matBitmap.width(),matBitmap.height(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(matBitmap,presave);

                bmpSave=Bitmap.createBitmap(presave);

                dlgImage.setImageBitmap(bmpSave);
            }
        });
        dlgFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bmpSave=flipBitmap(bmpSave);

                dlgImage.setImageBitmap(bmpSave);
            }
        });

        final ImageView imgSample=(ImageView)findViewById(R.id.imgSample);
        imgSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgImage.setImageBitmap(bmpSave);

                if (bmpSave!=null) {
                    if (chosenGallery) {
                        linBar.setVisibility(View.VISIBLE);
                        dlgSeek.setProgress(3);
                    } else linBar.setVisibility(View.GONE);

                    dialog.show();
                }
            }
        });

        dlgSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                Runnable t= new Runnable() {
                    @Override
                    public void run() {
                        Bitmap presave=warpDisplayImage(matSave,sourceSave,(double)seekBar.getProgress() / 10);
                        bmpSave = Bitmap.createScaledBitmap(presave, Math.round(presave.getWidth())
                                , Math.round(presave.getWidth() * (float) Math.abs(1/ratioSave)), true);
                        dlgImage.setImageBitmap(bmpSave);
                    }
                };
                t.run();

            }
        });

        //TODO bitmap resolution
        imgSicon=(ImageView)findViewById(R.id.imgSicon);
        ImageView imgDone=(ImageView)findViewById(R.id.imgDone);
        imgDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap presave;
                if (isCropEnabled) {
                    if (source.size()==4) {
                        sourceSave=new ArrayList<>(source);

                        double distanceA = Math.hypot(sourceSave.get(0).x-sourceSave.get(1).x,
                                sourceSave.get(0).y-sourceSave.get(1).y);
                        double distanceB = Math.hypot(sourceSave.get(1).x-sourceSave.get(2).x,
                                sourceSave.get(1).y-sourceSave.get(2).y);
                        warpRatio=distanceA/distanceB;
                        ratioSave=warpRatio;

                    }else {
                        bmpSave=Bitmap.createBitmap(prevRes.width(),prevRes.height(), Bitmap.Config.ARGB_4444);
                        Utils.matToBitmap(prevRes, bmpSave);
                    }
                }else {
                    presave=Bitmap.createBitmap(prevRes.width(),prevRes.height(), Bitmap.Config.ARGB_4444);
                    Utils.matToBitmap(prevRes, presave);
                    bmpSave=presave;
                }

                matSave=prevRes.clone();

                imgSample.setImageBitmap(bmpSave);

                imgSicon.setImageResource(R.drawable.checkmark);
            }
        });

        final ImageView imgPick=(ImageView)findViewById(R.id.imgPick);
        imgPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosenGallery) {
                    sample=Bitmap.createBitmap(50,50, Bitmap.Config.ARGB_4444);
                    imgPick.setImageResource(R.drawable.gallery);
                    chosenGallery=false;
                }else {
                    worker.interrupt();
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                }
            }
        });

        final ImageView imgFlash=(ImageView)findViewById(R.id.imgFlash);
        imgFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (isFlashOn) {
                        mOpenCvCameraView.turnOffTheFlash();
                        imgFlash.setImageResource(R.drawable.flash);
                        isFlashOn=false;
                    }else {
                        mOpenCvCameraView.turnOnTheFlash();
                        imgFlash.setImageResource(R.drawable.flash_off);
                        isFlashOn=true;
                    }
                }catch (Exception e){
                    MessageBox.send("Error", e.getMessage(),LiveActivity.this);
                    e.printStackTrace();
                }

            }
        });

        final ImageView imgCrop=(ImageView)findViewById(R.id.imgCrop);
        imgCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCropEnabled) {
                    imgCrop.setImageResource(R.drawable.crop);
                    isCropEnabled = false;
                }else {
                    imgCrop.setImageResource(R.drawable.no_crop);
                    isCropEnabled = true;
                }

            }
        });

        final ImageView imgAlert=(ImageView)findViewById(R.id.imgAlert);
        imgAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(LiveActivity.this, imgAlert)
                        .autoHide(true, 7000)
                        .corner(30)
                        .position(ViewTooltip.Position.LEFT)
                        .text("The difference between dominant colors of background and document, effects the scanning results Significantly!")
                        .show();
            }
        });
    }

    private Bitmap flipBitmap(Bitmap d)
    {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        Bitmap src = Bitmap.createBitmap(d);
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return Bitmap.createBitmap(dst);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    //
                    return;
                }
                InputStream inputStream = this.getContentResolver().openInputStream(data.getData());
                sample=BitmapFactory.decodeStream(inputStream);

                chosenGallery=true;
                ImageView imgPick=(ImageView)findViewById(R.id.imgPick);
                imgPick.setImageResource(R.drawable.minus);
            }

        }catch (FileNotFoundException e){
            MessageBox.send("Error", e.getMessage(),LiveActivity.this);
        }

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Log.d("Main", "OpenCV not loaded");
        } else {
            Log.d("Main", "OpenCV loaded");
            mOpenCvCameraView.enableView();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("permission", "CAMERA granted");
                camera_granted = true;
            } else {
                Log.i("permission", "CAMERA denied");
            }
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);


        if (sample == null) {
            sample = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_4444);
        }

        sample = Bitmap.createScaledBitmap(sample, 1000, 1000, true); //Todo size

        mat = new Mat(mGray.rows(), mGray.cols(), mGray.type());

        source = new ArrayList<>();

        resetVars();

        zoomPaint = new Paint();
        zoomPaint.setStyle(Paint.Style.STROKE);
        zoomPaint.setStrokeWidth(5f);
        zoomPaint.setShadowLayer(2f,0,0,Color.BLACK);
        zoomPaint.setAntiAlias(true);
        zoomPaint.setColor(Color.WHITE);

        linesPaint = new Paint();
        linesPaint.setColor(ContextCompat.getColor(this,R.color.line_blue));
        linesPaint.setStrokeWidth(5);
        linesPaint.setAntiAlias(true);

        target = new ArrayList<>();
        target.add(new Point(0, 0));
        target.add(new Point(mRgba.width(), 0));
        target.add(new Point(mRgba.width(), mRgba.height()));
        target.add(new Point(0, mRgba.height()));

        thread();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (isCropEnabled) {
            final Mat mRgba2 = mRgba.clone();

            mRgba2.setTo(new Scalar(0, 0, 0, 0));
            if (source.size() == 4 && chosenGallery) {
                Mat src = new Mat(mRgba.height(), mRgba.width(), mRgba.type());
                src.setTo(new Scalar(0));
                Utils.bitmapToMat(sample, src);

                Core.rotate(src, src, Core.ROTATE_180);

                Mat trans = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(target), Converters.vector_Point2f_to_Mat(source)); //source
                Mat dst = new Mat(mRgba.height(), mRgba.width(), mRgba.type());
                dst.setTo(new Scalar(0));
                Imgproc.warpPerspective(src, dst, trans, dst.size());
                Core.flip(dst,dst,0);
                Core.addWeighted(mRgba2, 0.4, dst, 0.6, 0.0, mRgba2);
            }

            if (source.size() == 4) {
                Core.transpose(mRgba2, mRgba2);
                Core.flip(mRgba2, mRgba2, 1);
                linesBmp = Bitmap.createBitmap(mRgba2.width(), mRgba2.height(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(mRgba2, linesBmp);

            } else {
                prevRes = mRgba.clone();
            }
        }else  linesBmp=null;


        focusBitmap=Bitmap.createBitmap(mOpenCvCameraView.getWidth(),mOpenCvCameraView.getHeight(), Bitmap.Config.ARGB_4444);
        if (clickedToFocus) {
            Canvas canvas = new Canvas(focusBitmap);
            canvas.drawCircle(focusX, focusY, 60, zoomPaint);
        }else if (source.size()!=4 && !clickedToFocus){
            linesBmp=null;
            focusBitmap=null;
        }

        if (linesBmp!=null && source.size()==4) {
            Canvas canvas = new Canvas(linesBmp);
            canvas.drawLine((float) source.get(0).y, (float) source.get(0).x, (float) source.get(1).y, (float) source.get(1).x, linesPaint);
            canvas.drawLine((float) source.get(1).y, (float) source.get(1).x, (float) source.get(2).y, (float) source.get(2).x, linesPaint);
            canvas.drawLine((float) source.get(2).y, (float) source.get(2).x, (float) source.get(3).y, (float) source.get(3).x, linesPaint);
            canvas.drawLine((float) source.get(3).y, (float) source.get(3).x, (float) source.get(0).y, (float) source.get(0).x, linesPaint);
            linesBmp=flipBitmap(linesBmp);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imgLines.setImageBitmap(linesBmp);
                imgFocus.setImageBitmap(focusBitmap);
            }
        });

        return mRgba;
    }

    public List<Point> getOrderedPoints(List<Point> points) {
        List<Point> orderedPoints=new ArrayList<>();
        List<Double> pointsY= new ArrayList<>();
        List<Point> finPoints=new ArrayList<>();

        if (points.get(0).y!=0 && points.get(2).y!=0) {
            for (Point point : points) {
                pointsY.add(point.y);
            }
            Collections.sort(pointsY);

            Point y0 = new Point();
            Point y1 = new Point();
            Point y2 = new Point();
            Point y3 = new Point();

            for (Point point : points) {
                if (point.y == pointsY.get(0) && y0.equals(new Point(0,0))) y0 = point;
                else if (point.y == pointsY.get(1) && y1.equals(new Point(0,0))) y1 = point;
                else if (point.y == pointsY.get(2) && y2.equals(new Point(0,0))) y2 = point;
                else if (point.y == pointsY.get(3) && y3.equals(new Point(0,0))) y3 = point;
            }

            if (y0.x > y1.x) {
                orderedPoints.add(y0);
                orderedPoints.add(y1);
            } else {
                orderedPoints.add(y1);
                orderedPoints.add(y0);
            }
            if (y2.x > y3.x) {
                orderedPoints.add(y3);
                orderedPoints.add(y2);
            } else {
                orderedPoints.add(y2);
                orderedPoints.add(y3);
            }


            finPoints.add(orderedPoints.get(0));
            finPoints.add(orderedPoints.get(3));
            finPoints.add(orderedPoints.get(2));
            finPoints.add(orderedPoints.get(1));
        }

        return finPoints;
    }

    private void resetVars(){
        features = new MatOfPoint();
        prevFeatures = new MatOfPoint2f();
        nextFeatures = new MatOfPoint2f();
        status = new MatOfByte();
        err = new MatOfFloat();
    }

    public Bitmap warpDisplayImage(Mat inputMat,List<Point> points, double overlay) {
        int resultWidth = inputMat.width();
        int resultHeight = inputMat.height();

        Mat startM = Converters.vector_Point2f_to_Mat(points);

        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(0, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, 0);

        Mat outputMat = new Mat(resultHeight, resultWidth, CvType.CV_8UC4);

        List<Point> dest = new ArrayList<>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);

        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight));

        Core.rotate(outputMat,outputMat,Core.ROTATE_90_COUNTERCLOCKWISE);
        Mat matSample=new Mat(outputMat.rows(),outputMat.cols(),outputMat.type());
        Utils.bitmapToMat(sample,matSample);

        if (chosenGallery){
            try {
                Core.flip(outputMat,outputMat,0);
                Core.rotate(outputMat,outputMat,Core.ROTATE_180);
                Core.addWeighted(outputMat, overlay, matSample, 1-overlay, 0.0, outputMat);
            }catch (Exception e){
                e.printStackTrace();
                Imgproc.resize(matSample,matSample,outputMat.size());
                Core.addWeighted(outputMat, overlay, matSample, 1-overlay, 0.0, outputMat);
            }
        }else Core.flip(outputMat,outputMat,1);

        Bitmap descBitmap = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outputMat, descBitmap);

        return descBitmap;
    }

    public void saveImage(Bitmap finalBitmap, String image_name) {
        String root = Environment.getExternalStorageDirectory().toString() + "/MultiScanner";
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "MultiScanner-" + image_name+ ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);

            if (chosenGallery)
            finalBitmap=ImageFilter.applyMagic(ImageFilter.applyClahe(finalBitmap));

            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            Toast.makeText(LiveActivity.this,"Image Saved Successfully!",Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(LiveActivity.this,"Image Failed to Save, Please Try Again!",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void thread() {
        worker = new Thread() {
            @Override
            public void run() {
                try {
                    while (running) {
                        Mat inputFrame = mRgba.clone();
                        if (inputFrame == null) {
                            //
                            continue;
                        }

                        final List<MatOfPoint> contours = new ArrayList<>();

                        res=inputFrame.clone();
                        Bitmap bitmap = Bitmap.createBitmap(res.width(), res.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(res, bitmap);

                        Imgproc.cvtColor(inputFrame,mGray,Imgproc.COLOR_BGR2GRAY);
                        Imgproc.adaptiveThreshold(mGray,mGray,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,
                                Imgproc.THRESH_BINARY,41,7.0);

                        Mat nGray=mGray.clone();

                        Imgproc.findContours(nGray, contours, new Mat(),
                                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                        MatOfPoint2f approxCurve = new MatOfPoint2f();
                        MatOfPoint2f approxCurve_temp = new MatOfPoint2f();

                        for (MatOfPoint cnt : contours) {
                            int contourSize = (int) cnt.total();
                            MatOfPoint2f new_mat = new MatOfPoint2f(cnt.toArray());
                            Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize * 0.1, true);
                            MatOfPoint approxf1 = new MatOfPoint();
                            approxCurve_temp.convertTo(approxf1,CvType.CV_32S);
                            if (approxCurve_temp.total() == 4 && contourArea(cnt) > 100000.00 && Imgproc.isContourConvex(approxf1)) {
                                approxCurve = approxCurve_temp;
                                break;
                            }
                        }

                        double[] temp_double = approxCurve.get(0, 0);
                        if (temp_double != null) {
                            Point p1 = new Point(temp_double[0], temp_double[1]);
                            temp_double = approxCurve.get(1, 0);
                            Point p2 = new Point(temp_double[0], temp_double[1]);
                            temp_double = approxCurve.get(2, 0);
                            Point p3 = new Point(temp_double[0], temp_double[1]);
                            temp_double = approxCurve.get(3, 0);
                            Point p4 = new Point(temp_double[0], temp_double[1]);

                            source = new ArrayList<>();
                            source.add(p4);
                            source.add(p3);
                            source.add(p2);
                            source.add(p1);

                            List<Point> points = getOrderedPoints(source);
                            if (points.size()==4) source=new ArrayList<>(points);

                            Bitmap combinedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(combinedBitmap);
                            canvas.drawBitmap(bitmap, 0, 0, null);

                            sample = Bitmap.createScaledBitmap(sample, bitmap.getWidth(), bitmap.getHeight(), true);

                        } else if (source.size()==4){
                            Bitmap combinedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(combinedBitmap);
                            canvas.drawBitmap(bitmap, 0, 0, null);

                            sample = Bitmap.createScaledBitmap(sample, bitmap.getWidth(), bitmap.getHeight(), true);
                        }
                        if (isCropEnabled)
                            prevRes=res.clone();
                        else prevRes=mRgba.clone();
                    }
                }catch (Exception e){
                    Log.e("worker Thread","Oh Crop!");
                    e.printStackTrace();
                }
            }
        };
        worker.start();
    }


}
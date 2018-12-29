package ir.danial.multiscanner;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.github.florent37.viewtooltip.ViewTooltip;
import ir.danial.multiscanner.Adapters.ContourAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;
import org.opencv.utils.Converters;

import static java.lang.Math.pow;

public class ScanFragment extends Fragment implements ContourAdapter.ItemClickListener {

    Button scanButton;
    public static ImageMagnifier sourceImageView;
    public FrameLayout sourceFrame;
    public PolygonView polygonView;
    public View view;
    private ProgressDialogFragment progressDialogFragment;
    private IScanner scanner;
    private Bitmap original;

    Button btnAdd;

    private ImageView imgDraw;
    boolean isDrawing = false;

    Bitmap finBitmap;
    List<MatOfPoint> detectedContours;

    RelativeLayout drawLayout;
    Paint drawPaint;
    View drawView;
    Path drawPath2;
    Bitmap drawBitmap;
    Canvas drawCanvas;
    Paint oPaint; //Black Stroke

    LinearLayoutManager contoursLayout;
    public static ContourAdapter adapter;

    public static ArrayList<Bitmap> contourItems;

    RecyclerView rvContour;

    public static int chosenIndex = 0;

    ArrayList<Map<Integer, PointF>> arrayPoints;

    public static FrameLayout frameLayout;

    ir.danial.multiscanner.Utils utils=new ir.danial.multiscanner.Utils();

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IScanner activity;
        if (!(context instanceof IScanner)) {
            throw new ClassCastException("Activity must implement IScanner");
        }else activity=(IScanner) context;

        this.scanner = activity;
    }

    @SuppressWarnings("deprecation")
    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < 23) {
            onAttachToContext(activity);
            this.scanner=(IScanner) activity;
        }
    }

    /*
     * This method will be called from one of the two previous method
     */
    protected void onAttachToContext(Context context) {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.scan_fragment_layout, sourceFrame,false);
        view.setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    public ScanFragment() {

    }

    private void init() {
        sourceImageView = (ImageMagnifier) view.findViewById(R.id.sourceImageView);
        scanButton = (Button) view.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new ScanButtonClickListener());
        btnAdd = (Button) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new btnAddClickListener());
        sourceFrame = (FrameLayout) view.findViewById(R.id.sourceFrame);
        polygonView = (PolygonView) view.findViewById(R.id.polygonView);
        drawLayout = (RelativeLayout) view.findViewById(R.id.rel);
        frameLayout = (FrameLayout) view.findViewById(R.id.frameLayout);

        imgDraw = (ImageView) view.findViewById(R.id.imgDraw);
        imgDraw.setOnClickListener(new imgDrawClickListener());

        sourceFrame.post(new Runnable() {
            @Override
            public void run() {
                original = getBitmap();

                setBitmap();

                Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());

                drawView = new SketchSheetView(drawLayout.getContext());

                drawPaint = new Paint();

                drawPath2 = new Path();

                drawLayout.addView(drawView, new RelativeLayout.LayoutParams(
                        scaledBitmap.getWidth(),
                        scaledBitmap.getHeight()));

                drawPaint.setDither(true);
                drawPaint.setColor(Color.parseColor("#FFFFFF"));
                drawPaint.setStyle(Paint.Style.STROKE);
                drawPaint.setStrokeJoin(Paint.Join.ROUND);
                drawPaint.setStrokeCap(Paint.Cap.ROUND);
                drawPaint.setStrokeWidth(20);

                oPaint = new Paint();
                oPaint.setDither(true);
                oPaint.setColor(Color.parseColor("#000000"));
                oPaint.setStyle(Paint.Style.STROKE);
                oPaint.setStrokeJoin(Paint.Join.ROUND);
                oPaint.setStrokeCap(Paint.Cap.ROUND);
                oPaint.setStrokeWidth(30);

                drawView.setVisibility(View.GONE);

                detectedContours = new ArrayList<>();
                contourItems = new ArrayList<>();
                arrayPoints = new ArrayList<>();
            }
        });

        drawLayout.setDrawingCacheEnabled(true);

        chosenIndex=0;
    }

    private Bitmap getBitmap() {
        Uri uri = getUri();
        try {
            return utils.getBitmap(getActivity(), uri);
        } catch (IOException e) {
            MessageBox.send("Error", e.getMessage(),getActivity());
            e.printStackTrace();
        }
        return null;
    }

    private Uri getUri() {
        return getArguments().getParcelable(ScanConstants.SELECTED_BITMAP);
    }

    private void setBitmap() {
        new LoadRects().execute();
    }


    private Map<Integer, PointF> findRectangle(Bitmap tempBitmap, int conIdx) {

        finBitmap = tempBitmap;

        Mat src = new Mat();

        Utils.bitmapToMat(tempBitmap, src);

        Mat blurred = src.clone();
        Imgproc.medianBlur(src, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        Imgproc.Canny(gray0, gray0, 0, 0);

        List<MatOfPoint> contours = new ArrayList<>();

        List<Mat> blurredChannel = new ArrayList<>();
        blurredChannel.add(blurred);

        List<Mat> gray0Channel = new ArrayList<>();
        gray0Channel.add(gray0);

        ArrayList<MatOfPoint2f> Curves = new ArrayList<>();

        List<MatOfPoint> rectContours=new ArrayList<>();

        Mat lines0=new Mat();
        Mat lines2=new Mat();
        for (int c = 0; c < 6; c++) {
            contours=new ArrayList<>();
            int ch[] = {c, 0};
            if (c<4) Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));
            int thresholdLevel = 1;
            for (int t = 0; t < thresholdLevel; t++) {
                if (t == 0) {
                    if (c==0){
                        Imgproc.medianBlur(src, gray, 7);
                        Imgproc.cvtColor(gray,gray,Imgproc.COLOR_BGR2HSV);
                        Core.inRange(gray,new Scalar(0, 0, 0),new Scalar(180, 255, 125),gray);
                        Imgproc.Canny(gray, gray, 200, 300);
                        Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                    }else if (c!=4){
                        Imgproc.medianBlur(gray0, gray0, 29);
                        Imgproc.Canny(gray0, gray, 10, 20, 3, true);
                        Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                    }
                } else {
                    Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                            Imgproc.THRESH_BINARY,
                            (src.width() + src.height()) / 200, t);
                }

                if (c==0) Imgproc.HoughLines(gray,lines0, 1, Math.PI/180, 200);
                else if (c==2) Imgproc.HoughLines(gray,lines2, 1, Math.PI/180, 200);


                if (c==3) {
                    gray.setTo(new Scalar(0));
                    for (int j = 0; j < 20; j++) {
                        for (int i = 0; i < lines0.cols(); i++) {
                            double data[] = lines0.get(j, i);
                            if (data != null) {
                                double rho1 = data[0];
                                double theta1 = data[1];
                                double cosTheta = Math.cos(theta1);
                                double sinTheta = Math.sin(theta1);
                                double x0 = cosTheta * rho1;
                                double y0 = sinTheta * rho1;
                                Point pt1 = new Point(x0 + 10000 * (-sinTheta), y0 + 10000 * cosTheta);
                                Point pt2 = new Point(x0 - 10000 * (-sinTheta), y0 - 10000 * cosTheta);
                                Imgproc.line(gray, pt1, pt2, new Scalar(255), 2);
                            }
                        }
                    }
                }else if (c==2){
                    for (int j = 0; j < 20; j++) {
                        for (int i = 0; i < lines2.cols(); i++) {
                            double data[] = lines2.get(j, i);
                            if (data != null) {
                                double rho1 = data[0];
                                double theta1 = data[1];
                                double cosTheta = Math.cos(theta1);
                                double sinTheta = Math.sin(theta1);
                                double x0 = cosTheta * rho1;
                                double y0 = sinTheta * rho1;
                                Point pt1 = new Point(x0 + 10000 * (-sinTheta), y0 + 10000 * cosTheta);
                                Point pt2 = new Point(x0 - 10000 * (-sinTheta), y0 - 10000 * cosTheta);
                                Imgproc.line(gray, pt1, pt2, new Scalar(255), 2);
                            }
                        }
                    }
                }else if (c==0){
                    for (int j = 0; j < 15; j++) {
                        for (int i = 0; i < lines0.cols(); i++) {
                            double data[] = lines0.get(j, i);
                            if (data != null) {
                                double rho1 = data[0];
                                double theta1 = data[1];
                                double cosTheta = Math.cos(theta1);
                                double sinTheta = Math.sin(theta1);
                                double x0 = cosTheta * rho1;
                                double y0 = sinTheta * rho1;
                                Point pt1 = new Point(x0 + 10000 * (-sinTheta), y0 + 10000 * cosTheta);
                                Point pt2 = new Point(x0 - 10000 * (-sinTheta), y0 - 10000 * cosTheta);
                                Imgproc.line(gray, pt1, pt2, new Scalar(255), 2);
                            }
                        }
                    }
                }else if (c==4){
                    gray.setTo(new Scalar(0));
                    for (int j = 0; j < 20; j++) {
                        for (int i = 0; i < lines2.cols(); i++) {
                            double data[] = lines2.get(j, i);
                            if (data != null) {
                                double rho1 = data[0];
                                double theta1 = data[1];
                                double cosTheta = Math.cos(theta1);
                                double sinTheta = Math.sin(theta1);
                                double x0 = cosTheta * rho1;
                                double y0 = sinTheta * rho1;
                                Point pt1 = new Point(x0 + 10000 * (-sinTheta), y0 + 10000 * cosTheta);
                                Point pt2 = new Point(x0 - 10000 * (-sinTheta), y0 - 10000 * cosTheta);
                                Imgproc.line(gray, pt1, pt2, new Scalar(255), 2);
                            }
                        }
                    }
                }else if (c==5){
                    src.convertTo(gray,-1,1.9,0);
                    Imgproc.cvtColor(gray,gray,Imgproc.COLOR_BGR2GRAY);
                    Imgproc.adaptiveThreshold(gray,gray,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,
                            Imgproc.THRESH_BINARY,41,7.0);
                }

                Imgproc.findContours(gray, contours, new Mat(),
                        Imgproc.RETR_LIST
                        , Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours) {

                    MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                    MatOfPoint2f approxCurve = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp, approxCurve,
                            Imgproc.arcLength(temp, true) * 0.02, true);
                    double area = Imgproc.contourArea(approxCurve.clone());
                    MatOfPoint approxf1 = new MatOfPoint();
                    approxCurve.convertTo(approxf1,CvType.CV_32S);

                    if (approxCurve.total() == 4 && area>10000.00 && Imgproc.isContourConvex(approxf1)) {
                        Curves.add(approxCurve);
                        rectContours.add(contour);
                    }
                }
            }
        }

        Utils.matToBitmap(src, finBitmap);
        Mat res;

        ArrayList<Integer> removableIndexes = new ArrayList<>();
        ArrayList<MatOfPoint> nRectContours = new ArrayList<>();
        if (rectContours.size() != 0) {//maxId >= 0 &&

            Collections.sort(rectContours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint o1, MatOfPoint o2) {
                    MatOfPoint2f temp1 = new MatOfPoint2f(o1.toArray());
                    MatOfPoint2f approxCurve1 = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp1, approxCurve1,
                            Imgproc.arcLength(temp1, true) * 0.02, true);

                    MatOfPoint2f temp2 = new MatOfPoint2f(o2.toArray());
                    MatOfPoint2f approxCurve2 = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp2, approxCurve2,
                            Imgproc.arcLength(temp2, true) * 0.02, true);

                    return Integer.valueOf((int)Imgproc.contourArea(approxCurve2.clone())).compareTo((int)Imgproc.contourArea(approxCurve1.clone()));
                }
            });

            Collections.sort(Curves, new Comparator<MatOfPoint2f>() {
                @Override
                public int compare(MatOfPoint2f o1, MatOfPoint2f o2) {
                    return Integer.valueOf((int)Imgproc.contourArea(o2)).compareTo((int)Imgproc.contourArea(o1));
                }
            });

            for (int i = 0; i < Curves.size(); i++) {
                double[] temp_double;
                temp_double = Curves.get(i).get(0, 0);
                PointF p1 = new PointF((float) temp_double[0], (float) temp_double[1]);
                temp_double = Curves.get(i).get(1, 0);
                PointF p2 = new PointF((float) temp_double[0], (float) temp_double[1]);
                temp_double = Curves.get(i).get(2, 0);
                PointF p3 = new PointF((float) temp_double[0], (float) temp_double[1]);
                temp_double = Curves.get(i).get(3, 0);
                PointF p4 = new PointF((float) temp_double[0], (float) temp_double[1]);
                List<PointF> pointFs = new ArrayList<>();

                pointFs.add(p1);
                pointFs.add(p2);
                pointFs.add(p3);
                pointFs.add(p4);

                Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);

                if (orderedPoints != null) {
                    if (p1.x != 0 && p1.y != 0) {
                        if (orderedPoints.size() == 4) arrayPoints.add(orderedPoints);
                        else {
                            removableIndexes.add(i);
                        }
                    } else removableIndexes.add(i);
                }else removableIndexes.add(i);
            }

            for (int j = 0; j < removableIndexes.size(); j++) {
                rectContours.set(removableIndexes.get(j),null);
            }


            for (int i = 0; i < rectContours.size(); i++) {
                if (rectContours.get(i)!=null) nRectContours.add(rectContours.get(i));
            }

            int height=original.getHeight();
            int width=original.getWidth();
            for (int i = 0; i < nRectContours.size(); i++) {
                res = src;
                res.setTo(new Scalar(0, 0, 0));

                Imgproc.drawContours(res, nRectContours, i, new Scalar(255, 255, 255,
                        .8), 8);

                Bitmap bitmap=Bitmap.createBitmap(res.width(),res.height(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(res, bitmap);
                if (width>height)
                    bitmap=Bitmap.createScaledBitmap(bitmap,200,height*200/width,true);
                else bitmap=Bitmap.createScaledBitmap(bitmap,width*200/height,200,true);
                contourItems.add(bitmap);
            }

        }
        detectedContours=nRectContours;

        final Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());//original

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sourceImageView.setImageBitmap(scaledBitmap);
            }
        });

        if (arrayPoints.size() == 0) {
            arrayPoints.add(getOutlinePoints(scaledBitmap));
            if(contours.size()!=0)
                detectedContours.add(contours.get(0));
            else{
                Point ocvPOut1 = new Point(0, 0);
                Point ocvPOut2 = new Point(0, scaledBitmap.getHeight());
                Point ocvPOut3 = new Point(scaledBitmap.getWidth(), scaledBitmap.getHeight());
                Point ocvPOut4 = new Point(scaledBitmap.getWidth(), 0);
                MatOfPoint matOfPoint= new MatOfPoint(ocvPOut1,ocvPOut2,ocvPOut3,ocvPOut4);
                detectedContours.add(matOfPoint);
            }

            contourItems = new ArrayList<>();
            contourItems.add(scaledBitmap);
        }

        return arrayPoints.get(conIdx);
    }

    private MatOfPoint findLargestContour(List<MatOfPoint> contours){
        double maxArea = 0;
        MatOfPoint largestContour=new MatOfPoint();
        MatOfPoint2f approxCurve;
        for (MatOfPoint contour : contours) {
            MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

            double area = Imgproc.contourArea(contour);
            approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(temp, approxCurve,
                    Imgproc.arcLength(temp, true) * 0.02, true);

            if (approxCurve.total() == 4) {

                if (area >= maxArea) {
                    maxArea = area;
                    largestContour = new MatOfPoint(contour);
                }
            }
        }

        return largestContour;
    }



    private List<PointF> getCurve(MatOfPoint contour){
        MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

        MatOfPoint2f approxCurve = new MatOfPoint2f();
        Imgproc.approxPolyDP(temp, approxCurve,
                Imgproc.arcLength(temp, true) * 0.02, true);

        double[] temp_double;
        temp_double = approxCurve.get(0, 0);
        PointF p1 = new PointF((float)temp_double[0],(float) temp_double[1]);
        temp_double = approxCurve.get(1, 0);
        PointF p2 = new PointF((float)temp_double[0],(float) temp_double[1]);
        temp_double = approxCurve.get(2, 0);
        PointF p3 = new PointF((float)temp_double[0],(float) temp_double[1]);
        temp_double = approxCurve.get(3, 0);
        PointF p4 = new PointF((float)temp_double[0],(float) temp_double[1]);
        List<PointF> pointFs= new ArrayList<>();
        pointFs.add(p1);
        pointFs.add(p2);
        pointFs.add(p3);
        pointFs.add(p4);

        return pointFs;
    }

    private Map<Integer, PointF> setDrawnContour(Bitmap bitmap) {
        isDrawing = false;
        imgDraw.setBackgroundColor(Color.TRANSPARENT);

        drawView.setVisibility(View.GONE);

        Map<Integer, PointF> orderedPoints;

        Bitmap tempBitmap = bitmap.copy(bitmap.getConfig(), true);

        Mat src = new Mat();
        Utils.bitmapToMat(tempBitmap, src);

        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Imgproc.dilate(src, src, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10)));
        Imgproc.Canny(src, src, 0, 0);
        Imgproc.dilate(src, src, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10)));

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(),
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        try {
            MatOfPoint largestContour = findLargestContour(contours);
            List<PointF> pointFs=getCurve(largestContour);

            detectedContours.set(chosenIndex, largestContour);
            Imgproc.drawContours(src, detectedContours, chosenIndex, new Scalar(255, 255, 255,
                    .8), 8);
            Imgproc.dilate(src, src, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10)));
            Imgproc.Canny(src, src, 0, 0);
            Imgproc.dilate(src, src, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10)));

            Utils.matToBitmap(src, tempBitmap);

            orderedPoints = orderedValidEdgePoints(pointFs);

            arrayPoints.set(chosenIndex, orderedPoints);

            polygonView.setVisibility(View.VISIBLE);

            drawLayout.destroyDrawingCache();

        }catch (Exception e){
            orderedPoints=getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    private Map<Integer, PointF> addContour(Bitmap tempBitmap) {
        frameLayout.buildDrawingCache();
        Bitmap bitmap = frameLayout.getDrawingCache();
        contourItems.set(chosenIndex, Bitmap.createBitmap(bitmap));
        adapter.notifyDataSetChanged();
        frameLayout.destroyDrawingCache();

        arrayPoints.set(chosenIndex, polygonView.getPoints());

        Map<Integer, PointF> orderedPoints;

        contourItems.add(tempBitmap);

        Bitmap nTemp=tempBitmap.copy(tempBitmap.getConfig(),true);
        Mat src = new Mat();
        Utils.bitmapToMat(nTemp, src);
        Mat res = src.clone();
        src.setTo(new Scalar(0, 0, 0));
        Utils.matToBitmap(src, nTemp);

        Imgproc.cvtColor(res, res, Imgproc.COLOR_BGR2GRAY);

        detectedContours.add(null);

        chosenIndex = contourItems.size() - 1;

        adapter.notifyDataSetChanged();

        orderedPoints=getOutlinePoints(tempBitmap);

        arrayPoints.add(orderedPoints);

        return orderedPoints;
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = null;
        }
        return orderedPoints;
    }

    private class ScanButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            frameLayout.buildDrawingCache();
            int width = frameLayout.getDrawingCache().getWidth();
            int height = frameLayout.getDrawingCache().getHeight();
            Bitmap bitmap;

            if (width>height)
                bitmap = Bitmap.createScaledBitmap(frameLayout.getDrawingCache(),200,height*200/width,true);
            else bitmap = Bitmap.createScaledBitmap(frameLayout.getDrawingCache(),width*200/height,200,true);

            contourItems.set(chosenIndex, Bitmap.createBitmap(bitmap));
            adapter.notifyDataSetChanged();
            frameLayout.destroyDrawingCache();

            arrayPoints.set(chosenIndex, polygonView.getPoints());

            ArrayList<Boolean> checked = adapter.getmChecked();

            ArrayList<Map<Integer, PointF>> checkedPoints = new ArrayList<>();
            for (int i = 0; i < arrayPoints.size(); i++) {
                while (checked.size()<arrayPoints.size())checked.add(false);
                if (checked.get(i)) checkedPoints.add(arrayPoints.get(i));
            }

            if (checkedPoints.size() > 0) {
                new ScanAsyncTask(checkedPoints).execute();
            } else showCheckDialog();
        }
    }

    private class btnAddClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Map<Integer, PointF> pointFs;
            Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
            pointFs = addContour(scaledBitmap);
            polygonView.destroyDrawingCache();
            polygonView.setPoints(pointFs);
            int padding = (int) getResources().getDimension(R.dimen.scanPadding);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(scaledBitmap.getWidth() + 2 * padding, scaledBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;
            polygonView.setLayoutParams(layoutParams);

            for (int i = 0; i < ContourAdapter.mClicked.size(); i++) {
                ContourAdapter.mClicked.set(i, false);
            }
            ContourAdapter.mClicked.add(true);

            rvContour.smoothScrollToPosition(chosenIndex);
        }
    }

    private class imgDrawClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (isDrawing) {
                stopDrawing();
            } else {
                startDrawing();
            }

        }
    }

    private void stopDrawing(){
        isDrawing = false;
        Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
        polygonView.destroyDrawingCache();
        polygonView.setPoints(arrayPoints.get(chosenIndex));
        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(scaledBitmap.getWidth() + 2 * padding, scaledBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;
        polygonView.setLayoutParams(layoutParams);
        polygonView.setVisibility(View.VISIBLE);

        drawView.setVisibility(View.GONE);

        imgDraw.setBackgroundColor(Color.TRANSPARENT);
    }
    private void startDrawing(){
        isDrawing = true;
        polygonView.setVisibility(View.GONE);
        drawView.setVisibility(View.VISIBLE);

        imgDraw.setBackgroundColor(Color.parseColor("#1B333E"));

        ViewTooltip
                .on(getActivity(), drawView)
                .autoHide(true, 4000)
                .corner(30)
                .position(ViewTooltip.Position.BOTTOM)
                .text("Draw a Rectangle Around Your Desired Area!")
                .show();
    }

    private void showErrorDialog() {
        SingleButtonDialogFragment fragment = new SingleButtonDialogFragment(R.string.ok, getString(R.string.cantCrop), "Error", true);
        FragmentManager fm = getActivity().getFragmentManager();
        fragment.show(fm, SingleButtonDialogFragment.class.toString());
    }

    private void showCheckDialog() {
        SingleButtonDialogFragment fragment = new SingleButtonDialogFragment(R.string.ok, getString(R.string.check_item), "Error", true);
        FragmentManager fm = getActivity().getFragmentManager();
        fragment.show(fm, SingleButtonDialogFragment.class.toString());
    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Bitmap scan(Bitmap original,float x1,float y1,float x2,float y2, float x3 , float y3, float x4, float y4){
        float w1= (float) Math.sqrt(pow(x4-x3,2) +  pow(x4 - x3, 2));
        float w2 = (float) Math.sqrt( pow(x2 - x1 , 2) + pow(x2-x1, 2));
        float h1 = (float) Math.sqrt( pow(y2 - y4 , 2) + pow(y2 - y4, 2));
        float h2 = (float) Math.sqrt( pow(y1 - y3 , 2) + pow(y1-y3, 2));

        float maxWidth = (w1 < w2) ? w1 : w2;
        float maxHeight = (h1 < h2) ? h1 : h2;

        Mat dst= Mat.zeros((int)maxHeight,(int)maxWidth,CvType.CV_8UC3);

        List<Point> startPts=new ArrayList<>();
        startPts.add(new Point(0,0));
        startPts.add(new Point(maxWidth-1,0));
        startPts.add(new Point(0,maxHeight-1));
        startPts.add(new Point(maxWidth-1,maxHeight-1));

        List<Point> dstPts=new ArrayList<>();
        dstPts.add(computePoint(x1,y1));
        dstPts.add(computePoint(x2,y2));
        dstPts.add(computePoint(x3,y3));
        dstPts.add(computePoint(x4,y4));

        Mat startM = Converters.vector_Point2f_to_Mat(startPts);
        Mat dstM = Converters.vector_Point2f_to_Mat(dstPts);

        Mat trans=Imgproc.getPerspectiveTransform(dstM,startM);

        Mat src=new Mat();
        Utils.bitmapToMat(original,src);
        Imgproc.warpPerspective(src,dst,trans,dst.size());

        Bitmap result = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst,result);
        return result;
    }

    private Point computePoint(float p1, float p2) {
        Point pt=new Point();
        pt.x = p1;
        pt.y = p2;
        return pt;
    }
    private Bitmap getScannedBitmap(Bitmap original, Map<Integer, PointF> points) {

        int width=original.getWidth();
        int height=original.getHeight();
        if (width > 3000 || height > 3000) {
            if (width > height)
                original = Bitmap.createScaledBitmap(original, 3000, height * 3000 / width, true);
            else
                original = Bitmap.createScaledBitmap(original, width * 3000 / height, 3000, true);
        }

        float xRatio = (float) original.getWidth() / sourceImageView.getWidth();
        float yRatio = (float) original.getHeight() / sourceImageView.getHeight();

        float x1 = (points.get(0).x) * xRatio;
        float x2 = (points.get(1).x) * xRatio;
        float x3 = (points.get(2).x) * xRatio;
        float x4 = (points.get(3).x) * xRatio;
        float y1 = (points.get(0).y) * yRatio;
        float y2 = (points.get(1).y) * yRatio;
        float y3 = (points.get(2).y) * yRatio;
        float y4 = (points.get(3).y) * yRatio;
        Log.d("", "Points(" + x1 + "," + y1 + ")(" + x2 + "," + y2 + ")(" + x3 + "," + y3 + ")(" + x4 + "," + y4 + ")");

        return scan(original,x1,y1,x2,y2,x3,y3,x4,y4);
    }

    private class ScanAsyncTask extends AsyncTask<Void, Void, ArrayList<Bitmap>> {

        private ArrayList<Map<Integer, PointF>> points;

        private ScanAsyncTask(ArrayList<Map<Integer, PointF>> points) {
            this.points = points;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(getString(R.string.scanning));
        }

        @Override
        protected ArrayList<Bitmap> doInBackground(Void... params) {
            ArrayList<Uri> uri = new ArrayList<>();

            File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                    + "/Android/data/"
                    + getActivity().getPackageName()
                    + "/Files");
            deleteRecursive(mediaStorageDir);

            Bitmap scannedBitmap;
            for (int i = 0; i < points.size(); i++) {
                scannedBitmap=getScannedBitmap(original, points.get(i));
                uri.add(utils.getUri(getActivity(), scannedBitmap,70));
            }

            scanner.onScanFinish(uri);
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Bitmap> bitmaps) {
            dismissDialog();
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                child.delete();
    }

    protected void showProgressDialog(String message) {
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
    }

    class SketchSheetView extends View {

        public SketchSheetView(Context context) {

            super(context);
            Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
            drawBitmap = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(), Bitmap.Config.RGB_565);

            drawCanvas = new Canvas(drawBitmap);

            this.setBackgroundColor(Color.TRANSPARENT);
        }

        private ArrayList<DrawingClass> DrawingClassArrayList = new ArrayList<>();

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            DrawingClass pathWithPaint = new DrawingClass();

            drawCanvas.drawPath(drawPath2, drawPaint);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                drawPath2.moveTo(event.getX(), event.getY());
                drawPath2.lineTo(event.getX(), event.getY());

            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

                drawPath2.lineTo(event.getX(), event.getY());


                pathWithPaint.setPath(drawPath2);

                pathWithPaint.setPaint(drawPaint);

                DrawingClassArrayList.add(pathWithPaint);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                ThreadA a = new ThreadA();
                a.main();

            }
            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (DrawingClassArrayList.size() > 0) {

                canvas.drawPath(
                        DrawingClassArrayList.get(DrawingClassArrayList.size() - 1).getPath(),
                        oPaint);

                canvas.drawPath(
                        DrawingClassArrayList.get(DrawingClassArrayList.size() - 1).getPath(),
                        DrawingClassArrayList.get(DrawingClassArrayList.size() - 1).getPaint());
            }

        }
    }

    private class ThreadA {
        public void main() {
            ThreadB b = new ThreadB();
            b.start();

            synchronized (b) {
                try {
                    Log.d("WAITING", "Waiting for b to complete...");
                    b.wait();
                } catch (InterruptedException e) {
                    MessageBox.send("Error", e.getMessage(),getActivity());
                    e.printStackTrace();
                }

                Log.d("Total Time", String.valueOf(b.total));

                frameLayout.buildDrawingCache();
                Bitmap cache = frameLayout.getDrawingCache();

                contourItems.set(chosenIndex, Bitmap.createBitmap(cache));
                adapter.notifyDataSetChanged();
                frameLayout.destroyDrawingCache();
            }
        }
    }

    private class ThreadB extends Thread {
        int total;

        @Override
        public void run() {
            synchronized (this) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sourceImageView.setVisibility(View.GONE);
                        drawLayout.buildDrawingCache();
                        Bitmap tempBitmap = drawLayout.getDrawingCache();

                        Map<Integer, PointF> pointFs = setDrawnContour(tempBitmap.copy(tempBitmap.getConfig(), true));
                        if (pointFs==null){
                            pointFs=getOutlinePoints(tempBitmap);
                        }
                        polygonView.setPoints(pointFs);
                        polygonView.setVisibility(View.VISIBLE);

                        Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
                        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(scaledBitmap.getWidth() + 2 * padding, scaledBitmap.getHeight() + 2 * padding);
                        layoutParams.gravity = Gravity.CENTER;
                        polygonView.setLayoutParams(layoutParams);

                        drawPath2.reset();
                        sourceImageView.setVisibility(View.VISIBLE);
                    }
                });
                notify();
            }
        }
    }
    ///////////////

    private class DrawingClass {

        Path DrawingClassPath;
        Paint DrawingClassPaint;

        public Path getPath() {
            return DrawingClassPath;
        }

        public void setPath(Path path) {
            this.DrawingClassPath = path;
        }


        private Paint getPaint() {
            return DrawingClassPaint;
        }

        private void setPaint(Paint paint) {
            this.DrawingClassPaint = paint;
        }
    }

    private class LoadRects extends AsyncTask<Void, Void, Void> {
        Bitmap tempBitmap;
        Map<Integer, PointF> pointFs;

        @Override
        protected void onPreExecute() {
            Log.d("START", "About to Start Loading Rects");
            Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
            sourceImageView.setImageBitmap(scaledBitmap);
            tempBitmap = ((BitmapDrawable) sourceImageView.getDrawable()).getBitmap();

            rvContour = (RecyclerView) view.findViewById(R.id.rvContours);
            contoursLayout = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
            rvContour.setLayoutManager(contoursLayout);
        }

        @Override
        protected Void doInBackground(Void... params) {
            pointFs = findRectangle(tempBitmap, 0);
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            Log.d("FINISH", "Rects Found");
            Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());

            polygonView.setVisibility(View.VISIBLE);
            polygonView.setPoints(pointFs);
            int padding = (int) getResources().getDimension(R.dimen.scanPadding);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(scaledBitmap.getWidth() + 2 * padding, scaledBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;
            polygonView.setLayoutParams(layoutParams);

            adapter = new ContourAdapter(getActivity(), contourItems);

            adapter.setClickListener(ScanFragment.this);
            rvContour.setAdapter(adapter);

            ContourAdapter.mClicked.set(0, true);

            view.setVisibility(View.VISIBLE);


            if (contourItems.size()<3){
                ViewTooltip
                        .on(getActivity(), imgDraw)
                        .autoHide(true, 4000)
                        .corner(30)
                        .position(ViewTooltip.Position.TOP)
                        .text("Specify Your Desired Area By Your Fingertips!")
                        .show();
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (isDrawing) {
            stopDrawing();
        }

        frameLayout.buildDrawingCache();
        int width = frameLayout.getDrawingCache().getWidth();
        int height = frameLayout.getDrawingCache().getHeight();
        Bitmap bitmap;

        if (width>height)
            bitmap = Bitmap.createScaledBitmap(frameLayout.getDrawingCache(),200,height*200/width,true);
        else bitmap = Bitmap.createScaledBitmap(frameLayout.getDrawingCache(),width*200/height,200,true);

        contourItems.set(chosenIndex, Bitmap.createBitmap(bitmap));
        adapter.notifyDataSetChanged();
        frameLayout.destroyDrawingCache();

        arrayPoints.set(chosenIndex, polygonView.getPoints());
        chosenIndex = position;
        Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
        polygonView.destroyDrawingCache();
        polygonView.setPoints(arrayPoints.get(position));
        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(scaledBitmap.getWidth() + 2 * padding, scaledBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;
        polygonView.setLayoutParams(layoutParams);

        for (int i = 0; i < ContourAdapter.mClicked.size(); i++) {
            ContourAdapter.mClicked.set(i, false);
        }
        ContourAdapter.mClicked.set(position, true);
    }
}
package ir.danial.multiscanner;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;

class ImageFilter {

    static Bitmap applyClahe(Bitmap bitmap){
        CLAHE clahe=Imgproc.createCLAHE(3,new Size(8,8));
        Mat src=new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap,src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2Lab);
        List<Mat> lab=new ArrayList<>();
        Core.split(src,lab);
        clahe.apply(lab.get(0),lab.get(0));
        Core.merge(lab,src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_Lab2BGR);
        Utils.matToBitmap(src,bitmap);
        return bitmap;
    }

    static Bitmap applyMagic(Bitmap bitmap){
        Mat src= new Mat();
        Utils.bitmapToMat(bitmap,src);
        src.convertTo(src,-1,1.9,-80);
        Utils.matToBitmap(src,bitmap);
        return bitmap;
    }

    static Bitmap applyGray(Bitmap bitmap){
        Mat src= new Mat();
        Utils.bitmapToMat(bitmap,src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2GRAY);
        Utils.matToBitmap(src,bitmap);
        return bitmap;
    }


    static Bitmap applyBW(Bitmap bitmap){
        Mat src= new Mat();
        Utils.bitmapToMat(bitmap,src);
        src.convertTo(src,-1,1.9,0);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(src,src,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY,41,7.0);
        Utils.matToBitmap(src,bitmap);
        return bitmap;
    }

    static Bitmap applyBW2(Bitmap bitmap){
        Mat src= new Mat();
        Utils.bitmapToMat(bitmap,src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(src,src,30,255,Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        Utils.matToBitmap(src,bitmap);
        return bitmap;
    }

    static Bitmap applyWhiteboard(Bitmap bitmap){
        Mat src= new Mat();
        Utils.bitmapToMat(bitmap,src);

        src.convertTo(src,-1,1.9,-80);

        Mat dst=new Mat();
        Imgproc.cvtColor(src,src,Imgproc.COLOR_RGBA2RGB);
        Imgproc.bilateralFilter(src,dst,9, 80, 80, Core.BORDER_DEFAULT);

        Mat gray=new Mat();
        Imgproc.cvtColor(dst,gray,Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(gray,gray,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,29,0);//11,2

        Size size=new Size(5,5);
        Mat element = Imgproc.getStructuringElement(MORPH_ELLIPSE,size);
        Imgproc.morphologyEx(gray,gray,Imgproc.CV_MOP_CLOSE,element);

        Mat white=new Mat(gray.size(), CV_8UC3,new Scalar(255,255,255));
        Core.bitwise_not(gray,gray);
        Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 3);
        src.copyTo(white,gray);

        white.convertTo(white,-1,1.9,-80);

        Imgproc.threshold(white,white,127,255,Imgproc.THRESH_BINARY);

        Utils.matToBitmap(white,bitmap);
        return bitmap;
    }

}



package ir.danial.multiscanner.Adapters;

/**
 * Created by Danial on 4/26/2018.
 */

import java.util.ArrayList;
import java.util.Arrays;
import  java.util.List;
import  android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import  android.util.SparseBooleanArray;
import  android.view.LayoutInflater;
import  android.view.View;
import  android.view.ViewGroup;
import  android.widget.ArrayAdapter;
import  android.widget.ImageView;
import  android.widget.TextView;

import ir.danial.multiscanner.R;


public class  DirAdapter extends ArrayAdapter<String> {

    private LayoutInflater inflater;

    private List<String> DataList;
    private List<String> filePaths;
    private List<Bitmap> bitmapsResized;

    private  SparseBooleanArray mSelectedItemsIds;

    private CropClickListener cropClickListener = null;
    private LiveClickListener liveClickListener = null;

    // Constructor for get Context and  list
    public  DirAdapter(Context context, int resourceId,  String[] list, String[] paths
            ,CropClickListener cropListener,LiveClickListener liveListener) {
        super(context,  resourceId, list);

        mSelectedItemsIds = new  SparseBooleanArray();

        DataList= new ArrayList<>(Arrays.asList(list));
        filePaths= new ArrayList<>(Arrays.asList(paths));
        inflater =  LayoutInflater.from(context);
        cropClickListener = cropListener;
        liveClickListener=liveListener;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap pdf=decodeSampledBitmapFromResource(context.getResources(),R.id.pdf,100,100);

        bitmapsResized=new ArrayList<>();
        for (String filePath: filePaths) {
            if (!filePath.endsWith("pdf")) {
                bitmapsResized.add(decodeSampledBitmapFromPath(filePath,100,100));
            }else {
                bitmapsResized.add(pdf);
            }
        }
    }

    // Container Class for item
    private class ViewHolder {
        TextView tvTitle;
        ImageView imgPhoto;
        ImageView imgCrop;
        CardView cardPhoto;
        ImageView imgLive;
    }

    public View getView(final int position, View view,@NonNull ViewGroup parent) {

        final ViewHolder  holder;
        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.photo_item,parent, false);
            holder.tvTitle = (TextView)  view.findViewById(R.id.txtTitle);
            holder.imgPhoto = (ImageView)  view.findViewById(R.id.imgPhoto);
            holder.imgCrop=(ImageView) view.findViewById(R.id.imgCrop);
            holder.cardPhoto=(CardView)view.findViewById(R.id.cardPhoto);
            holder.imgLive=(ImageView)view.findViewById(R.id.imgLive);

            view.setTag(holder);
        } else {
            holder = (ViewHolder)  view.getTag();
        }
        // Capture position and set to the  TextViews
        holder.tvTitle.setText(DataList.get(position));

        // Capture position and set to the  ImageView
        if (DataList.get(position).endsWith("pdf")){
            holder.imgCrop.setVisibility(View.GONE);
            holder.imgLive.setVisibility(View.GONE);
            holder.imgPhoto.setImageResource(R.drawable.pdf);
            holder.imgPhoto.setScaleType(ImageView.ScaleType.FIT_XY);
        }else{
            holder.imgCrop.setVisibility(View.VISIBLE);
            holder.imgLive.setVisibility(View.VISIBLE);
            holder.imgPhoto.setImageBitmap(bitmapsResized.get(position));
            holder.imgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        holder.imgCrop.setTag(position);
        holder.imgCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cropClickListener != null)
                    cropClickListener.onCropClick((Integer) v.getTag());
            }
        });

        holder.imgLive.setTag(position);
        holder.imgLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(liveClickListener != null)
                    liveClickListener.onLiveClick((Integer) v.getTag());
            }
        });

        if (mSelectedItemsIds.get(position)) {
            holder.cardPhoto.setCardBackgroundColor(Color.LTGRAY);
        } else {
            holder.cardPhoto.setCardBackgroundColor(Color.WHITE);
        }

        return view;
    }

    public void remove(String  object,int index) {

        DataList.remove(object);
        filePaths.remove(index);
        bitmapsResized.remove(index);
        notifyDataSetChanged();

    }

    // get List after update or delete

    public  List<String> getDataList() {
        return DataList;
    }

    public  List<String> getBitmaps() {
        return filePaths;
    }

    public void  toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));

    }

    // Remove selection after unchecked
    public void  removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    // Item checked on selection
    public void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, value);
        else
            mSelectedItemsIds.delete(position);
        notifyDataSetChanged();
    }

    public void selectAll(){
        for (int i=0;i<mSelectedItemsIds.size();i++){
            mSelectedItemsIds.put(i,true);
        }
    }

    // Get number of selected item
    public int  getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    public  SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    public interface CropClickListener {
        void onCropClick(int position);
    }

    public interface LiveClickListener {
        void onLiveClick(int position);
    }

    private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private static Bitmap decodeSampledBitmapFromPath(String path,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
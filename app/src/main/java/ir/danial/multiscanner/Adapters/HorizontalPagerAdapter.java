package ir.danial.multiscanner.Adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ir.danial.multiscanner.R;
import ir.danial.multiscanner.Utils;

/**
 * Created by Danial on 4/24/2018.
 */

public class HorizontalPagerAdapter extends RecyclerView.Adapter<HorizontalPagerAdapter.ViewHolder> {

    private ItemClickListener mClickListener;

    private List<Uri> mUris;
    private List<Uri> tUris;
    private int itemHeight;
    private Context context;
    // data is passed into the constructor
    public HorizontalPagerAdapter(Context context, List<Uri> uris, List<Uri> transformedUris) {
        this.context=context;

        mUris=new ArrayList<>(uris);
        tUris=new ArrayList<>(transformedUris);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        Activity context = (Activity) recyclerView.getContext();

        Point windowDimensions = new Point();
        context.getWindowManager().getDefaultDisplay().getSize(windowDimensions);
        itemHeight = Math.round(windowDimensions.y * 0.6f);
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.item_pager, parent, false);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                itemHeight);

        v.setLayoutParams(params);

        return new ViewHolder(v);
    }

    // binds the data to the view and textview in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        //holder.imageView.setImageBitmap(mBitmaps.get(position));

        if (tUris.get(position)==null)
        Picasso.get().load(mUris.get(position)).fit().centerInside().into(holder.imageView);
        else Picasso.get().load(tUris.get(position)).fit().centerInside().into(holder.imageView);

    }
    Bitmap getBitmap(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }
    // total number of rows
    @Override
    public int getItemCount() {
        return mUris.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imageView;

        ViewHolder(final View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.pagerImg);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    public Uri getItem(int id) {
        return mUris.get(id);
    }

    public void setItem(int id,Uri bitmap){
        tUris.set(id,bitmap);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
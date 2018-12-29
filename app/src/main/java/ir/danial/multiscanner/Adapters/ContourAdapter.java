package ir.danial.multiscanner.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import ir.danial.multiscanner.R;

public class ContourAdapter extends RecyclerView.Adapter<ContourAdapter.ViewHolder> {
    private Context context;

    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    private List<Bitmap> mBitmaps;
    private ArrayList<Boolean> mChecked;
    public static ArrayList<Boolean> mClicked;


    // data is passed into the constructor
    public ContourAdapter(Context context, List<Bitmap> bitmaps) {
        this.context=context;
        this.mInflater = LayoutInflater.from(context);
        this.mBitmaps=bitmaps;

        mChecked = new ArrayList<>(mBitmaps.size());
        for(int i =0;i<mBitmaps.size();i++){
            mChecked.add(false);
        }

        mClicked = new ArrayList<>(mBitmaps.size());
        for(int i =0;i<mBitmaps.size();i++){
            mClicked.add(false);
        }
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_contour, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // binds the data to the view and textview in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.imageView.setImageBitmap(mBitmaps.get(position));

        if (mChecked.size()<=position){
            mChecked.add(false);
        }

        if (mClicked.size()<=position){
            mClicked.add(false);
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setFocusable(false);
        holder.checkBox.setChecked(mChecked.get(position));
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mChecked.set(position, isChecked);
            }
        });

        if (mClicked.get(position)){
            holder.imageView.setBackgroundColor(Color.parseColor("#AA3939"));
        }else holder.imageView.setBackgroundColor(Color.GRAY);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mBitmaps.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView imageView;
        public CheckBox checkBox;

        public ViewHolder(final View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.conImg);
            checkBox = (CheckBox) itemView.findViewById(R.id.conCheck);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    public Bitmap getItem(int id) {
        return mBitmaps.get(id);
    }

    public ArrayList<Boolean> getmChecked(){
        return mChecked;
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
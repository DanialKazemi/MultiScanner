package ir.danial.multiscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import ir.danial.multiscanner.Adapters.HorizontalPagerAdapter;
import com.yarolegovich.discretescrollview.DiscreteScrollView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;


public class ResultFragment extends Fragment {

    private View view;
    Button btnDone;
    private Bitmap original;
    Button btnOriginal;
    Button btnClahe;
    Button btnMagic;
    Button btnWhiteboard;
    Button btnGray;
    Button btnBW;
    Button btnBW2;
    ImageView imgRotate;

    private TextView txtNumber;
    private Bitmap transformed;
    private static ProgressDialogFragment progressDialogFragment;

    private ArrayList<Uri> originalUris;
    private ArrayList<Uri> transformedUris;

    DiscreteScrollView discreteScrollView;
    HorizontalPagerAdapter hicvpAdapter;
    int idx=0;

    Utils utils=new Utils();

    public ResultFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.result_layout, container,false);
        init();
        return view;
    }

    private void init() {
        btnOriginal = (Button) view.findViewById(R.id.btnOriginal);
        btnOriginal.setOnClickListener(new OriginalButtonClickListener());
        btnClahe=(Button)view.findViewById(R.id.btnClahe);
        btnClahe.setOnClickListener(new ClaheButtonClickListener());
        btnMagic = (Button) view.findViewById(R.id.btnMagic);
        btnMagic.setOnClickListener(new MagicColorButtonClickListener());
        btnWhiteboard=(Button)view.findViewById(R.id.btnWhiteboard);
        btnWhiteboard.setOnClickListener(new WhiteboardButtonClickListener());
        btnGray = (Button) view.findViewById(R.id.btnGray);
        btnGray.setOnClickListener(new GrayButtonClickListener());
        btnBW = (Button) view.findViewById(R.id.btnBW);
        btnBW.setOnClickListener(new BWButtonClickListener());
        btnBW2 = (Button) view.findViewById(R.id.btnBW2);
        btnBW2.setOnClickListener(new BW2ButtonClickListener());
        imgRotate=(ImageView)view.findViewById(R.id.imgRotate);
        imgRotate.setOnClickListener(new RotateClickListener());
        txtNumber=(TextView)view.findViewById(R.id.txtNumber);

        getBitmaps();

        btnDone = (Button) view.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new DoneButtonClickListener());

    }

    private void getBitmaps() {
        originalUris=new ArrayList<>();
        originalUris = getUris();
        transformedUris=new ArrayList<>();
        for (int i=0;i<originalUris.size();i++) transformedUris.add(null);

        try {
            discreteScrollView = (DiscreteScrollView) view.findViewById(R.id.hicvp);

            hicvpAdapter=new HorizontalPagerAdapter(getActivity(),originalUris,transformedUris);

            discreteScrollView.setAdapter(hicvpAdapter);
            discreteScrollView.scrollToPosition(0);
            discreteScrollView.addOnItemChangedListener(new DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder>() {
                @Override
                public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition) {
                    try {
                        original=utils.getBitmap(getActivity(),originalUris.get(adapterPosition));
                    }catch (IOException e){
                        MessageBox.send("Error", e.getMessage(),getActivity());
                    }
                    idx=adapterPosition;
                    txtNumber.setText(idx+1 +" / " + originalUris.size());
                }
            });

            original = utils.getBitmap(getActivity(),originalUris.get(0));

        } catch (IOException e) {
            MessageBox.send("Error", e.getMessage(),getActivity());
            e.printStackTrace();
        }
    }

    private ArrayList<Uri> getUris() {
        return getArguments().getParcelableArrayList(ScanConstants.SCANNED_RESULT);
    }

    private class DoneButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            showProgressDialog(getResources().getString(R.string.loading));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent data = new Intent();

                        ArrayList<Uri> uris = new ArrayList<>();

                        for (int i=0;i<originalUris.size();i++){
                            if (transformedUris.get(i)==null) uris.add(originalUris.get(i));
                            else uris.add(transformedUris.get(i));
                        }

                        data.putExtra(ScanConstants.SCANNED_RESULT, uris);

                        getActivity().setResult(Activity.RESULT_OK, data);
                        original.recycle();
                        System.gc();

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dismissDialog();
                                getActivity().finish();
                            }
                        });

                    } catch (Exception e) {
                        MessageBox.send("Error", e.getMessage(),getActivity());
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private class BWButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {

            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed=null;
                        transformed=ImageFilter.applyBW(original);
                    } catch (final OutOfMemoryError e) {
                        MessageBox.send("Error", e.getMessage(),getActivity());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = Bitmap.createBitmap(original);
                                transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                            hicvpAdapter.setItem(idx,transformedUris.get(idx));
                            hicvpAdapter.notifyDataSetChanged();

                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class BW2ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {

            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed=null;
                        transformed=ImageFilter.applyBW2(original);
                    } catch (final OutOfMemoryError e) {
                        MessageBox.send("Error", e.getMessage(),getActivity());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = Bitmap.createBitmap(original);
                                transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                            hicvpAdapter.setItem(idx,transformedUris.get(idx));
                            hicvpAdapter.notifyDataSetChanged();

                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class MagicColorButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {

            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed=null;
                        transformed=ImageFilter.applyMagic(original);
                    } catch (final OutOfMemoryError e) {
                        MessageBox.send("Error", e.getMessage(),getActivity());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = Bitmap.createBitmap(original);
                                transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                            hicvpAdapter.setItem(idx,transformedUris.get(idx));
                            hicvpAdapter.notifyDataSetChanged();
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class WhiteboardButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {

            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed=null;
                        transformed=ImageFilter.applyWhiteboard(original);
                    } catch (final OutOfMemoryError e) {
                        MessageBox.send("Error", e.getMessage(),getActivity());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = Bitmap.createBitmap(original);
                                transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                            hicvpAdapter.setItem(idx,transformedUris.get(idx));
                            hicvpAdapter.notifyDataSetChanged();

                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class ClaheButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {

            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed=null;
                        transformed = ImageFilter.applyClahe(original);
                    } catch (final OutOfMemoryError e) {
                        MessageBox.send("Error", e.getMessage(),getActivity());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = Bitmap.createBitmap(original);
                                transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                            hicvpAdapter.setItem(idx,transformedUris.get(idx));
                            hicvpAdapter.notifyDataSetChanged();

                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class GrayButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {

            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed=null;
                        transformed=ImageFilter.applyGray(original);
                    } catch (final OutOfMemoryError e) {
                        MessageBox.send("Error", e.getMessage(),getActivity());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = Bitmap.createBitmap(original);
                                transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                            hicvpAdapter.setItem(idx,transformedUris.get(idx));
                            hicvpAdapter.notifyDataSetChanged();

                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class OriginalButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                showProgressDialog(getResources().getString(R.string.applying_filter));

                transformed=null;
                transformed = Bitmap.createBitmap(original);

                transformedUris.set(idx,utils.getUri(getActivity(),transformed,100));
                hicvpAdapter.setItem(idx,originalUris.get(idx));
                hicvpAdapter.notifyDataSetChanged();

                dismissDialog();
            } catch (OutOfMemoryError e) {
                MessageBox.send("Error", e.getMessage(),getActivity());
                e.printStackTrace();
                dismissDialog();
            }
        }
    }

    private class RotateClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        original.recycle();
                        original=null;

                        if (transformedUris.get(idx)!=null)
                        original = rotateBitmap(utils.getBitmap(getActivity(),transformedUris.get(idx)));
                        else original = rotateBitmap(utils.getBitmap(getActivity(),originalUris.get(idx)));
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }catch (IOException e){

                        e.printStackTrace();
                        dismissDialog();
                        onClick(v);
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transformedUris.set(idx,utils.getUri(getActivity(),original,100));

                            hicvpAdapter.setItem(idx,transformedUris.get(idx));
                            hicvpAdapter.notifyDataSetChanged();

                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    public Bitmap rotateBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        Mat mat= new Mat(width,height,CvType.CV_8U);
        org.opencv.android.Utils.bitmapToMat(original,mat);
        Core.rotate(mat,mat,0);
        Bitmap rotatedBitmap= Bitmap.createBitmap(mat.width(),mat.height(), Bitmap.Config.ARGB_4444);
        org.opencv.android.Utils.matToBitmap(mat,rotatedBitmap);

        return rotatedBitmap;
    }

    protected synchronized void showProgressDialog(String message) {
        if (progressDialogFragment != null && progressDialogFragment.isVisible()) {
            // Before creating another loading dialog, close all opened loading dialogs (if any)
            progressDialogFragment.dismissAllowingStateLoss();
        }
        progressDialogFragment = null;
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected synchronized void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
    }



}
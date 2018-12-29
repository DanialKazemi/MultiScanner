package ir.danial.multiscanner;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class PickImageFragment extends Fragment {

    private View view;
    private Uri fileUri;
    private IScanner scanner;
    Utils utils=new Utils();

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
        view = inflater.inflate(R.layout.pick_image_fragment, container,false);
        init();
        return view;
    }

    private void init() {

        AVLoadingIndicatorView avi=(AVLoadingIndicatorView)view.findViewById(R.id.avi);
        avi.show();
        
        if (isIntentPreferenceSet()) {
            handleIntentPreference();
        } else {
            getActivity().finish();
        }
    }

    private void clearTempImages() {
        try {
            File tempFolder = new File(ScanConstants.IMAGE_PATH);
            for (File f : tempFolder.listFiles())
                f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIntentPreference() {
        int preference = getIntentPreference();
        if (preference == ScanConstants.OPEN_CAMERA) {
            openCamera();
        } else if (preference == ScanConstants.OPEN_MEDIA) {
            openMediaContent();
        }else if (preference==ScanConstants.GET_MEDIA){
            Bitmap bitmap=BitmapFactory.decodeFile(ScanActivity.galleryPath);
            postImagePick(bitmap);
        }
    }

    private boolean isIntentPreferenceSet() {
        int preference = getArguments().getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
        return preference != 0;
    }

    private int getIntentPreference() {
        return  getArguments().getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
    }

    public void openMediaContent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, ScanConstants.PICKFILE_REQUEST_CODE);
    }

    public void openCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File file = createImageFile();
        boolean isDirectoryCreated = file.getParentFile().mkdirs();
        Log.d("", "openCamera: isDirectoryCreated: " + isDirectoryCreated);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri tempFileUri = FileProvider.getUriForFile(getActivity().getApplicationContext(),
                    "ir.danial.multiscanner.provider", // As defined in Manifest
                    file);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri);
        } else {
            Uri tempFileUri = Uri.fromFile(file);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri);
        }
        startActivityForResult(cameraIntent, ScanConstants.START_CAMERA_REQUEST_CODE);
    }

    private File createImageFile() {
        clearTempImages();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new
                Date());
        File file = new File(ScanConstants.IMAGE_PATH, "IMG_" + timeStamp +
                ".jpg");
        fileUri = Uri.fromFile(file);
        return file;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("", "onActivityResult" + resultCode);

        new resultTask(requestCode,resultCode, data).execute();
    }

    private class resultTask extends AsyncTask<Void,Void,Void>{

        int requestCode;
        int resultCode;
        Intent data;

        resultTask(int requestCode, int resultCode, Intent data){
            this.requestCode=requestCode;
            this.resultCode=resultCode;
            this.data=data;
            //pd = new ProgressDialog(getActivity());
        }
        @Override
        protected void onPreExecute(){
            //pd.setMessage(getResources().getString(R.string.loading));
            //pd.setCancelable(false);
            //pd.show();
        }

        @Override
        protected Void doInBackground(Void... params){
            Bitmap bitmap = null;
            if (resultCode == Activity.RESULT_OK) {
                try {
                    switch (requestCode) {
                        case ScanConstants.START_CAMERA_REQUEST_CODE:
                            bitmap = getBitmap(fileUri);
                            break;

                        case ScanConstants.PICKFILE_REQUEST_CODE:
                            bitmap = getBitmap(data.getData());
                            break;
                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity(),"No such file or directory",Toast.LENGTH_LONG).show();
                    getActivity().finish();
                    e.printStackTrace();
                }
            } else {
                getActivity().finish();
            }
            if (bitmap != null) {
                postImagePick(bitmap);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void res){
            //pd.dismiss();
        }


    }

    protected void postImagePick(Bitmap bitmap) {
        Uri uri = utils.getUri(getActivity(), bitmap,100);

        bitmap.recycle();
        scanner.onBitmapSelect(uri);
    }

    private Bitmap getBitmap(Uri selectedimg) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled=false;
        AssetFileDescriptor fileDescriptor = null;
        fileDescriptor =
                getActivity().getContentResolver().openAssetFileDescriptor(selectedimg, "r");
        Bitmap original
                = BitmapFactory.decodeFileDescriptor(
                fileDescriptor.getFileDescriptor(), null, options);

        //
        File curFile=null;
        try {
            curFile = new File(getFilePath(getActivity().getApplicationContext(),selectedimg)); //image file
        }catch (Exception e){
            Log.e("TAG", "Failed to get image file", e);
        }
        try {
            ExifInterface exif = new ExifInterface(curFile.getAbsolutePath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(rotation);
            Matrix matrix = new Matrix();
            if (rotation != 0f) {matrix.preRotate(rotationInDegrees);}
            original = Bitmap.createBitmap(original,0,0, original.getWidth(), original.getHeight(), matrix, true);
        }catch(IOException ex){
            Log.e("TAG", "Failed to get Exif data", ex);
            original=rotateBitmap(original);
        }

        //

        return original;
    }
    private Bitmap rotateBitmap(Bitmap original) {
        if (original != null) {
            if (original.getHeight() < original.getWidth()) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap scaleBitmap = Bitmap.createScaledBitmap(original, original.getWidth(), original.getHeight(), true);

                //rotate
                original =  Bitmap.createBitmap(scaleBitmap, 0, 0, scaleBitmap.getWidth(), scaleBitmap.getHeight(), matrix, true);
            }
        }

        return original;
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }




    @SuppressLint("NewApi")
    public static String getFilePath(Context context, Uri uri) throws URISyntaxException {
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) { //Build.VERSION.SDK_INT >= 19
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
package ir.danial.multiscanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

import com.wang.avi.AVLoadingIndicatorView;

import ir.danial.multiscanner.Adapters.DirAdapter;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 99;
    private static final int LIVE_CODE = 98;
    int PERMISSION_ALL=1;

    FloatingActionButton scanButton;
    private FloatingActionButton cameraButton;
    private FloatingActionButton mediaButton;
    private FloatingActionButton flowButton;
    private FloatingActionButton fabOpen;

    private ImageView imgNone;

    ArrayList<Image> pdfImages;

    Toolbar mTopToolbar;

    String galleryPath;
    DirAdapter dirAdapter;

    String[] fileNames=null;
    String[] filePaths=null;

    ArrayList<String> titles;

    boolean isFABOpen=false;

    ListView imageGallery;

    String[] PERMISSIONS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mTopToolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(mTopToolbar);

        init();

        PERMISSIONS=new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};


        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }else new listTask().execute();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void init() {
        imageGallery = (ListView) findViewById(R.id.imagegallery);
        fabOpen=(FloatingActionButton) findViewById(R.id.fabOpen);
        scanButton = (FloatingActionButton) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new ScanButtonClickListener());
        cameraButton = (FloatingActionButton) findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new ScanButtonClickListener(ScanConstants.OPEN_CAMERA));
        mediaButton = (FloatingActionButton) findViewById(R.id.mediaButton);
        mediaButton.setOnClickListener(new ScanButtonClickListener(ScanConstants.OPEN_MEDIA));
        flowButton = (FloatingActionButton) findViewById(R.id.flowButton);
        flowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                Intent i = new Intent(getApplicationContext(), LiveActivity.class);
                startActivityForResult(i, LIVE_CODE);
            }
        });

        fabOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasPermissions(MainActivity.this, PERMISSIONS)){
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_ALL);
                }else {
                    if (!isFABOpen) {
                        showFABMenu();
                    } else {
                        closeFABMenu();
                    }
                }
            }
        });

        imgNone=(ImageView)findViewById(R.id.imgNone);
    }

    private void showFABMenu(){
        isFABOpen=true;
        cameraButton.animate().translationX(-getResources().getDimension(R.dimen.standard_55));
        mediaButton.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        flowButton.animate().translationX(getResources().getDimension(R.dimen.standard_55));
        fabOpen.animate().rotation(315);
    }

    private void closeFABMenu(){
        isFABOpen=false;
        cameraButton.animate().translationX(0);
        mediaButton.animate().translationY(0);
        flowButton.animate().translationX(0);
        fabOpen.animate().rotation(0);
    }

    private class ScanButtonClickListener implements View.OnClickListener {

        private int preference;

        ScanButtonClickListener(int preference) {
            this.preference = preference;
        }

        ScanButtonClickListener() {
        }

        @Override
        public void onClick(View v) {
            closeFABMenu();
            startScan(preference);
        }
    }

    protected void startScan(int preference) {
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
        intent.putExtra("path",galleryPath);
        startActivityForResult(intent, REQUEST_CODE);
    }

    void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                child.delete();

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<Uri> uri = data.getExtras().getParcelableArrayList(ScanConstants.SCANNED_RESULT);
            Bitmap bitmap;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            try {
                for (int i=0;i<uri.size();i++) {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri.get(i));
                    if (uri.size()>1) saveImage(bitmap, timeStamp + " ("+String.valueOf(i+1) + ")");
                    else saveImage(bitmap, timeStamp);
                }
                File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                        + "/Android/data/"
                        + getApplicationContext().getPackageName()
                        + "/Files");
                deleteRecursive(mediaStorageDir);
                new listTask().execute();
            } catch (IOException e) {
                MessageBox.send("Error", e.getMessage(),MainActivity.this);
                e.printStackTrace();
            }
        }else if (requestCode==LIVE_CODE && resultCode==Activity.RESULT_OK){
            new listTask().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    new listTask().execute();

                } else {

                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            }

        }
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
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
            out.flush();
            out.close();

            addPicToGallery(MainActivity.this, root + "/" + fname);
        } catch (Exception e) {
            MessageBox.send("Error", e.getMessage(),MainActivity.this);
            e.printStackTrace();
        }
    }

    public static void addPicToGallery(Context context, String photoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    private class listTask extends AsyncTask<Void, Void, Void> {

        AVLoadingIndicatorView avi=(AVLoadingIndicatorView) findViewById(R.id.avi);
        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            imageGallery.setAdapter(null);
            avi.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            initList();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            setListAdapter();
            avi.setVisibility(View.GONE);
        }
    }

    private void initList() {
        titles=new ArrayList<>();

        pdfImages=new ArrayList<>();

        String root = Environment.getExternalStorageDirectory().toString() + "/MultiScanner";
        final File path = new File(root);
        path.mkdirs();

        if(path.exists()) {
            File[] list = path.listFiles();
            Arrays.sort(list, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                }
            });

            fileNames=new String[list.length];
            filePaths=new String[list.length];
            for (int i=0;i<list.length;i++){
                fileNames[i]=list[i].getName();
                filePaths[i]=list[i].getPath();
            }

            for (String fileName:filePaths) {
                try {
                    if (fileName.endsWith("pdf")) {
                        pdfImages.add(null);
                    }
                    else {
                        Image image = Image.getInstance(fileName);
                        pdfImages.add(image);
                    }
                } catch (IOException e) {
                    MessageBox.send("Error", e.getMessage(),MainActivity.this);
                    e.printStackTrace();
                } catch (BadElementException e) {
                    MessageBox.send("Error", e.getMessage(),MainActivity.this);
                    e.printStackTrace();
                }
            }

            final ListView imageGallery = (ListView) findViewById(R.id.imagegallery);

            //content: instead of file: cuz of api>=24
            if(Build.VERSION.SDK_INT>=24){
                try{
                    Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                    m.invoke(null);
                }catch(Exception e){
                    MessageBox.send("Error", e.getMessage(),MainActivity.this);
                    e.printStackTrace();
                }
            }

            imageGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i=new Intent(Intent.ACTION_VIEW);
                    if (dirAdapter.getDataList().get(position).endsWith("pdf")) {
                        i.setDataAndType(Uri.fromFile(new File(filePaths[position])), "application/pdf");
                    }else {
                        i.setDataAndType(Uri.fromFile(new File(filePaths[position])), "image/*");
                    }
                    startActivity(i);
                }
            });

            dirAdapter = new DirAdapter(this, R.layout.photo_item, fileNames, filePaths, new DirAdapter.CropClickListener() { //loadedBitmaps causes error in high counts
                @Override
                public void onCropClick(int position) {
                    String filePath = dirAdapter.getDataList().get(position);
                    galleryPath = Environment.getExternalStorageDirectory().toString() + "/MultiScanner/" + filePath;
                    startScan(ScanConstants.GET_MEDIA);
                }
            }, new DirAdapter.LiveClickListener() {
                @Override
                public void onLiveClick(int position) {
                    Intent i = new Intent(getApplicationContext(), LiveActivity.class);
                    String filePath = dirAdapter.getDataList().get(position);
                    galleryPath = Environment.getExternalStorageDirectory().toString() + "/MultiScanner/" + filePath;
                    i.putExtra("bmpPath",galleryPath);
                    startActivityForResult(i,LIVE_CODE);
                }
            });
        }
    }

    private void setListAdapter(){
        if (dirAdapter!=null) {
            if (dirAdapter.getCount() == 0) imgNone.setVisibility(View.VISIBLE);
            else imgNone.setVisibility(View.GONE);
        }else imgNone.setVisibility(View.GONE);

        imageGallery.setAdapter(dirAdapter);
        imageGallery.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        imageGallery.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                final int checkedCount  = imageGallery.getCheckedItemCount();
                // Set the  CAB title according to total checked items
                mode.setTitle(checkedCount  + "  Selected");
                // Calls  toggleSelection method from ListViewAdapter Class
                dirAdapter.toggleSelection(position);

            }
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_options, menu);
                //mTopToolbar.inflateMenu(R.menu.menu_options);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                // TODO  Auto-generated method stub
                switch  (item.getItemId()) {
                    case R.id.selectAll:
                        final int checkedCount  = fileNames.length;
                        // If item  is already selected or checked then remove or
                        // unchecked  and again select all
                        dirAdapter.removeSelection();
                        for (int i = 0; i <  checkedCount; i++) {
                            imageGallery.setItemChecked(i,   true);
                        }

                        // Set the  CAB title according to total checked items
                        // Calls  toggleSelection method from ListViewAdapter Class
                        // Count no.  of selected item and print it

                        mode.setTitle(checkedCount  + "  Selected");
                        return true;

                    case R.id.delete:
                        // Add  dialog for confirmation to delete selected item
                        // record.
                        AlertDialog.Builder  builder = new AlertDialog.Builder(
                                MainActivity.this);
                        builder.setMessage("Do you want to delete selected record(s)?");

                        builder.setNegativeButton("No", new  DialogInterface.OnClickListener() {
                            @Override
                            public void  onClick(DialogInterface dialog, int which) {

                            }
                        });
                        builder.setPositiveButton("Yes", new  DialogInterface.OnClickListener() {
                            @Override
                            public void  onClick(DialogInterface dialog, int which) {
                                SparseBooleanArray selected = dirAdapter
                                        .getSelectedIds();
                                for (int i =  (selected.size() - 1); i >= 0; i--) {
                                    if  (selected.valueAt(i)) {
                                        File file = new File(filePaths[selected.keyAt(i)]);

                                        boolean deleted = file.delete();

                                        String  selecteditem = dirAdapter
                                                .getItem(selected.keyAt(i));

                                        // Remove  selected items following the ids
                                        dirAdapter.remove(selecteditem,selected.keyAt(i));

                                        if (dirAdapter.getCount()==0) imgNone.setVisibility(View.VISIBLE);
                                        else imgNone.setVisibility(View.GONE);
                                    }
                                }
                                // Close CAB
                                mode.finish();
                                selected.clear();
                                new listTask().execute();
                            }
                        });

                        AlertDialog alert =  builder.create();
                        alert.setIcon(R.drawable.camera);// dialog  Icon
                        alert.setTitle("Confirmation"); // dialog  Title
                        alert.show();
                        return true;
                    case R.id.pdf:
                        // Add  dialog for confirmation to delete selected item
                        // record.
                        AlertDialog.Builder  builderp = new AlertDialog.Builder(
                                MainActivity.this);
                        builderp.setMessage("Do you want to create PDF?");

                        builderp.setNegativeButton("No", new  DialogInterface.OnClickListener() {
                            @Override
                            public void  onClick(DialogInterface dialog, int which) {
                                // TODO  Auto-generated method stub

                            }
                        });
                        builderp.setPositiveButton("Yes", new  DialogInterface.OnClickListener() {
                            @Override
                            public void  onClick(DialogInterface dialog, int which) {
                                // TODO  Auto-generated method stub
                                SparseBooleanArray selected = dirAdapter
                                        .getSelectedIds();

                                String root = Environment.getExternalStorageDirectory().toString() + "/MultiScanner";
                                File myDir = new File(root);
                                myDir.mkdirs();
                                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                                String fname = "MultiScanner-" + timeStamp+ ".pdf";
                                File file = new File(myDir, fname);
                                try {
                                    Document document = new Document();

                                    PdfWriter.getInstance(document, new FileOutputStream(file));
                                    document.open();

                                    document.setMargins(5f,5f,5f,5f);
                                    addImage(document,selected);
                                    document.close();
                                }catch (Exception e){
                                    MessageBox.send("Error", e.getMessage(),MainActivity.this);
                                }

                                new listTask().execute();

                                // Close CAB
                                mode.finish();
                                selected.clear();
                            }
                        });

                        AlertDialog alertp =  builderp.create();
                        alertp.setIcon(R.drawable.camera);// dialog  Icon
                        alertp.setTitle("Confirmation"); // dialog  Title
                        alertp.show();
                        return true;
                    case R.id.share:
                        SparseBooleanArray selected = dirAdapter
                                .getSelectedIds();
                        ArrayList<Uri> uris=new ArrayList<>();
                        for (int i =  (selected.size() - 1); i >= 0; i--) {
                            if  (selected.valueAt(i)) {
                                uris.add(Uri.fromFile(new File(filePaths[selected.keyAt(i)])));
                            }
                        }

                        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);
                        shareIntent.setType("*/*");
                        startActivity(Intent.createChooser(shareIntent , "Share"));

                        mode.finish();
                        selected.clear();
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                dirAdapter.removeSelection();
            }
        });
    }

    private void addImage(Document document,SparseBooleanArray sparseBooleanArray){

        try {
            for (int i = (sparseBooleanArray.size() - 1); i >= 0; i--) {
                if (sparseBooleanArray.valueAt(i)) {
                    Image image=pdfImages.get(sparseBooleanArray.keyAt(i));

                    if (image==null) document=mergePdf(document,sparseBooleanArray.keyAt(i));
                    else {
                        image.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());

                        document.newPage();
                        document.add(image);
                    }
                }
            }
        }catch (Exception e){
            MessageBox.send("Error", e.getMessage(),MainActivity.this);
            e.printStackTrace();
        }
    }

    private Document mergePdf(Document document,int index) throws Exception {

        PdfReader reader = new PdfReader(filePaths[index]);
        int n = reader.getNumberOfPages();
        reader.close();
        for (int i = 1; i <= n; i++) {
            reader = new PdfReader(filePaths[index]);
            reader.selectPages(String.valueOf(i));

            Image image=null;
            for (int j=0;j<reader.getXrefSize();j++) {
                try{
                    PRStream stream = (PRStream) reader.getPdfObject(j);
                    byte[] bytes = PdfReader.getStreamBytesRaw(stream);
                    image = Image.getInstance(bytes);
                    break;
                }catch (Exception e) {
                    continue;
                }
            }

            image.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());

            document.newPage();
            document.add(image);
            reader.close();
        }

        return document;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent i=null;
        if (id == R.id.about) {
            i = new Intent(getApplicationContext(), Danial.class);
            startActivity(i);
        }
        else if (id==R.id.help){
            new OpenLocalPDF(MainActivity.this,"apppresentation.pdf").execute();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(!isFABOpen){
            super.onBackPressed();
        }else{
            closeFABMenu();
        }
    }
}

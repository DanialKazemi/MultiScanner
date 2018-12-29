package ir.danial.multiscanner;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class Danial extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danial);

        android.support.v7.widget.Toolbar toolBar=(android.support.v7.widget.Toolbar)findViewById(R.id.toolBar1);
        setSupportActionBar(toolBar);
        getSupportActionBar().setTitle("About");
        toolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        toolBar.setTitleTextColor(Color.WHITE);

        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView txtName = (TextView) findViewById(R.id.txtDanial);

        Typeface face = Typeface.createFromAsset(getAssets(),
                "fonts/stingray.otf");
        txtName.setTypeface(face);

        final String[] addresses=new String[1];
        addresses[0]="mastermind_907@yahoo.com";
        ImageView imgTelegram=(ImageView)findViewById(R.id.imgEmail);
        imgTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","mastermind_907@yahoo.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MultiScanner Suggestion");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi Danial,");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, addresses);
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            }
        });
    }
}

package com.example.tli3scanner;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;

public class InformationActivity extends AppCompatActivity {

    private HashMap<String, String> mUrls = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.information);
        }

        mUrls.put("www.tli3.ru", "https://www.tli3.ru/");
        mUrls.put("termologika.ru", "https://termologika.ru/");
        mUrls.put("install.appcenter.ms", "https://install.appcenter.ms/users/thermology/apps/tli3scanner-1/distribution_groups/usertesting");

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = (TextView)v;
                openUrl(mUrls.get(textView.getText().toString()));
            }
        };

        findViewById(R.id.urlMaker).setOnClickListener(listener);
        findViewById(R.id.urlUpdate).setOnClickListener(listener);
        findViewById(R.id.urlWebapp).setOnClickListener(listener);

    }

    private void openUrl(String url){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
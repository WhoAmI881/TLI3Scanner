package com.example.tli3scanner;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class PdfActivity extends AppCompatActivity {

    private File mReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        String report = getIntent().getExtras().getString("pdf_name");
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(report);
        }

        PhotoView photoView = findViewById(R.id.pdfview);
        ImageView mShareImage = findViewById(R.id.share);
        ImageView mOpenWithImage = findViewById(R.id.open_with);
        ImageView mPrintImage = findViewById(R.id.print);
        mShareImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePdfFile();
            }
        });
        mOpenWithImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWithPdfFile();
            }
        });
        mPrintImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printPdfFile();
            }
        });

        mReport = new File(getFilesDir(), report);
        try {
            ParcelFileDescriptor pdf = ParcelFileDescriptor.open(mReport, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pdf);
            PdfRenderer.Page page = renderer.openPage(0);
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth()*2, page.getHeight()*2, Bitmap.Config.ARGB_4444);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            photoView.setImageBitmap(bitmap);
            page.close();
            renderer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void sharePdfFile(){
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".fileprovider", mReport);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("application/pdf");
        startActivity(Intent.createChooser(shareIntent, null));
    }

    private void openWithPdfFile(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri uri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".fileprovider", mReport);

        intent.setDataAndType(uri, "application/pdf");
        startActivity(intent);
    }

    private void printPdfFile(){
        PrintManager printManager = (PrintManager)getSystemService(getBaseContext().PRINT_SERVICE);
        try {
            PrintDocumentAdapter printDocumentAdapter = new PdfDocumentAdapter(this, mReport.getPath());
            printManager.print("Document",printDocumentAdapter,new PrintAttributes.Builder().build());
        }catch (Exception ex){

        }
    }

}
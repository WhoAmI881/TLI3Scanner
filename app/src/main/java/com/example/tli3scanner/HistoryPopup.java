package com.example.tli3scanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.tli3scanner.history.HistoryCreator;
import com.example.tli3scanner.history.HistoryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HistoryPopup {

    private ArrayList<HistoryItem> mHistory;

    private Context mContext;
    private PopupWindow mPopupWindow;
    private View mPopupView;

    private Button mCloseButton;
    private TextView mTitleView;
    private ListView mListHistory;

    public HistoryPopup(Context context){
        mContext = context;
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_history_layout, null);

        mCloseButton = mPopupView.findViewById(R.id.messageButton);
        mTitleView = mPopupView.findViewById(R.id.title);
        mListHistory = mPopupView.findViewById(R.id.history);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        mPopupWindow = new PopupWindow(mPopupView, width, height, true);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
            }
        });
    }

    public void showHistoryWindow(ArrayList<HistoryItem> history) {

        mPopupWindow.showAtLocation(mPopupView, Gravity.CENTER, 0, 0);
        mHistory = history;

        ArrayList<String> value = new ArrayList<>(mHistory.size());
        for (HistoryItem item : mHistory) {
            value.add(item.getSerial10() + "          (" + item.getDateTime() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_list_item_1, value);

        mListHistory.setAdapter(adapter);

        mListHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPopupWindow.dismiss();
                openPdfIntent(mHistory.get(position).getSerial36());
            }
        });
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener){
        mPopupWindow.setOnDismissListener(listener);
    }

    private void openPdfIntent(String serial){
        Intent intent = new Intent(mContext, PdfActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("pdf_name", serial + ".pdf");
        intent.putExtras(bundle);
        mContext.startActivity(intent);

        /*
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File report = new File(mContext.getFilesDir(), serial + ".pdf");
        Uri uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileprovider", report);

        intent.setDataAndType(uri, "application/pdf");
        mContext.startActivity(intent);

         */

    }

}

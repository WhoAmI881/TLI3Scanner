package com.example.tli3scanner;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;

import java.util.Locale;

public class LanguagePopup {

    private final String RU = "ru";
    private final String EN = "en";

    private Context mContext;
    private PopupWindow mPopupWindow;
    private View mPopupView;
    private RadioButton mRadioRus;
    private RadioButton mRadioEng;
    private String mSelect;

    public LanguagePopup(Context context){
        mContext = context;
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_language_layout, null);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;

        mPopupWindow = new PopupWindow(mPopupView, width, height, true);

        mPopupView.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
            }
        });
        mPopupView.findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Locale locale = new Locale(mSelect);
                Locale.setDefault(locale);
                Resources resources = mContext.getResources();
                Configuration config = resources.getConfiguration();
                config.setLocale(locale);
                resources.updateConfiguration(config, resources.getDisplayMetrics());

                Activity activity = ((Activity)mContext);
                activity.finish();
                activity.startActivity(activity.getIntent());
            }
        });

        mRadioRus = mPopupView.findViewById(R.id.ru);
        mRadioEng = mPopupView.findViewById(R.id.en);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.ru:
                        mSelect = RU;
                        break;
                    case R.id.en:
                        mSelect = EN;
                }
            }
        };

        mRadioRus.setOnClickListener(listener);
        mRadioEng.setOnClickListener(listener);

    }

    public void showLanguageWindow() {
        mPopupWindow.showAtLocation(mPopupView, Gravity.CENTER, 0, 0);

        Resources resources = mContext.getResources();
        String language = resources.getString(R.string.select_lang);

        if(language.equals(RU)){
            mRadioRus.setChecked(true);
            mSelect = RU;
        }else{
            mRadioEng.setChecked(true);
            mSelect = EN;
        }
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener){
        mPopupWindow.setOnDismissListener(listener);
    }

}

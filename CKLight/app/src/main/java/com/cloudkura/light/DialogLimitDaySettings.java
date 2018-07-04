package com.cloudkura.light;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.Map;

public class DialogLimitDaySettings extends DialogFragment {

    // 親Activity
    static ActivityMain mParentActivity;

    // 画面部品
    Dialog dialog;
    LinearLayout mLlLimitDaySpan;
    Fragment fragment;
    View mView;
    Switch mSwAlertOnOff;
    NumberPicker mNpAlertSpan;

    // 設定情報
    DASettings daSettings;
    boolean mIsAlertOn = false;

    public DialogLimitDaySettings() { }

    // ログ書き出し
    private void writeLog() {
        CKFB.writeLog(this.getClass().getSimpleName());
    }
    private void writeLog(String value) {
        CKFB.writeLog(this.getClass().getSimpleName(), value);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mParentActivity = (ActivityMain) context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mParentActivity = (ActivityMain) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.gc();
    }

    public static DialogLimitDaySettings newInstance() {
        return new DialogLimitDaySettings();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writeLog();

        // 自分を保存
        fragment = this;

        //XMLとの紐付け
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fragment_limit_day_settings, null, false);

        // ダイアログ情報を取得
        dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(mView);

        // 角丸を有効にするためデフォルトの描画設定をクリア
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);    // ダイアログ外タップで消させない。

        // 画面項目
        mLlLimitDaySpan = (LinearLayout) mView.findViewById(R.id.fragment_limit_day_settings_days);
        mSwAlertOnOff = (Switch) mView.findViewById(R.id.fragment_setting_alert_onoff);
        mSwAlertOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsAlertOn = isChecked;
                mLlLimitDaySpan.setVisibility(mIsAlertOn ? View.VISIBLE : View.GONE);
            }
        });
        mNpAlertSpan = (NumberPicker) mView.findViewById(R.id.fragment_setting_alert_span);
        //mNpAlertSpan = (MyNumberPicker) mView.findViewById(R.id.fragment_setting_alert_span);

        // 範囲指定
        mNpAlertSpan = CKUtil.setNumberPickerRangeTargetDays(mNpAlertSpan);
        // dividerの色変更
        CKUtil.setNumberPickerDividerNone(mNpAlertSpan);

        // 初期化
        daSettings = CKDBUtil.getDASettings();

        // 設定情報を読み込み、画面に設定
        ArrayList<String> whereList = new ArrayList<>();
        String whereSql = DASettings.col_Id + " = ? ";
        whereList.add(DASettings.key_LimitDayAlertSetting);
        ArrayList<Map<String, String>> listItems = daSettings.getItemListView(whereSql, whereList.toArray(new String[0]), "");
        if (listItems != null && listItems.size() > 0){
            Map<String, String> item = listItems.get(0);
            mSwAlertOnOff.setChecked(daSettings.getAlertOnOff());
            mNpAlertSpan.setValue(item.get(DASettings.col_AlertSpan) == null ? CKUtil.IN_1_MONTH : Integer.valueOf(item.get(DASettings.col_AlertSpan)));

            mIsAlertOn = mSwAlertOnOff.isChecked();
            mLlLimitDaySpan.setVisibility(mIsAlertOn ? View.VISIBLE : View.GONE);
        }

        // 閉じるボタン
        Button btClose = (Button) mView.findViewById(R.id.fragment_item_settings_button_cancel);
        btClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // 登録ボタン
        Button btCommit = (Button) mView.findViewById(R.id.fragment_item_settings_button_commit);
        btCommit.setOnTouchListener(CKUtil.doOnTouchAnimation());
        btCommit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // 基本情報の登録
                daSettings.setValue(DASettings.col_Id, DASettings.key_LimitDayAlertSetting);
                daSettings.setValue(DASettings.col_AlertOnOff, String.valueOf(mIsAlertOn ? "1" : "0"));
                daSettings.setValue(DASettings.col_AlertSpan, String.valueOf(mNpAlertSpan.getValue()));
                daSettings.upsertData();

                writeLog("alert-onoff: " + daSettings.getValue(DASettings.col_AlertOnOff));
                writeLog("alert-span: " + daSettings.getValue(DASettings.col_AlertSpan));

                // ナビゲーション表示を更新
                mParentActivity.setNavigationItemDisplay();
                // アラート発火
                CKStatus.setAlertNotification();

                // 画面を閉じる
                dismiss();
            }
        });

        // キーボード非表示
        CKUtil.hideKeyboad(mView);

        // NumberPickerのフォーカスを外すためにボタンへフォーカス
        btCommit.requestFocus();

        return dialog;
    }

    // ダイアログの横幅、高さ、表示位置を設定
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        lp.width = (int) (metrics.widthPixels * 0.9);//横幅
        dialog.getWindow().setAttributes(lp);
    }

}

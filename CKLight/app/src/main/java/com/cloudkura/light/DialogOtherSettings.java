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
import android.widget.Switch;
import android.widget.TextView;

public class DialogOtherSettings extends DialogFragment {

    // 親Activity
    ActivityMain mParentActivity;

    // 画面部品
    Dialog dialog;
    Fragment fragment;
    View mView;
    TextView mTvLastLaunchedDateTime;
    Switch mSwIsShowNoticePopup;

    // 設定情報
    DASettings daSettings;

    public DialogOtherSettings() { }

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

    public static DialogOtherSettings newInstance() {
        return new DialogOtherSettings();
    }

    // ログ書き出し
    private void writeLog(String value) {
        CKFB.writeLog(this.getClass().getSimpleName(), value);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writeLog("!!!!開発モードが開かれた!!!!");

        // 自分を保存
        fragment = this;

        //XMLとの紐付け
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fragment_other_settings, null, false);

        // ダイアログ情報を取得
        dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(mView);

        // 角丸を有効にするためデフォルトの描画設定をクリア
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);    // ダイアログ外タップで消させない。

        // 画面項目
        mTvLastLaunchedDateTime = (TextView) mView.findViewById(R.id.fragment_other_settings_last_launched_datetime);
        mSwIsShowNoticePopup = (Switch) mView.findViewById(R.id.fragment_setting_show_notice_popup);

        // 各種フラグの設定値
        mSwIsShowNoticePopup.setChecked(CKUtil.isShowNoticePopup());

        // お知らせ画面
        mSwIsShowNoticePopup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateSettingItemBoolean(DASettings.key_IsShowNoticeDialog, DASettings.col_IsHidden, isChecked);
            }
        });

        // 設定情報を読み込み、画面に設定
        daSettings = CKDBUtil.getDASettings();
        mTvLastLaunchedDateTime.setText(CKUtil.getLastLaunchedDateTime());

        // 閉じるボタン
        Button btClose = (Button) mView.findViewById(R.id.fragment_item_other_settings_button_cancel);
        btClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mParentActivity.refreshRelationInfo();
                dismiss();
            }
        });

        // キーボード非表示
        CKUtil.hideKeyboad(mView);

        return dialog;
    }

    // 設定情報の更新
    private void updateSettingItemBoolean(String key, String col, boolean flg) {
        daSettings.setValue(DASettings.col_Id, key);
        daSettings.setValue(col, flg ? "0" : "1");
        daSettings.upsertData();
    }
    private void updateSettingItemString(String key, String col, String val) {
        daSettings.setValue(DASettings.col_Id, key);
        daSettings.setValue(col, val);
        daSettings.upsertData();
    }

    // ダイアログの横幅、高さ、表示位置を設定
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        lp.width = (int) (metrics.widthPixels * 0.95);//横幅
        lp.height = (int) (metrics.heightPixels * 0.95);//高さ 1000
        dialog.getWindow().setAttributes(lp);
    }

}

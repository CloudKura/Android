package com.cloudkura.light;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.google.android.gms.ads.NativeExpressAdView;

public class DialogNotice extends DialogFragment
{

    // AdMod用
    NativeExpressAdView mAdView;

    // 画面部品
    Dialog dialog;
    View mView;
    Button mBtClose;

    public static DialogNotice newInstance() {
        return new DialogNotice();
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //XMLとの紐付け
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.popup_notice, null, false);

        //ダイアログの作成
        dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(mView);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // オブジェクト設定
        mBtClose = (Button) mView.findViewById(R.id.popup_ads_close_button);

        //閉じるボタン押下時はダイアログを消す
        mBtClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // キーボード非表示
        CKUtil.hideKeyboad(mView);

        return dialog;
    }

    @Override
    public void onResume() {
        if (mAdView != null) {
            mAdView.resume();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        System.gc();
        super.onDestroy();
    }
}
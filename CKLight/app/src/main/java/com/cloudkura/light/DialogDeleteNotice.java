package com.cloudkura.light;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;
import java.util.Map;

public class DialogDeleteNotice extends android.support.v4.app.DialogFragment {

    enum enmDeleteType {
        deleteAllItem,          // 商品を削除(一覧画面での削除)
        deleteEachLimitDate,    // 消費期限別に削除(詳細画面での削除)
        deleteEachItem          // 一品ずつの削除(個別に分解表示した画面での削除)
    }

    // AdMod用
    AdView mAdView;
    //InterstitialAd mInterstitialAd;

    // 親Activity
    Activity mParentActivity;
    String mBarcode;
    ArrayList<String> mDeleteItemsIId;
    DialogDeleteAffiliate mDialogDeleteAffiliate;
    // AdMod用
    AdRequest mAdRequest;

    // 画面部品
    Dialog dialog;
    View mView;

    // アフィリエイトリンク
    //String mAffiUrlRakuten = "";

    public DialogDeleteNotice() { }

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
        mParentActivity = (Activity) context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mParentActivity = activity;
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

    public static DialogDeleteNotice newInstance() {
        return new DialogDeleteNotice();
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writeLog();

        //XMLとの紐付け
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.popup_delete_notice, null, false);

        // 事前ロード
        CKUtil.loadAdRequest();

        // Google AdMod用
        mAdView = (AdView) mView.findViewById(R.id.adView);
        mAdView.loadAd(CKUtil.getmAdRequest());
        /*
        // 全画面用
        mInterstitialAd = new InterstitialAd(CKUtil.getMyContext());
        mInterstitialAd.setAdUnitId(CKUtil.getMyString(R.string.interstitial_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            // 取得した広告を表示
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // 閉じるボタンで実装
            }
        });
        */

        // ダイアログ情報を取得
        dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(mView);

        // 角丸を有効にするためデフォルトの描画設定をクリア
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);    // ダイアログ外タップで消させない。

        String itemIId = getArguments().getString(DialogDeleteAffiliate.ARG_ITEM_ID);
        mBarcode = itemIId;
        mDeleteItemsIId = getArguments().getStringArrayList(DialogDeleteAffiliate.ARG_DELETE_ITEMS_IID);

        LinearLayout lnDeleteInfo = (LinearLayout)mView.findViewById(R.id.popup_notice_delete);
        TextView tvItemName = (TextView)mView.findViewById(R.id.popup_delete_notice_item_name);
        TextView tvRemainCount = (TextView)mView.findViewById(R.id.popup_delete_notice_remain_count);
        TextView tvNoticeAffiriate = (TextView)mView.findViewById(R.id.popup_notice_delete_affiriate);
        if (itemIId != null && itemIId.equals("")) {
            lnDeleteInfo.setVisibility(View.GONE);
            tvNoticeAffiriate.setVisibility(View.GONE);
            //mAffiUrlRakuten = CKUtil.getMyString(R.string.weburl1);
        } else {
            lnDeleteInfo.setVisibility(View.VISIBLE);
            // 商品名表示
            Map<String, String> itemInfo = CKDBUtil.getDAItem().getUpsertByIId(itemIId);
            if (itemInfo == null || itemInfo.get(DAItem.col_Explain).equals("")) {
                tvItemName.setText(CKUtil.getMyString(R.string.item_no_name));
            } else {
                tvItemName.setText(itemInfo.get(DAItem.col_Explain));
            }

            // 残個数表示
            DAItemStock daItemStock = CKDBUtil.getDAItemStock();
            int itemRemainCount = daItemStock.getItemCount(itemIId, "", "");
            tvRemainCount.setText(CKUtil.numFormat(itemRemainCount));

            /*
            // アフィリエイト取得
            mAffiUrlRakuten = CKUtil.getMyString(R.string.weburl1);
            tvNoticeAffiriate.setVisibility(View.GONE);
            */
        }

        /*
        // 楽天ボタン
        Button btRakuten = (Button) mView.findViewById(R.id.popup_notice_deleted_button_rakuten);
        btRakuten.setOnTouchListener(CKUtil.doOnTouchAnimation());
        btRakuten.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                writeLog("del-buy_rakuten: " + mBarcode);
                CKUtil.openURL(mAffiUrlRakuten + mBarcode + "/");
            }
        });
        // Yahooボタン
        Button btYahoo = (Button) mView.findViewById(R.id.popup_notice_deleted_button_yahoo);
        btYahoo.setOnTouchListener(CKUtil.doOnTouchAnimation());
        btYahoo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                writeLog("del-buy_yahoo: " + mBarcode);
                CKUtil.openURL(CKUtil.getMyString(R.string.weburl2) + mBarcode);
            }
        });
        */

        // 次画面の事前ロード
        if (mDialogDeleteAffiliate == null) {
            Bundle arg = new Bundle();
            arg.putString(DialogDeleteAffiliate.ARG_ITEM_ID, mBarcode);
            arg.putSerializable(DialogDeleteAffiliate.ARG_DELETE_TYPE, DialogDeleteNotice.enmDeleteType.deleteEachItem);
            arg.putStringArrayList(DialogDeleteAffiliate.ARG_DELETE_ITEMS_IID, mDeleteItemsIId);
            arg.putBoolean(DialogDeleteAffiliate.ARG_FROM_NO_STOCK_LIST, false);

            mDialogDeleteAffiliate = new DialogDeleteAffiliate();
            mDialogDeleteAffiliate.setCancelable(false);
            mDialogDeleteAffiliate.setArguments(arg);
        }

        // 閉じるラベル
        TextView tvClose = (TextView) mView.findViewById(R.id.popup_delete_notice_button_close);
        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 補充おすすめ通知画面を表示
                String dialogTag = "notice_affiliate";
                if (getFragmentManager().findFragmentByTag(dialogTag) == null) {
                    mDialogDeleteAffiliate.show(getFragmentManager(), dialogTag);
                }

                dismiss();
                /*
                if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
                */
            }
        });

        // キーボード非表示
        CKUtil.hideKeyboad(mView);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
    }
}

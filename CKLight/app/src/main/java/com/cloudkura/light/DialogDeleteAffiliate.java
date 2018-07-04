package com.cloudkura.light;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Map;

public class DialogDeleteAffiliate extends android.support.v4.app.DialogFragment {

    // 引数
    public final static String ARG_ITEM_ID = "item_id";
    public final static String ARG_DELETE_ITEMS_IID = "delete_items";
    public final static String ARG_DELETE_TYPE = "delete_type";
    public final static String ARG_FROM_NO_STOCK_LIST = "no_stock_list";

    // AdMod用
    AdView mAdView;
    //InterstitialAd mInterstitialAd;

    // 親Activity
    Activity mParentActivity;
    String mBarcode;
    ArrayList<String> mArgItemIIds;

    // 画面部品
    Dialog dialog;
    View mView;

    public DialogDeleteAffiliate() { }

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

    public static DialogDeleteAffiliate newInstance() {
        return new DialogDeleteAffiliate();
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writeLog();

        //XMLとの紐付け
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.popup_delete_affiliate, null, false);

        // Google AdMod用
        mAdView = (AdView) mView.findViewById(R.id.adView);
        mAdView.loadAd(CKUtil.getmAdRequest());
        // 全画面用
        /*
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

        String itemIId = getArguments().getString(ARG_ITEM_ID);
        mBarcode = itemIId;
        mArgItemIIds = getArguments().getStringArrayList(ARG_DELETE_ITEMS_IID);

        // 楽天ボタン
        Button btRakuten = (Button) mView.findViewById(R.id.popup_delete_affiliate_button_rakuten);
        btRakuten.setOnTouchListener(CKUtil.doOnTouchAnimation());
        btRakuten.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                writeLog("del-buy_rakuten: " + mBarcode);
                CKUtil.openURL(CKUtil.getMyString(R.string.weburl1) + mBarcode + "/");
            }
        });
        // Yahooボタン
        Button btYahoo = (Button) mView.findViewById(R.id.popup_delete_affiliate_button_yahoo);
        btYahoo.setOnTouchListener(CKUtil.doOnTouchAnimation());
        btYahoo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                writeLog("del-buy_yahoo: " + mBarcode);
                CKUtil.openURL(CKUtil.getMyString(R.string.weburl2) + mBarcode);
            }
        });

        // 補充リストへ追加ボタン
        Button btShoppingList = (Button) mView.findViewById(R.id.popup_delete_affiliate_button_add_shopping_list);
        if (getArguments().getBoolean(ARG_FROM_NO_STOCK_LIST)) {
            // 補充ボタンは補充リスト以外の削除画面からの場合に表示
            btShoppingList.setVisibility(View.GONE);
        } else {
            btShoppingList.setVisibility(View.VISIBLE);

            btShoppingList.setOnTouchListener(CKUtil.doOnTouchAnimation());
            btShoppingList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    writeLog("add-to-shopping-list: " + mBarcode);

                    for (int i = 0; i < mArgItemIIds.size(); i++) {
                        String barcode = mArgItemIIds.get(i);
                        // 補充対象商品としてマーク
                        Map<String, String> daShoppingListItem = CKDBUtil.getDAItem().getUpsertByIId(barcode);
                        daShoppingListItem.put(DAItem.col_ItemType, DAItem.val_ItemType_Fill);
                        DAItem daItem = CKDBUtil.getDAItem();
                        daItem.setAllValue(daShoppingListItem);
                        daItem.upsertData();

                        // 補充リスト用のストック情報を追加(非表示解除)
                        DAItemStock daShoppingListStock = CKDBUtil.getDAItemStock();
                        Map<String, String> daSetValue = daShoppingListStock.getItemByKey(barcode, DAItemStock.val_ShoppingList);
                        if (daSetValue == null) {
                            daShoppingListStock.setValue(DAItemStock.col_IId, barcode);
                            daShoppingListStock.setValue(DAItemStock.col_LimitDt, DAItemStock.val_ShoppingList);
                        } else {
                            daShoppingListStock.setAllValue(daSetValue);
                        }
                        daShoppingListStock.setValue(DAItemStock.col_IsHidden, DABase.val_HiddenOff);
                        daShoppingListStock.upsertData();
                    }

                    // 補充リストタブを表示
                    if (mParentActivity.getClass() == ActivityMain.class) {
                        ActivityMain activityMain = (ActivityMain) mParentActivity;
                        activityMain.replaceToTabFragment(CKUtil.TabPage.TAB_NOSTOCK);
                    }

                    CKUtil.showLongToast(CKUtil.getMyString(R.string.message_add_stock_list));

                    // 画面を閉じる
                    closeDialog();
                }
            });
        }

        // 閉じるラベル
        TextView tvClose = (TextView) mView.findViewById(R.id.popup_delete_affiliate_button_close);
        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 画面を閉じる
                closeDialog();
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

    // 画面を閉じる共通処理
    private void closeDialog() {

        if (mParentActivity.getClass().equals(ActivityNotificationResult.class)) {
            ((ActivityNotificationResult) mParentActivity).onDissMissReturn();
        } else {
            try {
                for (Fragment fragment : getFragmentManager().getFragments()) {
                    if (fragment != null && fragment.getClass() != null) {
                        if (fragment.getClass().equals(DialogUseList.class)) {
                            ((DialogUseList) fragment).closeDialogUseList();
                            break;
                        }
                    }
                }

            } catch (Exception ex) {
            }
        }

        dismiss();
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

package com.cloudkura.light;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.NumberPicker;

import java.util.Calendar;
import java.util.Map;

public class DialogAddItemStock extends DialogFragment {

    static DAItemStock mItemStock;
    static String mIId;
    static String mLimitDate;

    // 画面部品
    Dialog dialog;
    MyDatePicker mDpLimitDt;
    NumberPicker mNpCount10;
    NumberPicker mNpCount1;
    Button mBtRegist;
    Button mBtClose;

    // 引数
    public final static String ARG_ITEM_ID = "item_id";
    public final static String ARG_LIMIT_DATE = "limit_date";

    // Interface
    public interface OnDialogEditStockListener {
        void onDialogEditStockButtonRegistClick(String limitDate);
        void onDialogEditStockButtonCancelClick();
    }

    private DialogAddItemStock.OnDialogEditStockListener mlistener;

    // ログ書き出し
    private void writeLog() {
        CKFB.writeLog(this.getClass().getSimpleName());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment fragment = this.getTargetFragment();
        try {
            // Fragment/Activity どちらから呼びだされたか。
            mlistener = (fragment != null) ? (DialogAddItemStock.OnDialogEditStockListener) fragment : (DialogAddItemStock.OnDialogEditStockListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("implement error OnDialogEditStockListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Fragment fragment = this.getTargetFragment();
        try {
            // Fragment/Activity どちらから呼びだされたか。
            mlistener = (fragment != null) ? (DialogAddItemStock.OnDialogEditStockListener) fragment : (DialogAddItemStock.OnDialogEditStockListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("implement error OnDialogEditStockListener");
        }
    }

    public static DialogAddItemStock newInstance() {
        DialogAddItemStock fragment = new DialogAddItemStock();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        writeLog();

        //XMLとの紐付け
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.popup_edit_item_stock, null, false);

        //ダイアログの作成
        dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        //dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 引数取得
        mIId = getArguments().getString(ARG_ITEM_ID);
        mIId = (mIId == null ? "" : mIId);
        mLimitDate = getArguments().getString(ARG_LIMIT_DATE);
        mItemStock = CKDBUtil.getDAItemStock();
        mItemStock.setValue(DAItemStock.col_IId, mIId);

        // オブジェクト設定
        mDpLimitDt = (MyDatePicker) view.findViewById(R.id.popup_edit_item_stock_limit_date);
        mNpCount10 = (NumberPicker) view.findViewById(R.id.popup_edit_item_stock_count_10);
        mNpCount1 = (NumberPicker) view.findViewById(R.id.popup_edit_item_stock_count_1);
        mBtRegist = (Button) view.findViewById(R.id.popup_edit_item_stock_regist);
        mBtRegist.setOnTouchListener(CKUtil.doOnTouchAnimation());
        mBtClose = (Button) view.findViewById(R.id.popup_edit_item_stock_close_button);

        // 入力制御
        mNpCount10.setMinValue(0);
        mNpCount10.setMaxValue(9);
        mNpCount1.setMinValue(0);
        mNpCount1.setMaxValue(9);
        // 初期値
        mNpCount10.setValue(0);
        mNpCount1.setValue(1);

        // dividerの色変更
        CKUtil.setNumberPickerDividerNone(mNpCount10);
        CKUtil.setNumberPickerDividerNone(mNpCount1);

        // 初期値設定
        if (mLimitDate != null && ! mLimitDate.equals("")) {

            // 期限
            Calendar calendarToday = Calendar.getInstance();
            calendarToday.setTime(CKUtil.isDate(mLimitDate));
            int year = calendarToday.get(Calendar.YEAR);
            int month = calendarToday.get(Calendar.MONTH);
            int day = calendarToday.get(Calendar.DAY_OF_MONTH);
            mDpLimitDt.init(year, month, day, new MyDatePicker.OnDateChangedListener(){
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    return;
                }
            });

            // 初期化
            mBtRegist.setText("反　映");
            // 数量
            Map<String, String> data = mItemStock.getItemByKey(mIId, mLimitDate);
            if (data != null) {
                // ボタン文言を切り替え
                mBtRegist.setText("ストックを更新");

                mItemStock.setAllValue(data);
                String itemCount = CKUtil.nullTo(mItemStock.getValue(DAItemStock.col_ItemCount), "0").toString();
                if (! itemCount.equals("")) {
                    if (itemCount.length() > 1) {
                        mNpCount10.setValue(Integer.valueOf(itemCount.substring(0, 1)));
                        mNpCount1.setValue(Integer.valueOf(itemCount.substring(1, 2)));
                    } else {
                        mNpCount10.setValue(0);
                        mNpCount1.setValue(Integer.valueOf(itemCount));
                    }
                }
            }
        }

        // 保存ボタン押下時の処理
        mBtRegist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeLog();

                // 編集後の消費期限
                String limitDate = CKUtil.getFormatDate(mDpLimitDt.getYear(), mDpLimitDt.getMonth() + 1, mDpLimitDt.getDayOfMonth());

                // 数量
                int inputCount = mNpCount10.getValue() * 10 + mNpCount1.getValue();
                if (inputCount == 0) {
                    // 数量0チェック
                    // 追加不可を通知する。
                    new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle("数量")
                            .setMessage("数量を入力してください。")
                            .setPositiveButton(
                                    R.string.alert_dialog_button_yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            // 何もしない
                                        }
                                    })
                            .show();
                } else {

                    // 既存データを削除
                    if (mLimitDate != null && ! mLimitDate.equals("")) {
                        // キー設定
                        mItemStock.setValue(DAItemStock.col_IId, mIId);
                        mItemStock.setLimitDate(mLimitDate);
                        mItemStock.deleteData();
                    }

                    // 画面で入力された消費期限で同一キーの判定
                    DAItemStock checkStock = CKDBUtil.getDAItemStock();
                    Map<String, String> existData = checkStock.getItemByKey(mIId, limitDate);
                    if (existData == null) {
                        // 新規登録
                        // IID
                        mItemStock.setValue(DAItemStock.col_IId, mIId);
                        // 消費期限
                        mItemStock.setLimitDate(limitDate);
                        // 数量
                        mItemStock.setValue(DAItemStock.col_ItemCount, String.valueOf(inputCount) );

                        // 更新
                        dismissDialogAfterUpdateData();
                    } else {

                        mItemStock.setAllValue(existData);
                        String preCount = mItemStock.getValue(DAItemStock.col_ItemCount);
                        String registCount = String.valueOf(Integer.valueOf(preCount)  + inputCount);

                        if (Integer.valueOf(registCount) > CKDBUtil.MAX_ITEM_COUNT) {
                            // 追加不可を通知する。
                            new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                                    .setCancelable(false)
                                    .setTitle(CKUtil.getMyString(R.string.message_add_stock_limit_day) + limitDate)
                                    .setMessage(CKUtil.getMyString(R.string.message_not_add_by_limit_over) + String.valueOf(CKDBUtil.MAX_ITEM_COUNT))
                                    .setPositiveButton(
                                            R.string.alert_dialog_button_yes,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int whitch) {
                                                    // 何もしない
                                                }
                                            })
                                    .show();

                        } else {
                            mItemStock.setValue(DAItemStock.col_ItemCount, registCount);

                            if (preCount.equals("0")) {
                                // 更新
                                dismissDialogAfterUpdateData();
                            } else {
                                String msg = CKUtil.getMyString(R.string.message_add_stock_limit_day) + CKUtil.getFormatDate(CKUtil.isDate(limitDate)) + "\n" +
                                        CKUtil.getMyString(R.string.message_add_stock_count) + preCount +
                                        CKUtil.getMyString(R.string.message_add_stoc_count_before_after_sign) + registCount;

                                // 数量を加算の確認
                                new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                                        .setCancelable(false)
                                        .setTitle(R.string.alert_dialog_title_confirm)
                                        .setMessage(CKUtil.getMyString(R.string.message_already_regsited_same_limitday_item) + msg)
                                        .setPositiveButton(
                                                R.string.alert_dialog_button_yes,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int whitch) {
                                                        // 更新
                                                        dismissDialogAfterUpdateData();
                                                    }
                                                })
                                        .setNegativeButton(
                                                R.string.alert_dialog_button_no,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int whitch) {
                                                        // 何もしない。
                                                    }
                                                })
                                        .show();
                            }
                        }
                    }
                }
            }
        });

        //閉じるボタン押下時はダイアログを消す
        mBtClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeLog();

                dialog.dismiss();

                // 呼出し側の処理
                mlistener.onDialogEditStockButtonCancelClick();
            }
        });

        // キーボード非表示
        CKUtil.hideKeyboad(view);

        return dialog;
    }

    // 更新して画面を閉じる
    private void dismissDialogAfterUpdateData() {
        // 追加
        mItemStock.upsertData();
        // 商品情報更新をWEBへ通知
        CKUtil.postToWebAboutLocalInfo();
        // 追加メッセージ
        CKUtil.showLongToast(getString(R.string.message_add_stock_list));
        dialog.dismiss();
        // 呼出し側の処理
        mlistener.onDialogEditStockButtonRegistClick(mItemStock.getLimitDate());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.gc();
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
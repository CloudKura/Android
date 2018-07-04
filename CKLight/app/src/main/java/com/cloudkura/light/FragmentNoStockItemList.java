package com.cloudkura.light;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class FragmentNoStockItemList extends Fragment {

    // 親Activity
    ActivityMain mParentActivity;

    // 画面の部品
    Dialog dialog;
    View mView;

    AbsListView mAbsListView;
    NoStockItemListAdapter mAdapter;
    ArrayList<Map<String, String>> mListItems;
    Switch mSwDisplayCheckedItem;

    // 表示設定欄
    LinearLayout mLlSettingLayer;
    TextView mTvfragmentNoStockListTitle;

    // 入力欄表示行の存在判定
    boolean mIsOpenInputLayer = false;

    // 先頭のIID
    String mTopIid = "";

    // 行情報
    static class ViewHolder {
        LinearLayout llItemLayer;
        ImageView ivImage;
        TextView tvItemName;
        TextView tvItemBarcode;
        ImageView ivCheck;
        LinearLayout llInputLayer;
        MyDatePicker dtLimitDate;
        NumberPicker npCount10;
        NumberPicker npCount01;
        Button btCancel;
        Button btRegist;
        Button btRakuten;
        Button btYahoo;
        ImageButton btDeleteItem;

        String iid = "";
    }

    public FragmentNoStockItemList() { }

    public static FragmentNoStockItemList newInstance() {
        return new FragmentNoStockItemList();
    }

    // ログ書き出し
    private void writeLog() {
        CKFB.writeLog(this.getClass().getSimpleName());
    }
    private void writeLog(String value) {
        CKFB.writeLog(this.getClass().getSimpleName(), value);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // xmlファイルとの紐付け
        mView = inflater.inflate(R.layout.fragment_no_stock_item_list, null, false);

        //データ無し時に表示するView
        final TextView tvEmptyView = (TextView) mView.findViewById(R.id.fragment_no_stock_item_empty_view);
        //リストの準備
        mAbsListView = (ListView) mView.findViewById(R.id.fragment_no_stock_list_view);
        mAbsListView.setEmptyView(tvEmptyView);

        // 設定欄
        mLlSettingLayer = (LinearLayout) mView.findViewById(R.id.fragment_no_stock_item_list_settings_layer);
        mTvfragmentNoStockListTitle = (TextView) mView.findViewById(R.id.fragment_no_stock_list_title);

        // 初期表示
        initNoStockItemList();

        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mParentActivity = (ActivityMain) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    // 一覧リフレッシュ
    public void refreshNoStockItemList() {
        initNoStockItemList();
    }

    // View項目からDA～への反映
    private Map<String, String> setDAItemValue(Map<String, String> rowValue) {

        Map<String, String> daItem = CKDBUtil.getDAItem().getUpsertByIId(rowValue.get(DAItem.col_IId));
        daItem.put(DAItem.col_Explain, rowValue.get(DAItem.col_Explain));
        daItem.put(DAItem.col_ImagePath, rowValue.get(DAItem.col_ImagePath));
        daItem.put(DAItem.col_ItemType, rowValue.get(DAItem.col_ItemType));
        daItem.put(DAItem.col_Calorie, rowValue.get(DAItem.col_Calorie));
        daItem.put(DAItem.col_Barcode, rowValue.get(DAItem.col_Barcode));
        daItem.put(DAItem.col_IsHandFlag, "0");
        daItem.put(DAItem.col_IsHidden, rowValue.get(DAItem.col_IsHidden));

        return daItem;
    }
    private Map<String, String> setDAItemStockValue(Map<String, String> rowValue) {

        Map<String, String> daItemStock = CKDBUtil.getDAItemStock().getItemByKey(rowValue.get(DAItem.col_IId), DAItemStock.val_ShoppingList);
        daItemStock.put(DAItemStock.col_ItemCount, rowValue.get(DAItemStock.col_ItemCount));
        daItemStock.put(DAItemStock.col_Memo, rowValue.get(DAItemStock.col_Memo));
        daItemStock.put(DAItemStock.col_IsHidden, rowValue.get(DAItemStock.col_IsHidden));

        return daItemStock;
    }

    // 在庫なし商品一覧の初期化
    private void initNoStockItemList() {

        //一覧へ表示
        /*
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }
        if (mAbsListView != null) {
            mAbsListView.setAdapter(null);
        }
        */

        mIsOpenInputLayer = false;

        // 表示データを抽出(追加済み商品の表示考慮)
        DASettings daSettings = CKDBUtil.getDASettings();
        mListItems = CKDBUtil.getDAItemStock().getNoStockItemList(daSettings.isDisplayCheckedItem());
        if (mAdapter == null) {
            mAdapter = new NoStockItemListAdapter(getContext(), R.layout.row_no_stock_item, mListItems);
            mAbsListView.setAdapter(mAdapter);
        } else {
            mAdapter.clear();
            mAdapter.addAll(mListItems);
            mAdapter.notifyDataSetChanged();
        }
        // 設定欄の表示制御
        if (CKDBUtil.getDAItem().isExistFillItem()) {
            mLlSettingLayer.setVisibility(View.VISIBLE);
            mTvfragmentNoStockListTitle.setVisibility(View.VISIBLE);
        } else {
            mLlSettingLayer.setVisibility(View.GONE);
            mTvfragmentNoStockListTitle.setVisibility(View.GONE);
        }

        //mAdapter = new NoStockItemListAdapter(getContext(), R.layout.row_no_stock_item, mListItems);
        //mAbsListView.setAdapter(mAdapter);

        /*
        // タップ時のイベント
        mAbsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                ViewHolder holder = (ViewHolder) mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition()).getTag();

                DAItemStock itemStock = CKDBUtil.getDAItemStock();
                itemStock.setAllValue(getItem(position));
                if (itemStock.getValue(DAItem.col_IsHandFlag).equals(DAItem.val_HandOn)) {
                    // 商品追加済みの場合は補充リストからの削除を確認
                    new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle(R.string.alert_dialog_title_confirm)
                            .setMessage(R.string.message_confirm_delete)
                            .setPositiveButton(
                                    R.string.alert_dialog_button_yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            DAItem delItem = setDAItemValue((Map<String, String>) mAbsListView.getItemAtPosition(position));
                                            delItem.setValue(DAItem.col_ItemType, DAItem.val_ItemType_Normal);
                                            delItem.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOff);
                                            delItem.upsertData();
                                        }
                                    })
                            .show();
                } else {
                    // 補充おすすめ通知画面を表示
                    String dialogTag = "notice_affiliate";
                    if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                        Bundle arg = new Bundle();
                        arg.putString(DialogDeleteAffiliate.ARG_ITEM_ID, holder.tvItemBarcode.getText().toString());
                        arg.putSerializable(DialogDeleteAffiliate.ARG_DELETE_TYPE, DialogDeleteNotice.enmDeleteType.deleteEachItem);
                        arg.putBoolean(DialogDeleteAffiliate.ARG_FROM_NO_STOCK_LIST, true);

                        DialogDeleteAffiliate dialogDeleteAffiliate = new DialogDeleteAffiliate();
                        dialogDeleteAffiliate.setCancelable(false);
                        dialogDeleteAffiliate.setArguments(arg);
                        dialogDeleteAffiliate.show(getFragmentManager(), dialogTag);
                    }
                }
            }
        });
        */

        // 設定欄の値設定
        setSettingLayerValue();

        //スクロール時のイベント
        mAbsListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                // 上下タブの表示制御
                switch (scrollState) {
                    // スクロールが停止したら表示
                    case SCROLL_STATE_IDLE:
                        // 入力欄表示中はfabを非表示
                        if (! mIsOpenInputLayer) {
                            mParentActivity.setFabVisibility(true);
                        }
                        // 先頭のIIdを保存
                        ViewHolder viewHolder = (ViewHolder) mAbsListView.getChildAt(0).getTag();
                        mTopIid = viewHolder.iid;

                        break;

                    // スクロール中は非表示
                    case SCROLL_STATE_TOUCH_SCROLL:
                    case SCROLL_STATE_FLING:
                        mParentActivity.setFabVisibility(false);
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    // 設定欄の値設定
    private void setSettingLayerValue() {

        // 追加済み商品の表示設定
        if (mSwDisplayCheckedItem == null) {
            mSwDisplayCheckedItem = (Switch) mView.findViewById(R.id.fragment_no_stock_list_display_checked_item);
        }
        mSwDisplayCheckedItem.setOnCheckedChangeListener(null);
        mSwDisplayCheckedItem.setChecked(CKDBUtil.getDASettings().isDisplayCheckedItem());
        mSwDisplayCheckedItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 設定値を保存
                DASettings daSettings = CKDBUtil.getDASettings();
                daSettings.setValue(DASettings.col_Id, DASettings.key_IsDisplayCheckedItem);
                daSettings.setValue(DASettings.col_IsHidden, isChecked ? DASettings.val_DisplayCheckedItemOn : DASettings.val_DisplayCheckedItemOff);
                daSettings.upsertData();

                // 画面再表示
                mAdapter.refreshList();
            }
        });
    }

    // 在庫なし商品を表示するためのAdapterを定義
    private class NoStockItemListAdapter extends ArrayAdapter<Map<String, String>> {

        private LayoutInflater inflater;
        private int inflate_item_id;
        private int regPosition = -1;
        private String regRegistCount = "";
        private String regLimitDate = "";

        private ViewHolder mBaseHolder;

        NoStockItemListAdapter(Context context, int id, ArrayList<Map<String, String>> items) {
            super(context, id, items);

            inflate_item_id = id;
            inflater = LayoutInflater.from(context);
        }

        @Override
        @NonNull
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

            // 品目・ストック
            Map<String, String> itemStock = getItem(position);

            // View生成によるメモリ消費を抑えるためにHolderクラスへViewを保持
            ViewHolder holder;
            if (convertView == null) {

                convertView = inflater.inflate(inflate_item_id, parent, false);

                holder = new ViewHolder();

                    holder.llItemLayer = (LinearLayout) convertView.findViewById(R.id.row_no_stock_item_layer);
                    // 画像の表示
                    holder.ivImage = (ImageView) convertView.findViewById(R.id.row_no_stock_item_thumbnail_photo);
                    // 商品名
                    holder.tvItemName = (TextView) convertView.findViewById(R.id.row_no_stock_item_name);
                    // バーコード
                    holder.tvItemBarcode = (TextView) convertView.findViewById(R.id.row_no_stock_item_barcode);
                    // チェックアイコン変更
                    holder.ivCheck = (ImageView) convertView.findViewById(R.id.row_no_stock_item_show_input);

                    // 入力欄はデフォルト非表示
                    holder.llInputLayer = (LinearLayout) convertView.findViewById(R.id.row_no_stock_item_input_layer);
                    holder.llInputLayer.setVisibility(View.GONE);

                    holder.dtLimitDate = (MyDatePicker) convertView.findViewById(R.id.row_no_stock_limit_date);
                    holder.npCount10 = (NumberPicker) convertView.findViewById(R.id.row_no_stock_count_10);
                    holder.npCount01 = (NumberPicker) convertView.findViewById(R.id.row_no_stock_count_1);
                    //holder.npCount10 = (MyNumberPicker) convertView.findViewById(R.id.row_no_stock_count_10);
                    //holder.npCount01 = (MyNumberPicker) convertView.findViewById(R.id.row_no_stock_count_1);

                    holder.btCancel = (Button) convertView.findViewById(R.id.fragment_item_new_prev_button);
                    holder.btCancel.setText(CKUtil.getMyString(R.string.regist_cancel_button));
                    holder.btCancel.setTextSize(16);

                    holder.btRegist = (Button) convertView.findViewById(R.id.fragment_item_new_next_button);
                    holder.btRegist.setText(CKUtil.getMyString(R.string.regist_add_list_button));
                    holder.btRegist.setTextSize(16);

                    holder.btRakuten = (Button) convertView.findViewById(R.id.row_no_stock_button_rakuten);
                    holder.btYahoo = (Button) convertView.findViewById(R.id.row_no_stock_button_yahoo);
                    holder.btDeleteItem = (ImageButton) convertView.findViewById(R.id.row_no_stock_item_delete);

                // IID保存
                holder.iid = itemStock.get(DAItem.col_IId);

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.tvItemName.setText(itemStock.get(DAItem.col_Explain));
            holder.tvItemBarcode.setText(itemStock.get(DAItem.col_Barcode));
            holder.ivImage.setImageBitmap(CKUtil.getBitmap(itemStock.get(DAItem.col_ImagePath)));
            holder.ivCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View currentView = mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition());
                    // 状態変更
                    ViewHolder holder = (ViewHolder) currentView.getTag();
                    if (holder != null) {

                        Map<String, String> itemStock = getItem(position);
                        if (itemStock.get(DAItemStock.col_IsHidden).equals(DAItem.val_HiddenOn)) {

                            // 商品追加済みであれば解除
                            DAItemStock updateStock = CKDBUtil.getDAItemStock();
                            updateStock.setAllValue(setDAItemStockValue(itemStock));
                            updateStock.setValue(DAItemStock.col_IsHidden, DAItem.val_HiddenOff);
                            updateStock.upsertData();

                            // 再取得して描画更新
                            refreshList();
                            // List最終行へ移動
                            mAbsListView.setSelection(mAbsListView.getCount());

                            // 選択済みIidを先頭に表示
                            mTopIid = holder.iid;
                            moveToSelectPosition(mTopIid);

                        } else {
                            // 選択状態の切り替え
                            if (itemStock.get(DAItem.col_IsHandFlag).equals(DAItem.val_HandOn)) {
                                mIsOpenInputLayer = false;
                                // 入力欄を開いた状態を覚える
                                itemStock.put(DAItem.col_IsHandFlag, DAItem.val_HandOff);
                            } else {
                                mIsOpenInputLayer = true;
                                // 入力欄を開いた状態を覚える
                                itemStock.put(DAItem.col_IsHandFlag, DAItem.val_HandOn);
                            }
                            currentView.setTag(holder);
                            mListItems.set(position, itemStock);
                            // 行の表示設定
                            setAppearanceItemRow(currentView, position);

                            mTopIid = "";
                            if(itemStock.get(DAItem.col_IsHandFlag).equals(DAItem.val_HandOn)) {
                                // 選択済みIidを先頭に表示
                                mTopIid = holder.iid;
                                moveToSelectPosition(mTopIid);
                            }
                        }
                        // fab制御
                        mParentActivity.setFabVisibility(! mIsOpenInputLayer);
                    }

                }
            });

            holder.npCount01.setTag(position);
            holder.btCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 入力欄を閉じる
                    setInputAreaViewGone(position);
                }
            });

            holder.btRegist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ViewHolder viewHolder = (ViewHolder) mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition()).getTag();
                    if (viewHolder == null) {
                        return;
                    }

                    // 登録チェック
                    String registCount = String.valueOf(viewHolder.npCount10.getValue() * 10 + viewHolder.npCount01.getValue());
                    String limitDate = CKUtil.getFormatDate(viewHolder.dtLimitDate.getYear(), viewHolder.dtLimitDate.getMonth() + 1, viewHolder.dtLimitDate.getDayOfMonth());
                    checkRegistableItemStockCount(position, registCount, limitDate);
                }
            });

            // 楽天ボタン
            holder.btRakuten.setOnTouchListener(CKUtil.doOnTouchAnimation());
            holder.btRakuten.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    ViewHolder viewHolder = (ViewHolder) mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition()).getTag();
                    if (viewHolder == null) {
                        return;
                    }
                    String barcode = viewHolder.tvItemBarcode.getText().toString();
                    writeLog("fill-buy_rakuten: " + barcode);
                    CKUtil.openURL(CKUtil.getMyString(R.string.weburl1) + barcode + "/");
                }
            });

            // Yahooボタン
            holder.btYahoo.setOnTouchListener(CKUtil.doOnTouchAnimation());
            holder.btYahoo.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    ViewHolder viewHolder = (ViewHolder) mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition()).getTag();
                    if (viewHolder == null) {
                        return;
                    }

                    String barcode = viewHolder.tvItemBarcode.getText().toString();
                    writeLog("fill-buy_yahoo: " + barcode);
                    CKUtil.openURL(CKUtil.getMyString(R.string.weburl2) + barcode);
                }
            });

            holder.btDeleteItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 商品追加済みの場合は補充リストからの削除を確認
                    new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle(R.string.alert_dialog_title_confirm)
                            .setMessage(R.string.message_confirm_hide)
                            .setPositiveButton(
                                    R.string.alert_dialog_button_yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            DAItem delItem = CKDBUtil.getDAItem();
                                            delItem.setAllValue(setDAItemValue((Map<String, String>) mAbsListView.getItemAtPosition(position)));
                                            delItem.setValue(DAItem.col_ItemType, DAItem.val_ItemType_Normal);
                                            delItem.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOff);
                                            delItem.upsertData();
                                            // 表示リフレッシュ
                                            refreshList();
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
            });

            // 行の表示設定
            setAppearanceItemRow(convertView, position);

            return convertView;
        }

        // 指定IIdを画面先頭へ表示
        private void moveToSelectPosition(String iid) {

            for (int position = 0; position < mAdapter.getCount(); position++) {
                DAItemStock itemStock = CKDBUtil.getDAItemStock();
                itemStock.setAllValue(mAdapter.getItem(position));
                if (position > mAbsListView.getChildCount()) {
                    mAbsListView.setSelection(0);
                    break;
                } else {
                    if (itemStock.getValue(DAItemStock.col_IId).equals(iid)) {
                        mAbsListView.setSelection(position);
                        break;
                    }
                }
            }
        }

        // 画面表示リフレッシュ
        private void refreshList() {
            initNoStockItemList();
            mIsOpenInputLayer = false;
            //mAdapter.notifyDataSetChanged();

            // 選択済みIidを先頭に表示
            moveToSelectPosition(mTopIid);
        }

        // 指定された行の商品を商品リストへ追加
        private void registToItemList(int position, String limitDate, String registCount) {
            // 補充リスト用の行は非表示チェックONして行の表示を補充済みに変更

            Map<String, String> nowValue = (Map<String, String>) mAbsListView.getItemAtPosition(position);

            DAItem daItem = CKDBUtil.getDAItem();
            daItem.setAllValue(setDAItemValue(nowValue));
            daItem.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOn);
            daItem.upsertData();

            // 日付・数量をストックに保存（商品リストへ追加用）
            DAItemStock itemStock = CKDBUtil.getDAItemStock();
            limitDate = CKUtil.getSystemFormatDate(CKUtil.isDate(limitDate));

            Map<String, String> daRegStock = itemStock.getItemByKey(nowValue.get(DAItem.col_IId), limitDate);
            if (daRegStock == null) {
                itemStock.setValue(DAItemStock.col_IId, nowValue.get(DAItem.col_IId));
                itemStock.setValue(DAItemStock.col_LimitDt, limitDate);
            } else {
                itemStock.setAllValue(daRegStock);
            }
            itemStock.setValue(DAItemStock.col_ItemCount, registCount);
            itemStock.setValue(DAItemStock.col_IsHidden, DAItemStock.val_HiddenOff);
            itemStock.upsertData();

            // 補充リストから商品リストへの追加回数をカウントアップ
            String addCount = itemStock.getAddWishCount(nowValue.get(DAItem.col_IId));
            addCount = String.valueOf((Integer.valueOf(addCount)) + 1);

            // 補充リスト用のデータ更新
            itemStock.setAllValue(itemStock.getItemByKey(nowValue.get(DAItem.col_IId), DAItemStock.val_ShoppingList));
            itemStock.setValue(DAItemStock.col_ItemCount, addCount);
            itemStock.setValue(DAItemStock.col_IsHidden, DAItemStock.val_HiddenOn);
            itemStock.upsertData();

            // 入力欄を閉じる
            setInputAreaViewGone(position);

            // 商品タブの商品詳細欄を閉じる
            mParentActivity.setItemDetailFooterVisibility(false);

            // リフレッシュ
            mParentActivity.refreshRelationInfo();

            // 通知
            CKUtil.showLongToast(CKUtil.getMyString(R.string.message_add_stock_list));
        }

        // 数量登録時のチェック
        private void checkRegistableItemStockCount(int position, String registCount, String limitDate) {

            // 数量
            if (registCount == null || registCount.equals("") || registCount.equals("0")) {
                new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                        .setCancelable(false)
                        .setTitle(R.string.message_title_count)
                        .setMessage(R.string.message_input_count)
                        .setPositiveButton(
                                R.string.alert_dialog_button_yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whitch) {
                                        // 数量入力を促すのでキーボードを隠す
                                        CKUtil.hideKeyboad(mView);
                                    }
                                })
                        .show();
                return;
            }

            DAItemStock itemStock = CKDBUtil.getDAItemStock();
            itemStock.setAllValue((Map<String, String>) mAbsListView.getItemAtPosition(position));

            // 画面で入力された消費期限で同一キーの判定
            Map<String, String> existData = CKDBUtil.getDAItemStock().getItemByKey(itemStock.getValue(DAItemStock.col_IId), limitDate);
            if (existData == null) {
                // 同一キー未登録なら保存可能
                registToItemList(position, limitDate, registCount);
                return;
            } else {

                String preCount = existData.get(DAItemStock.col_ItemCount);
                if (preCount == null || preCount.equals("")) {
                    preCount = "0";
                }
                registCount = String.valueOf(Integer.valueOf(preCount) + Integer.valueOf(registCount));

                if (Integer.valueOf(registCount) > CKDBUtil.MAX_ITEM_COUNT) {
                    // 追加不可を通知する。
                    new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle(CKUtil.getMyString(R.string.message_add_stock_limit_day) + itemStock.getLimitDate())
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
                    if (preCount.equals("0")) {
                        // 同一キー登録ありでも数量0なら保存可能
                        registToItemList(position, limitDate, registCount);
                        return;
                    } else {

                        String msg = CKUtil.getMyString(R.string.message_add_stock_limit_day) + itemStock.getLimitDate() + "\n" +
                                CKUtil.getMyString(R.string.message_add_stock_count) + preCount +
                                CKUtil.getMyString(R.string.message_add_stoc_count_before_after_sign) + registCount;

                        regPosition = position;
                        regLimitDate = limitDate;
                        regRegistCount = registCount;

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
                                                registToItemList(regPosition, regLimitDate, regRegistCount);
                                                regPosition = -1;
                                                regLimitDate = "";
                                                regRegistCount = "";

                                                // 更新
                                                mParentActivity.refreshRelationInfo();
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

                return;
            }
        }

        // 指定位置の入力欄を閉じる
        public void setInputAreaViewGone(int position) {
            View view = mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition());
            if (view != null) {
                LinearLayout inputLayer = (LinearLayout) view.findViewById(R.id.row_no_stock_item_input_layer);
                if (inputLayer.getVisibility() == View.VISIBLE) {
                    ImageView isHand = (ImageView) view.findViewById(R.id.row_no_stock_item_show_input);
                    inputLayer.setVisibility(View.GONE);
                    isHand.setImageDrawable(CKUtil.getMyDrawable(R.drawable.ic_check_box_off));
                }
            }
            DAItemStock itemStock = CKDBUtil.getDAItemStock();
            itemStock.setAllValue(getItem(position));
            itemStock.setValue(DAItem.col_IsHandFlag, DAItem.val_HandOff);
            mListItems.set(position, itemStock.getAllValue());
        }

        // 商品リスト追加済みの行の表示制御
        private void setAppearanceItemRow(View currentView, int position) {

            if (currentView == null) {
                return;
            }

            ViewHolder viewHolder = (ViewHolder) currentView.getTag();
            if (viewHolder == null) {
                return;
            }

            DAItemStock itemStock = CKDBUtil.getDAItemStock();
            itemStock.setAllValue(getItem(position));

            currentView.setVisibility(View.VISIBLE);
            TextPaint paint = viewHolder.tvItemName.getPaint();
            if (itemStock.getValue(DAItemStock.col_IsHidden).equals(DAItem.val_HiddenOn)) {
                // 入力欄は必ず非表示
                viewHolder.llInputLayer.setVisibility(View.GONE);
                // 取消線表示
                viewHolder.ivCheck.setImageDrawable(CKUtil.getMyDrawable(R.drawable.ic_check_box_on_done));
                viewHolder.tvItemName.setTextColor(CKUtil.getMyColor(R.color.colorCloseText));
                currentView.setBackgroundColor(CKUtil.getMyColor(R.color.colorRegistDialogBackground));
                paint.setFlags(viewHolder.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                // チェックを更新＆入力欄の表示連動
                PropertyValuesHolder holderAlpha;
                viewHolder.tvItemName.setTextColor(CKUtil.getMyColor(R.color.colorNormal));
                currentView.setBackgroundColor(CKUtil.getMyColor(R.color.colorTabUnder));
                paint.setFlags(viewHolder.tvItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

                if (! itemStock.getValue(DAItem.col_IsHandFlag).equals(DAItem.val_HandOn)) {
                    viewHolder.llInputLayer.setVisibility(View.GONE);
                    viewHolder.ivCheck.setImageDrawable(CKUtil.getMyDrawable(R.drawable.ic_check_box_off));

                    holderAlpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
                } else {
                    // 自行以外の入力欄をすべて閉じる
                    for (int listPos = 0; listPos < mAbsListView.getCount(); listPos++) {
                        if (position != listPos) {
                            setInputAreaViewGone(listPos);
                        }
                    }

                    viewHolder.llInputLayer.setVisibility(View.VISIBLE);
                    viewHolder.ivCheck.setImageDrawable(CKUtil.getMyDrawable(R.drawable.ic_check_box_on));

                    holderAlpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);

                    // チェックされた行の入力欄の設定
                    setInputLayerContents(currentView, position);
                }

                ObjectAnimator anime = ObjectAnimator.ofPropertyValuesHolder(viewHolder.llInputLayer, holderAlpha);
                anime.setDuration(300);
                anime.start();
            }
            paint.setAntiAlias(true);

        }

        // コントロールイベント
        private void setInputLayerContents(View currentView, int position) {

            if (currentView == null) {
                return;
            }

            ViewHolder holder = (ViewHolder) currentView.getTag();

            // 入力制御
            holder.npCount10.setMinValue(0);
            holder.npCount10.setMaxValue(9);
            holder.npCount01.setMinValue(0);
            holder.npCount01.setMaxValue(9);

            // dividerの色変更
            CKUtil.setNumberPickerDividerNone(holder.npCount10);
            CKUtil.setNumberPickerDividerNone(holder.npCount01);

            DAItemStock itemStock = CKDBUtil.getDAItemStock();
            itemStock.setAllValue((Map<String, String>) mAbsListView.getItemAtPosition(position));

            // 消費期限
            Calendar calendarToday = Calendar.getInstance();
            String limitDate = itemStock.getLimitDate();
            if (limitDate == null || limitDate.equals("")
                    || CKUtil.getSystemFormatDate(CKUtil.isDate(limitDate)).equals(DAItemStock.val_ShoppingList) ) {
                limitDate = CKUtil.getCurrentDate();
                itemStock.setLimitDate(limitDate);
            }
            calendarToday.setTime(CKUtil.isDate(limitDate));
            int year = calendarToday.get(Calendar.YEAR);
            int month = calendarToday.get(Calendar.MONTH);
            int day = calendarToday.get(Calendar.DAY_OF_MONTH);
            holder.dtLimitDate.init(year, month, day, new MyDatePicker.OnDateChangedListener(){
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                }
            });

            // 数量(1固定)
            holder.npCount10.setValue(0);
            holder.npCount01.setValue(1);
            /*
            String itemCountBuff = itemStock.getValue(DAItemStock.col_ItemCount);
            if (itemCountBuff.equals("0")) {
                itemCountBuff = "1";
            }
            int count10 = 0;
            int count01 = 1;
            try {
                if (itemCountBuff.length() == 2) {
                    count10 = Integer.valueOf(itemCountBuff.substring(0,1));
                    count01 = Integer.valueOf(itemCountBuff.substring(1,2));
                } else {
                    count01 = Integer.valueOf(itemCountBuff.substring(0,1));
                }
            } catch (Exception ex) {
                //
            }

            holder.npCount10.setValue(count10);
            holder.npCount01.setValue(count01);
            */

            // 数量Picker変更
            holder.npCount01.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                    int position = (int) picker.getTag();
                    ViewHolder viewHolder = (ViewHolder) mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition()).getTag();

                    if (oldVal == viewHolder.npCount01.getMaxValue() && newVal == viewHolder.npCount01.getMinValue()) {
                        int val10 = viewHolder.npCount10.getValue();
                        if (val10 < viewHolder.npCount10.getMaxValue()) {
                            val10++;
                            viewHolder.npCount10.setValue(val10);
                        }
                    } else if (oldVal == viewHolder.npCount01.getMinValue() && newVal == viewHolder.npCount01.getMaxValue()) {
                        int val10 = viewHolder.npCount10.getValue();
                        if (val10 > viewHolder.npCount10.getMinValue()) {
                            val10--;
                            viewHolder.npCount10.setValue(val10);
                        }
                    }
                }
            });

        }
    }
}

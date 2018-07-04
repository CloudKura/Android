package com.cloudkura.light;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class FragmentItemList extends android.support.v4.app.Fragment
    implements DialogAddItemStock.OnDialogEditStockListener
{

    // 編集モード選択値
    private enum EditMode{
        NORMAL,
        DELETE_SELECT
    }

    // 親Activity
    static ActivityMain mParentActivity;

    // 画面の部品
    View mView;
    AbsListView mAbsListView;
    ItemListAdapter mAdapter;
    LinearLayout mLlItemDetailFooter;
    Button mBtItemDetailClose;
    //SwipeRefreshLayout mSwipeRefreshLayout;

    // 表示切替
    Switch mSwOrderByItem;
    Switch mSwDisplayLimitDate;

    // 表示設定欄
    LinearLayout mLlSettingLayer;

    // 商品詳細欄
    View mVwLimitMark;
    TextView mTvItemCount;
    TextView mTvLimitDate;
    TextView mTvRemainDaysLabel;
    TextView mTvRemainDays;
    MyDatePicker mDtLimitDate;
    NumberPicker mNpCount10;
    NumberPicker mNpCount01;
    Button mBtEditStockCancel;
    Button mBtEditStockRegist;
    Button mBtEditItem;
    Button mBtDeleteItem;

    // 選択位置
    int nowPosition;
    String mActiveIId = "";
    ObjectSearchCondition mSearchCondition = new ObjectSearchCondition();
    ArrayList<Map<String, String>> mListItems;

    // 選択項目の値を保持
    Map<String, String> mSelectedItemValue;          // 画面編集値
    Map<String, String> mSelectedItemValueBackup;   // 保存用

    // 商品詳細の表示状態を保持
    boolean mIsDetailShow = false;

    // 商品欄の期限の表示状態を保持
    boolean mIsDisplayLimitDate = false;

    // 商品詳細欄の部品
    TextView mTvMemo;
    ImageView mImgView;
    TextView mTvBarcode;
    LinearLayout mLlDisplayStockLayer;
    LinearLayout mLlEditStockLayer;

    // ウィッシュ追加
    Switch mSwAddFillOnOff;

    // 画像の大きさを保持(grid_item_info.xmlの設定値と合わせる)
    int mImageWidth = 120;
    int mImageHeight = 120;

    // カードアニメーションの選択値
    private enum CardAnim{
        UPDATE,
        DELETE
    }

    public FragmentItemList() { }

    public static FragmentItemList newInstance() {
        return new FragmentItemList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // xmlファイルとの紐付け
        mView = inflater.inflate(R.layout.fragment_item_list, container, false);

        //データ無し時に表示するView
        final TextView tvEmptyView = (TextView) mView.findViewById(R.id.fragment_item_empty_view);
        //リストの準備
        mAbsListView = (GridView) mView.findViewById(R.id.fragment_item_list_grid_view);
        mAbsListView.setEmptyView(tvEmptyView);

        // 設定欄
        mLlSettingLayer = (LinearLayout) mView.findViewById(R.id.fragment_item_list_settings_layer);

        // 設定欄の表示設定
        setSettingLayerValue();

        /* ToDo: PullRefresh
        // SwipeRefreshLayoutの設定
        // https://dev.classmethod.jp/smartphone/swiperefreshlayout/
        SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Web同期
                        mParentActivity.syncWebDB();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                    // 待機
                }, 3500);
            }
        };

        mSwipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.fragment_item_list_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorInfo, R.color.colorAlert, R.color.colorExclamation);
        */
        //ListViewへの一覧設定
        setItemListView();

        // 商品詳細(最初は隠す)
        mLlItemDetailFooter = (LinearLayout) mView.findViewById(R.id.fragment_item_detail_footer);
        mLlItemDetailFooter.setVisibility(View.GONE);

        // 商品詳細の部品
        // 商品情報
        mTvMemo = (TextView) mView.findViewById(R.id.fragment_item_list_memo);
        mImgView = (ImageView) mView.findViewById(R.id.fragment_item_list_image);
        mTvBarcode = (TextView) mView.findViewById(R.id. fragment_item_list_barcode);
        mSwAddFillOnOff = (Switch) mView.findViewById(R.id.fragment_item_list_add_fill_onoff);
        mBtEditItem = (Button) mView.findViewById(R.id.fragment_item_list_edit_stock);
        mBtDeleteItem = (Button) mView.findViewById(R.id.fragment_item_list_delete_item);
        // ストック情報(表示)
        mLlDisplayStockLayer = (LinearLayout) mView.findViewById(R.id.row_item_list_stock_display_layer);
        mVwLimitMark = mView.findViewById((R.id.row_stock_limit_mark)) ;
        mTvItemCount = (TextView) mView.findViewById((R.id.row_stock_item_count)) ;
        mTvLimitDate = (TextView) mView.findViewById((R.id.row_item_info_limit_date)) ;
        mTvRemainDaysLabel = (TextView) mView.findViewById((R.id.row_item_info_remain_days_label)) ;
        mTvRemainDays = (TextView) mView.findViewById((R.id.row_item_info_remain_days)) ;
        // ストック情報(編集)
        mLlEditStockLayer = (LinearLayout) mView.findViewById(R.id.row_item_list_stock_edit_layer);
        mDtLimitDate = (MyDatePicker) mView.findViewById(R.id.row_no_stock_limit_date);
        mNpCount10 = (NumberPicker) mView.findViewById(R.id.row_no_stock_count_10);
        mNpCount01 = (NumberPicker) mView.findViewById(R.id.row_no_stock_count_1);
        //mNpCount10 = (MyNumberPicker) mView.findViewById(R.id.row_no_stock_count_10);
        //mNpCount01 = (MyNumberPicker) mView.findViewById(R.id.row_no_stock_count_1);
        mBtEditStockCancel = (Button) mView.findViewById(R.id.fragment_item_new_prev_button);
        mBtEditStockRegist = (Button) mView.findViewById(R.id.fragment_item_new_next_button);

        // ストック編集欄は初期非表示
        switchModeDisplayEditStock(false);

        // 商品詳細フッター内のオブジェクト
        // ×ボタン
        mBtItemDetailClose = (Button) mView.findViewById(R.id.fragment_item_list_close_button);
        mBtItemDetailClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 商品詳細フッターを非表示
                setItemDetailFooterVisibility(false);
            }
        });

        /*
        // ストック追加ボタン
        mBtAddStock = (Button) mView.findViewById(R.id.fragment_item_list_add_stock_button);
        mBtAddStock.setOnTouchListener(CKUtil.doOnTouchAnimation());
        // 消費期限単位の表示の場合は商品追加ボタンを非表示
        mBtAddStock.setVisibility(CKUtil.isShowDisplayGroupByItem() ? View.VISIBLE : View.GONE);
        mBtAddStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //ストック登録画面を表示
                String dialogTag = "add_stock";
                if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                    // 選択商品の情報を取得
                    DAItem localItem = CKDBUtil.getDAItem();
                    localItem.setAllValue((Map<String, String>) mAbsListView.getItemAtPosition(nowPosition));

                    // 引数設定
                    Bundle arg = new Bundle();
                    arg.putString(DialogAddItemStock.ARG_ITEM_ID, localItem.getValue(DAItem.col_IId));
                    if (mLvStockListView.getCount() == 0) {
                        arg.putString(DialogAddItemStock.ARG_LIMIT_DATE, "");
                    } else {
                        Map<String, String> selectItem = (Map<String, String>) mLvStockListView.getItemAtPosition(0);
                        arg.putString(DialogAddItemStock.ARG_LIMIT_DATE, selectItem.get(DAItemStock.col_LimitDt));
                    }

                    DialogAddItemStock addStock = DialogAddItemStock.newInstance();
                    addStock.setCancelable(false);
                    addStock.setArguments(arg);
                    addStock.setTargetFragment(( (FragmentTab) getParentFragment() ).mFragmentItemList, 0);
                    addStock.show(getFragmentManager(), dialogTag);
                }
            }
        });
        */

        // ストック情報(編集)表示ボタン
        mBtEditItem.setOnTouchListener(CKUtil.doOnTouchAnimation());
        mBtEditItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchModeDisplayEditStock(true);
            }
        });

        // 商品削除ボタン
        mBtDeleteItem.setOnTouchListener(CKUtil.doOnTouchAnimation());
        mBtDeleteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // ストック数量取得
                int stockCount = 0;
                try {
                    stockCount = Integer.parseInt(mSelectedItemValue.get(DAItemStock.col_ItemCount));
                } catch (Exception e) {
                }

                if (stockCount == 1) {
                    // 残数量が1の場合は削除確認メッセージを表示
                    new AlertDialog.Builder(mView.getContext(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle(R.string.alert_dialog_title_confirm)
                            .setMessage(R.string.message_confirm_delete_item)
                            .setPositiveButton(
                                    R.string.alert_dialog_button_yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {

                                            // ストックの数量を0にすることで削除とする。
                                            DAItemStock delStock = CKDBUtil.getDAItemStock();
                                            delStock.setAllValue(delStock.getItemByKey(mSelectedItemValue.get(DAItem.col_IId), mSelectedItemValue.get(DAItemStock.col_LimitDt)));
                                            delStock.setValue(DAItemStock.col_ItemCount, "0");
                                            delStock.upsertData();

                                            // 詳細画面を閉じる
                                            mLlItemDetailFooter.setVisibility(View.GONE);

                                            // 選択位置を初期化
                                            nowPosition = 0;
                                            // 全体リフレッシュ
                                            mParentActivity.refreshRelationInfo();
                                            // fabを再表示
                                            mParentActivity.setFabVisibility(true);

                                            // 画面を閉じる
                                            dialog.dismiss();

                                            // 削除通知画面を表示
                                            String dialogTag = "notice_deleted";
                                            if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                                                ArrayList<String> aryDeleteItemIId = new ArrayList<>();
                                                aryDeleteItemIId.add(mSelectedItemValue.get(DAItem.col_IId));

                                                Bundle arg = new Bundle();
                                                arg.putString(DialogDeleteAffiliate.ARG_ITEM_ID, "");
                                                arg.putStringArrayList(DialogDeleteAffiliate.ARG_DELETE_ITEMS_IID, aryDeleteItemIId);
                                                arg.putSerializable(DialogDeleteAffiliate.ARG_DELETE_TYPE, DialogDeleteNotice.enmDeleteType.deleteAllItem);

                                                DialogDeleteNotice dialogDeleteNotice = new DialogDeleteNotice();
                                                dialogDeleteNotice.setCancelable(false);
                                                dialogDeleteNotice.setArguments(arg);
                                                dialogDeleteNotice.setTargetFragment(( (FragmentTab) getParentFragment() ).mFragmentItemList, 0);
                                                dialogDeleteNotice.show(getParentFragment().getChildFragmentManager(), dialogTag);
                                            }

                                        }
                                    })
                            .setNegativeButton(
                                    R.string.alert_dialog_button_no,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            // 画面を閉じる
                                            dialog.dismiss();
                                        }
                                    })
                            .show();

                } else if (stockCount > 1) {
                    // 残数量が複数ある場合は削除選択画面を表示

                    String dialogTag = "use_list";
                    if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                        // 詳細欄を閉じる
                        setItemDetailFooterVisibility(false);

                        Bundle arg = new Bundle();
                        arg.putString(DialogUseList.ARG_SEARCH_IID, mSelectedItemValue.get(DAItem.col_IId));
                        arg.putString(DialogUseList.ARG_SEARCH_WORD, mSelectedItemValue.get(DAItem.col_Explain));
                        arg.putString(DialogUseList.ARG_SEARCH_LIMITDATE, mSelectedItemValue.get(DAItemStock.col_LimitDt));

                        // 画面呼出し
                        DialogUseList useList = DialogUseList.newInstance();
                        useList.setCancelable(false);
                        useList.setArguments(arg);
                        useList.setTargetFragment(( (FragmentTab) getParentFragment() ).mFragmentItemList, 0);
                        useList.show(getFragmentManager(), dialogTag);
                    }
                }
            }
        });

        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mParentActivity = (ActivityMain) context;
        // 全体リフレッシュ
        //mParentActivity.refreshRelationInfo();
    }

    // 設定欄の表示切替
    private void setSettingLayerValue() {
        if (mSwOrderByItem == null) {
            mSwOrderByItem = (Switch) mView.findViewById(R.id.fragment_item_list_display_order_item);
        }
        mSwOrderByItem.setOnCheckedChangeListener(null);
        mSwOrderByItem.setChecked(CKDBUtil.getDASettings().isItemListDisplayOrderByItem());
        mSwOrderByItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 設定値を保存
                DASettings daSettings = CKDBUtil.getDASettings();
                daSettings.setValue(DASettings.col_Id, DASettings.key_ItemListDisplayOrderByItem);
                daSettings.setValue(DASettings.col_IsHidden, isChecked ? DASettings.val_DisplayCheckedItemOn : DASettings.val_DisplayCheckedItemOff);
                daSettings.upsertData();

                // 画面再表示
                updateDataNotify();
            }
        });

        if (mSwDisplayLimitDate == null) {
            mSwDisplayLimitDate = (Switch) mView.findViewById(R.id.fragment_item_list_display_limit_date);
        }
        mSwDisplayLimitDate.setOnCheckedChangeListener(null);
        mSwDisplayLimitDate.setChecked(CKDBUtil.getDASettings().isItemListDisplayLimitDate());
        mSwDisplayLimitDate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 設定値を保存
                DASettings daSettings = CKDBUtil.getDASettings();
                daSettings.setValue(DASettings.col_Id, DASettings.key_ItemListDisplayLimitDate);
                daSettings.setValue(DASettings.col_IsHidden, isChecked ? DASettings.val_DisplayCheckedItemOn : DASettings.val_DisplayCheckedItemOff);
                daSettings.upsertData();

                // 画面再表示
                updateDataNotify();
            }
        });
    }

    // 商品詳細欄のストックの編集・表示の切替
    private void switchModeDisplayEditStock(boolean isEdit) {

        // アニメーション定義
        // ストック編集欄は初期非表示
        if (isEdit) {
            Animation animEdit = AnimationUtils.loadAnimation(CKUtil.getMyContext(), R.anim.slide_up);
            animEdit.setDuration(300);
            animEdit.setFillBefore(true);
            mLlEditStockLayer.startAnimation(animEdit);
            mLlEditStockLayer.setVisibility(View.VISIBLE);
            mLlDisplayStockLayer.setVisibility(View.GONE);
        } else {
            Animation animDisplay = AnimationUtils.loadAnimation(CKUtil.getMyContext(), R.anim.slide_up);
            animDisplay.setDuration(300);
            animDisplay.setFillBefore(true);
            mLlDisplayStockLayer.startAnimation(animDisplay);
            mLlDisplayStockLayer.setVisibility(View.VISIBLE);
            mLlEditStockLayer.setVisibility(View.GONE);
        }
    }

    // 商品詳細欄の値を設定
    private void setItemDetailValue() {

        // 項目値初期化
        mTvMemo.setText("-");

        // 引数情報を設定
        if (mSelectedItemValue == null) {
            CKUtil.showLongToast(getString(R.string.message_no_detail_info));
        } else {

            // 表示値を取得
            DAItemStock daItemStock = CKDBUtil.getDAItemStock();
            Map<String, String> stockData = daItemStock.getItemDetail(mSelectedItemValue.get(DAItem.col_IId), mSelectedItemValue.get(DAItemStock.col_LimitDt));
            if (stockData == null) {
                return;
            }

            // 説明
            if (stockData.get(DAItemStock.col_Detail_Explain).equals("")) {
                mTvMemo.setText(CKUtil.getMyString(R.string.item_no_name));
            } else {
                mTvMemo.setText(stockData.get(DAItemStock.col_Detail_Explain));
            }
            // バーコード
            mTvBarcode.setText(stockData.get(DAItemStock.col_Detail_Barcode));

            // 画像表示
            try {
                String imagePath = stockData.get(DAItemStock.col_Detail_ImagePath);
                if (imagePath == null || imagePath.equals("")) {
                    mImgView.setImageResource(R.mipmap.no_photo);
                } else {
                    Bitmap bmCamera = CKUtil.getBitmap(imagePath);
                    if (bmCamera == null) {
                        mImgView.setImageResource(R.mipmap.no_photo);
                    } else {
                        mImgView.setImageBitmap(bmCamera);
                    }

                }
            } catch (Exception e) {
                CKUtil.showLongToast(e.getMessage());
            }

            // 補充リスト追加
            DAItem daFillItem = CKDBUtil.getDAItem();
            daFillItem.setAllValue(daFillItem.getUpsertByIId(mSelectedItemValue.get(DAItem.col_IId)));

            // イベントを無効にして値設定、その後イベント再設定
            /*
            mSwAddFillOnOff.setOnCheckedChangeListener(null);
            mSwAddFillOnOff.setChecked(daFillItem.getValue(DAItem.col_ItemType).equals(DAItem.val_ItemType_Fill));
            mSwAddFillOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // 補充リスト表示/非表示を設定
                    DAItem daItem = CKDBUtil.getDAItem();
                    daItem.setAllValue(daItem.getUpsertByIId(mSelectedItemValue.get(DAItem.col_IId)));
                    daItem.setValue(DAItem.col_ItemType, isChecked ? DAItem.val_ItemType_Fill : DAItem.val_ItemType_Normal);
                    daItem.upsertData();
                    // 補充リスト用のストックを設定
                    DAItemStock daItemStock = CKDBUtil.getDAItemStock();
                    daItemStock.setValue(DAItemStock.col_IId, daItem.getValue(DAItem.col_IId));
                    daItemStock.setValue(DAItemStock.col_LimitDt, DAItemStock.val_ShoppingList);
                    //daItemStock.setValue(DAItemStock.col_ItemCount, "1");
                    //daItemStock.setValue(DAItemStock.col_IsHidden, DAItemStock.val_HiddenOff);
                    daItemStock.upsertData();

                    // 補充リストを更新
                    mParentActivity.tabRefreshNoStockItemList();
                }
            });
            */

            // ストック情報設定
            setItemDetailStockInfo();
        }
    }

    //ListViewへの一覧設定
    public void setItemListView() {

        // 並び順
        String orderBy = DAItemStock.col_LimitDt + " asc, " + DAItem.col_ItemType + " asc ";
        if (CKDBUtil.getDASettings().isItemListDisplayOrderByItem()) {
            orderBy = DAItemStock.col_IId + " asc, " + orderBy;
        }

        // 表示データを抽出
        // 商品・消費期限ごとに表示
        DAItemStock daItemStock = CKDBUtil.getDAItemStock();
        mListItems = daItemStock.getEachItemDateBySearchCondition(mSearchCondition, orderBy);

        // 更新されたデータが取得したリスト内のどの位置かを取得
        for (Map<String, String> item : mListItems) {
            if (item.get(DAItemStock.col_IId) != null && item.get(DAItemStock.col_IId).equals(mActiveIId)) {
                nowPosition = mListItems.indexOf(item);
                nowPosition = (nowPosition < 0 ? 0 : nowPosition);
                break;
            }
        }

        // 列数指定と画像サイズ調整
        mImageWidth = (int) ((float)getResources().getDisplayMetrics().widthPixels / 3);
        mImageHeight = mImageWidth;
        ((GridView)mAbsListView).setNumColumns(3);

        //一覧へ表示
        /*
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }
        if (mAbsListView != null) {
            mAbsListView.setAdapter(null);
        }
        mAdapter = new ItemListAdapter(CKUtil.getMyContext(), R.layout.grid_item_info, mListItems);
        */
        if (mAdapter == null) {
            mAdapter = new ItemListAdapter(CKUtil.getMyContext(), R.layout.grid_item_info, mListItems);
            mAbsListView.setAdapter(mAdapter);
        } else {
            mAdapter.clear();
            mAdapter.addAll(mListItems);
            mAdapter.notifyDataSetChanged();
            //mAdapter.notifyDataSetInvalidated();
            //mAbsListView.invalidateViews();
        }

        // 設定欄の表示制御
        mLlSettingLayer.setVisibility(mAbsListView.getCount() == 0 ? View.GONE : View.VISIBLE);

        // カードタップ時のイベント
        mAbsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 選択された項目の値を取得
                nowPosition = position;
                mSelectedItemValue = (Map<String, String>) parent.getItemAtPosition(position);
                mSelectedItemValueBackup = new HashMap<>(mSelectedItemValue);
                // ストックの編集欄を隠す
                switchModeDisplayEditStock(false);
                // 詳細画面を表示する。(表示してから値設定する。逆にすると値設定できず商品なし表示になる)
                setItemDetailFooterVisibility(true);
                setItemDetailValue();
            }
        });

        //スクロール時のイベント
        mAbsListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                // 上下タブの表示制御
                switch (scrollState) {
                    // スクロールが停止したら表示
                    case SCROLL_STATE_IDLE:
                        mParentActivity.setFabVisibility(! mIsDetailShow);
                        break;

                    // スクロール中は非表示
                    case SCROLL_STATE_TOUCH_SCROLL:
                    case SCROLL_STATE_FLING:
                        mParentActivity.setFabVisibility(false);
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
    }

    // 指定された行の商品を商品リストへ追加
    private void registToItemList() {

        // 日付・数量をストックに保存（商品リストへ追加用）
        DAItemStock itemStock = CKDBUtil.getDAItemStock();
        itemStock.setValue(DAItemStock.col_IId, mSelectedItemValue.get(DAItem.col_IId));
        itemStock.setValue(DAItemStock.col_LimitDt,  CKUtil.getSystemFormatDate(CKUtil.isSystemFormatDate(mSelectedItemValue.get(DAItemStock.col_LimitDt))));
        itemStock.setValue(DAItemStock.col_ItemCount, mSelectedItemValue.get(DAItemStock.col_ItemCount));
        itemStock.setValue(DAItemStock.col_IsHidden, DAItemStock.val_HiddenOff);
        itemStock.upsertData();

        mSelectedItemValueBackup = new HashMap<>(mSelectedItemValue);

        // 編集画面を表示画面へ
        switchModeDisplayEditStock(false);
        //setItemDetailFooterVisibility(false);

        // リフレッシュ
        mParentActivity.refreshRelationInfo();

        // 通知
        CKUtil.showLongToast(CKUtil.getMyString(R.string.message_add_stock_list));
    }

    // 数量登録時のチェック
    private void checkRegistableItemStockCount() {

        String limitDate = mSelectedItemValue.get(DAItemStock.col_LimitDt);
        String registCount = mSelectedItemValue.get(DAItemStock.col_ItemCount);

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

        // 既存データを論理削除
        DAItemStock itemStock = CKDBUtil.getDAItemStock();
        itemStock.setValue(DAItemStock.col_IId, mSelectedItemValueBackup.get(DAItem.col_IId));
        itemStock.setLimitDate(mSelectedItemValueBackup.get(DAItemStock.col_LimitDt));
        itemStock.deleteDataLogical();

        // 画面で入力された消費期限で同一キーの判定
        Map<String, String> existData = CKDBUtil.getDAItemStock().getItemByKey(mSelectedItemValue.get(DAItemStock.col_IId), limitDate);
        if (existData == null) {
            // 同一キー未登録なら保存可能
            registToItemList();
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
                        .setTitle(CKUtil.getMyString(R.string.message_add_stock_limit_day) + mSelectedItemValue.get(DAItemStock.col_LimitDt))
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
                    registToItemList();
                    return;
                } else {

                    final String registCalcedCount = registCount;
                    String msg = CKUtil.getMyString(R.string.message_add_stock_limit_day) + mSelectedItemValue.get(DAItemStock.col_LimitDt) + "\n" +
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
                                            mSelectedItemValue.put(DAItemStock.col_ItemCount, registCalcedCount);
                                            // DB更新
                                            registToItemList();
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

    // 商品詳細フッターの表示状況
    public boolean isVisibleItemDetailFooter() {
        return mIsDetailShow;
    }

    // 商品詳細フッター表示制御
    public void setItemDetailFooterVisibility(boolean isShow) {

        if (mLlItemDetailFooter == null) {
            return;
        }

        // 設定値を保持
        mIsDetailShow = isShow;

        boolean isNowShow = (mLlItemDetailFooter.getVisibility() != View.GONE);
        if (isShow == isNowShow) { return; }

        // 詳細欄のストック編集欄を非表示に
        if (isShow) {
            switchModeDisplayEditStock(false);
        }

        // アニメーション定義
        Animation anim = AnimationUtils.loadAnimation(CKUtil.getMyContext(), isShow ? R.anim.slide_up : R.anim.slide_down);
        if (isShow) {
            anim.setDuration(0);
        }
        anim.setFillBefore(true);
        mLlItemDetailFooter.startAnimation(anim);
        mLlItemDetailFooter.setVisibility(isShow ? View.VISIBLE : View.GONE);

        // fabの表示制御
        mParentActivity.setFabVisibility(! isShow);
    }

    // データの更新まとめ
    public void updateDataNotify() {
        if (mView == null) { return; }

        // 一覧画面をリフレッシュ
        mIsDisplayLimitDate = CKDBUtil.getDASettings().isItemListDisplayLimitDate();
        setItemListView();
        mAdapter.notifyDataSetChanged();
        mAdapter.notifyDataSetInvalidated();

        // 設定欄の表示設定
        setSettingLayerValue();

        /*
        // タブ内の選択位置を戻す
        if (mAbsListView != null) {
            mAbsListView.setSelection(nowPosition);
        }
        */

        // 詳細画面をリフレッシュ
        setItemDetailStockInfo();
    }

    static class ViewHolder {
        View vwLimitMark;
        RelativeLayout rlImageContainer;
        ImageView ivImage;
        TextView tvItemCount;
        TextView tvLimitDate;
        TextView tvItemName;
        TextView tvRemainDaysLabel;
        TextView tvRemainDays;
        TextView tvDeleteIcon;
    }

    // 商品情報を表示するためのAdapterを定義
    private class ItemListAdapter extends ArrayAdapter<Map<String, String>> {

        private DAItemStock mListItemStock;
        private LayoutInflater inflater;
        private int inflate_item_id;

        ItemListAdapter(Context context, int id, ArrayList<Map<String, String>> item) {
            super(context, id, item);

            inflate_item_id = id;
            inflater = LayoutInflater.from(context);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            // View生成によるメモリ消費を抑えるためにHolderクラスへViewを保持
            ViewHolder holder;
            if (convertView == null) {
                mListItemStock = CKDBUtil.getDAItemStock();

                convertView = inflater.inflate(inflate_item_id, parent, false);
                holder = new ViewHolder();
                holder.vwLimitMark = convertView.findViewById(R.id.row_item_info_limit_mark);
                holder.rlImageContainer = (RelativeLayout) convertView.findViewById(R.id.grid_item_info_container);
                ViewGroup.LayoutParams lp = holder.rlImageContainer.getLayoutParams();
                lp.height = mImageHeight;
                holder.rlImageContainer.setLayoutParams(lp);
                holder.ivImage = (ImageView) convertView.findViewById(R.id.row_item_info_image);
                // 画像タップ時のアニメーション
                //holder.ivImage.setOnTouchListener(CKUtil.doOnTouchAnimation());
                holder.tvItemCount = (TextView) convertView.findViewById(R.id.row_item_info_item_count);
                holder.tvLimitDate = (TextView) convertView.findViewById(R.id.row_item_info_limit_date);
                holder.tvItemName = (TextView) convertView.findViewById(R.id.row_item_info_item_name);
                holder.tvDeleteIcon = (TextView) convertView.findViewById(R.id.row_item_info_delete_select);
                // 削除選択アイコン(非表示)
                holder.tvDeleteIcon.setVisibility(View.GONE);
                // 商品名(非表示)
                holder.tvItemName.setVisibility(View.GONE);
                holder.tvRemainDaysLabel = (TextView) convertView.findViewById(R.id.row_item_info_remain_days_label);
                holder.tvRemainDays = (TextView) convertView.findViewById(R.id.row_item_info_remain_days);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            // 期限に日付を表示するか？
            holder.tvLimitDate.setVisibility(mIsDisplayLimitDate ? View.VISIBLE : View.GONE);

            // 品目ストック
            mListItemStock.setAllValue(getItem(position));

            // 画像等のコンテナ
            /*
            ViewGroup.LayoutParams lp = holder.rlImageContainer.getLayoutParams();
            lp.height = mImageHeight;
            holder.rlImageContainer.setLayoutParams(lp);
            // 画像タップ時のアニメーション
            holder.ivImage.setOnTouchListener(CKUtil.doOnTouchAnimation());
            */
            // 画像の表示
            //ImageTask task = new ImageTask(holder.ivImage);
            //task.execute(itemStock.getValue(DAItem.col_ImagePath));
            holder.ivImage.setImageBitmap(CKUtil.decodeSampledBitmapFromFile(mListItemStock.getValue(DAItem.col_ImagePath), mImageWidth, mImageHeight));

            // 数量
            String itemCount = mListItemStock.getValue(DAItemStock.col_ItemCount);
            holder.tvItemCount.setText(itemCount.equals("") ? "0" : itemCount);
            /*
            // 期限(非表示)
            holder.tvLimitDate.setVisibility(View.GONE);
            // 商品名(非表示)
            holder.tvItemName.setVisibility(View.GONE);
            */
            // 期限と残日数計算
            int rid = CKUtil.calcRemainDays(mListItemStock.getLimitDate(), holder.tvLimitDate, holder.tvRemainDaysLabel, holder.tvRemainDays);
            if (rid != -1) {
                holder.vwLimitMark.setBackgroundResource(rid);
            }

            return convertView;

        }
    }

    // 画像表示用の非同期処理
    class ImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView mIvImage;
        public ImageTask(ImageView view) {
            mIvImage = view;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return CKUtil.decodeSampledBitmapFromFile(params[0], mImageWidth, mImageHeight);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mIvImage.setImageBitmap(bitmap);
        }
    }

    // 商品詳細のストック情報を設定
    private void setItemDetailStockInfo() {

        if (! mIsDetailShow) { return; }

        // リスト表示値を取得
        DAItemStock daItemStock = CKDBUtil.getDAItemStock();
        Map<String, String> stock = daItemStock.getItemByKey(mSelectedItemValue.get(DAItem.col_IId), mSelectedItemValue.get(DAItemStock.col_LimitDt));
        if (stock == null) {
            return;
        }

        // ストック表示欄
        // 数量
        mTvItemCount.setText(mSelectedItemValue.get(DAItemStock.col_ItemCount));
        if (mTvItemCount.getText().toString().equals("")) {
            mTvItemCount.setText("0");
        }

        // 期限と残日数計算
        int rid = CKUtil.calcRemainDays(mSelectedItemValue.get(DAItemStock.col_LimitDt), mTvLimitDate, mTvRemainDaysLabel, mTvRemainDays);
        if (rid != -1) {
            // 期限警告マーク
            mVwLimitMark.setBackgroundResource(rid);
            mTvRemainDaysLabel.setText("");
            mTvRemainDaysLabel.setVisibility(rid == R.color.colorExclamation ? View.VISIBLE : View.INVISIBLE);
        }

        // ストック情報(編集)の設定
        setEditStockLayerInfo();

        return;
    }

    // ストック情報(編集)の設定
    private void setEditStockLayerInfo() {

        // 日付・数量
        if (mDtLimitDate == null) {
            mDtLimitDate = (MyDatePicker) mView.findViewById(R.id.popup_edit_item_stock_limit_date);
            mNpCount10 = (NumberPicker) mView.findViewById(R.id.popup_edit_item_stock_count_10);
            mNpCount01 = (NumberPicker) mView.findViewById(R.id.popup_edit_item_stock_count_1);
            //mNpCount10 = (MyNumberPicker) mView.findViewById(R.id.popup_edit_item_stock_count_10);
            //mNpCount01 = (MyNumberPicker) mView.findViewById(R.id.popup_edit_item_stock_count_1);
        }

        // 入力制御
        mNpCount10.setMinValue(0);
        mNpCount10.setMaxValue(9);
        mNpCount01.setMinValue(0);
        mNpCount01.setMaxValue(9);

        // dividerの色変更
        CKUtil.setNumberPickerDividerNone(mNpCount10);
        CKUtil.setNumberPickerDividerNone(mNpCount01);

        // 消費期限
        Calendar calendarToday = Calendar.getInstance();
        String limitDate = mSelectedItemValue.get(DAItemStock.col_LimitDt);
        limitDate = CKUtil.getFormatDate(CKUtil.isSystemFormatDate(limitDate));

        if (limitDate == null || limitDate.equals("")) {
            limitDate = CKUtil.getCurrentDate();
            mSelectedItemValue.put(DAItemStock.col_LimitDt, limitDate);
        }
        calendarToday.setTime(CKUtil.isDate(limitDate));
        int year = calendarToday.get(Calendar.YEAR);
        int month = calendarToday.get(Calendar.MONTH);
        int day = calendarToday.get(Calendar.DAY_OF_MONTH);
        mDtLimitDate.init(year, month, day, new MyDatePicker.OnDateChangedListener(){
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String newDt = CKUtil.getFormatDate(year, monthOfYear + 1, dayOfMonth);
                // 変更値を保存
                mSelectedItemValue.put(DAItemStock.col_LimitDt, CKUtil.getSystemFormatDate(CKUtil.isDate(newDt)));
            }
        });

        // 数量
        String itemCountBuff = mSelectedItemValue.get(DAItemStock.col_ItemCount);
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

        mNpCount10.setValue(count10);
        mNpCount01.setValue(count01);

        // 数量Picker変更
        mNpCount10.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setItemStockCount();
            }
        });
        mNpCount01.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (oldVal == mNpCount01.getMaxValue() && newVal == mNpCount01.getMinValue()) {
                    int val10 = mNpCount10.getValue();
                    if (val10 < mNpCount10.getMaxValue()) {
                        val10++;
                        mNpCount10.setValue(val10);
                    }
                } else if (oldVal == mNpCount01.getMinValue() && newVal == mNpCount01.getMaxValue()) {
                    int val10 = mNpCount10.getValue();
                    if (val10 > mNpCount10.getMinValue()) {
                        val10--;
                        mNpCount10.setValue(val10);
                    }
                }

                setItemStockCount();
            }
        });

        // ストック編集欄
        // キャンセル
        mBtEditStockCancel.setText(CKUtil.getMyString(R.string.regist_cancel_button));
        mBtEditStockCancel.setTextSize(16);
        mBtEditStockCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 値を復元
                mSelectedItemValue = new HashMap<>(mSelectedItemValueBackup);
                // 編集画面を閉じる
                switchModeDisplayEditStock(false);
            }
        });
        // DB更新
        mBtEditStockRegist.setText(CKUtil.getMyString(R.string.regist_update_button));
        mBtEditStockRegist.setTextSize(16);
        mBtEditStockRegist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 値変更の有無を確認
                if (mSelectedItemValue.get(DAItemStock.col_LimitDt).equals(mSelectedItemValueBackup.get(DAItemStock.col_LimitDt))
                        && mSelectedItemValue.get(DAItemStock.col_ItemCount).equals(mSelectedItemValueBackup.get(DAItemStock.col_ItemCount))) {

                    // 詳細画面を閉じる
                    switchModeDisplayEditStock(false);

                    CKUtil.showLongToast(CKUtil.getMyString(R.string.message_add_stock_list));
                } else {
                    // データチェックとDB更新
                    checkRegistableItemStockCount();
                    // 画面更新
                    mParentActivity.refreshRelationInfo();
                }
            }
        });
    }

    private void setItemStockCount() {
        mSelectedItemValue.put(DAItemStock.col_ItemCount, String.valueOf(mNpCount10.getValue() * 10 + mNpCount01.getValue()));
    }

    // 期限・数量ダイアログからの戻り値制御
    @Override
    public void onDialogEditStockButtonRegistClick(String limitDate) {
        // 更新された消費期限を設定
        mSelectedItemValue.put(DAItemStock.col_LimitDt, limitDate);
        // 全体リフレッシュ
        mParentActivity.refreshRelationInfo();
    };

    @Override
    public void onDialogEditStockButtonCancelClick() { };

    /*
    // グラフ表示
    private void setupGraphView(String iid) {

        if (iid.equals("")) { return; }

        // グラフ
        HorizontalBarChart barChart = (HorizontalBarChart) mView.findViewById(R.id.fragment_item_list_graph);
        barChart.setNoDataText("データは\nありません");
        barChart.setDescription("");

        // イベントを停止
        barChart.setTouchEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);

        // 描画
        barChart.setEnabled(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setInverted(true);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawBorders(true);
        barChart.setDrawMarkerViews(true);
        barChart.setDrawValueAboveBar(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setXOffset(30f);
        barChart.setBorderColor(CKUtil.getMyColor(R.color.colorTabUnder));
        barChart.getXAxis().setAxisLineColor(CKUtil.getMyColor(R.color.colorTabUnder));
        barChart.getXAxis().setGridColor(CKUtil.getMyColor(R.color.colorTabUnder));

        Legend legend = barChart.getLegend();
        legend.setEnabled(false);

        // 種類の数
        String itemTypeList[];
        Integer graphValue[];
        String itemTypeValue[];

        // データ取得
        DAItemStock daItemStock = CKDBUtil.getDAItemStock();
        ArrayList<Map<String, String>> graphData = daItemStock.getGraphDataByLimit(iid);
        // 総数を取得
        int allItemCount = 0;
        int maxItemCountGroupByItemType = 0;
        for (Map<String, String> data : graphData) {
            int summaryData = Integer.valueOf((String) CKUtil.nullTo(data.get(DAItemStock.col_PieChart_SummaryData), "0"));
            allItemCount = allItemCount +  summaryData;
            if (summaryData > maxItemCountGroupByItemType) {
                maxItemCountGroupByItemType = summaryData;
            }
        }
        // 在庫なし
        if (allItemCount == 0) {
            barChart.clear();
            return;
        }

        // 分類区分(横軸)
        itemTypeList = new String [] {
                getString(R.string.limit_day_over),
                getString(R.string.limit_day_in1month),
                getString(R.string.limit_day_in1year),
                getString(R.string.limit_day_other)
        };
        itemTypeValue = new String[itemTypeList.length];
        graphValue = new Integer[itemTypeList.length];

        List<Integer> graphColors = new ArrayList<>();

        // 種類別の比率を取得
        for (Map<String, String> data : graphData) {
            for (int i = 0; i < itemTypeList.length; i++) {
                String summaryTitle = data.get(DAItemStock.col_PieChart_SummaryTitle);
                if (String.valueOf(i).equals(summaryTitle)) {

                    String summaryData = (String) CKUtil.nullTo(data.get(DAItemStock.col_PieChart_SummaryData), "0");
                    graphValue[i] = Integer.valueOf(summaryData);
                    itemTypeValue[i] = String.format("%,d", Integer.valueOf(summaryData));

                    // 色指定(グラフ)
                    switch (summaryTitle) {
                        case "0":
                            graphColors.add(CKUtil.getMyColor(R.color.colorExclamation));
                            break;
                        case "1":
                            graphColors.add(CKUtil.getMyColor(R.color.colorAlert));
                            break;
                        case "2":
                            graphColors.add(CKUtil.getMyColor(R.color.colorInfo));
                            break;
                        case "3":
                            graphColors.add(CKUtil.getMyColor(R.color.colorNoInformation));
                            break;
                    }
                }
            }
        }

        // 縦軸の範囲
        barChart.getAxisRight().setAxisMinValue(0);
        barChart.getAxisLeft().setAxisMaxValue(maxItemCountGroupByItemType);

        // グラフに表示するデータ
        ArrayList<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < itemTypeValue.length; i++) {
            if (itemTypeValue[i] != null && ! itemTypeValue[i].equals("")) {
                entries.add(new BarEntry(graphValue[i], i));
            }
        }

        // グラフ表示
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(graphColors);
        dataSet.setBarSpacePercent(1f);
        dataSet.setValueTextSize(18f);
        dataSet.setValueTextColor(CKUtil.getMyColor(R.color.colorGridDetailItemHeader));
        dataSet.setValueFormatter(new LargeValueFormatter());
        barChart.setData(new BarData(itemTypeList, dataSet));

        // 描画更新を通知してリフレッシュ
        barChart.notifyDataSetChanged();
        barChart.invalidate();
    }
    */
}

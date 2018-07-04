package com.cloudkura.light;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class DialogUseList extends DialogFragment
{
    //引数名定義
    public final static String ARG_SEARCH_IID = "searchIId";
    public final static String ARG_SEARCH_LIMITDATE = "limitDate";
    public final static String ARG_SEARCH_CONDITION = "searchCondition";
    public final static String ARG_SEARCH_WORD = "searchWord";
    public final static String ARG_SEARCH_BARCODE = "searchbarcode";

    // 編集モード選択値
    private enum EditMode{
        NORMAL,
        DELETE_SELECT
    }

    // 親Activity
    AppCompatActivity mParentActivity;

    // 呼出し元
    static DialogItemSearch mSourceFragment;

    // 条件文言
    String mCondition = "";
    // 商品リストの詳細画面からの削除
    boolean mIsDeleteItemList = false;

    // 画面部品
    Fragment fragment;
    Dialog dialog;
    View mView;
    AbsListView mAbsListView;
    ItemListAdapter mAdapter;
    TextView mTxTitle;
    TextView mTxMessage;
    Button mBtDeleteSelectedItem;
    Button mBtCancelSelectedItem;
    Button mBtClose;

    String mDeleteSelectTopItemBarcode;
    boolean mIsOneTypeItem = true;
    ArrayList<String> mDeleteItemIId;

    // 商品名欄
    LinearLayout mLlItemNameLayer;
    EditText mEtItemNameInput;
    TextView mTvTextCount;
    TextView mTvTextCountMax;
    TextWatcher mTextWatcher;
    ImageButton mIbKeyboard;
    ListView mLvItemNameWords;
    ArrayAdapter<String> mItemNameWordsAdapter;
    ImageButton mIbSearch;
    // 類語検索用
    String mPreInputWord = "";
    LinearLayout mLlItemSelectStatus;

    // モード
    EditMode mEditMode = EditMode.NORMAL;

    int nowPosition;
    String mActiveIId = "";
    ObjectSearchCondition mSearchCondition = new ObjectSearchCondition();
    ArrayList<Map<String, String>> mListItems;

    // カードアニメーションの選択値
    private enum CardAnim{
        UPDATE,
        DELETE
    }

    // 画像の大きさを保持(grid_item_info.xmlの設定値と合わせる)
    int mImageWidth = 120;
    int mImageHeight = 120;

    public DialogUseList() { }

    public static DialogUseList newInstance() {
        return new DialogUseList();
    }

    public static DialogUseList newInstance(DialogItemSearch dialogItemSearch) {
        mSourceFragment = dialogItemSearch;
        return new DialogUseList();
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

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 自分を保存
        fragment = this;

        // xmlファイルとの紐付け
        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        mView = layoutInflater.inflate(R.layout.fragment_use_list, null, false);

        int alertSpan = 0;

        CKUtil.SelectLimitDay selectLimitDayCondition = null;
        String searchIId = "";
        String searchLimitDate = "";
        String searchCondition = "";
        String searchWord = "";
        String searchBarcode = "";

        //引数取得
        Bundle arg = getArguments();
        if (arg==null) {
            // 商品名
            callEventRowMemo(true);
        } else {
            searchIId = arg.getString(ARG_SEARCH_IID);
            searchIId = (searchIId == null ? "" : searchIId);
            searchLimitDate = arg.getString(ARG_SEARCH_LIMITDATE);
            searchLimitDate = (searchLimitDate == null ? "" : searchLimitDate);

            // 商品名(引数未指定の場合は非表示)
            callEventRowMemo(false);

            searchCondition = arg.getString(ARG_SEARCH_CONDITION);
            searchCondition = (searchCondition == null ? "" : searchCondition);
            searchWord = arg.getString(ARG_SEARCH_WORD);
            searchWord = (searchWord == null ? "" : searchWord);
            searchBarcode = arg.getString(ARG_SEARCH_BARCODE);
            searchBarcode = (searchBarcode == null ? "" : searchBarcode);
        }

        if (searchCondition.equals(CKUtil.getMyString(R.string.limit_day_over_detail))) {
            selectLimitDayCondition = CKUtil.SelectLimitDay.OVER;
        } else if (searchCondition.equals(CKUtil.getMyString(R.string.limit_day_in1month_detail))) {
            selectLimitDayCondition = CKUtil.SelectLimitDay.IN_1MONTH;
        } else if (searchCondition.equals(CKUtil.getMyString(R.string.limit_day_in1year_detail))) {
            selectLimitDayCondition = CKUtil.SelectLimitDay.IN_1YEAR;
        } else if (searchCondition.equals(CKUtil.getMyString(R.string.limit_day_other_detail))) {
            selectLimitDayCondition = CKUtil.SelectLimitDay.OTHER;
        } else if (searchCondition.equals(CKUtil.getMyString(R.string.limit_day_alert))) {
            selectLimitDayCondition = CKUtil.SelectLimitDay.ALERT_SPAN;
            // 期間判定
            DASettings daSettings = CKDBUtil.getDASettings();
            alertSpan = daSettings.getAlertSpan();
        }
        mSearchCondition.setSearchIId(searchIId);
        mSearchCondition.setmSearchLimitDate(searchLimitDate);
        mSearchCondition.setSelectedLimitDay(selectLimitDayCondition, alertSpan);
        mSearchCondition.setSearchWord(searchWord);
        mSearchCondition.setBarcode(searchBarcode);

        //データ無し時に表示するView
        final TextView tvEmptyView = (TextView) mView.findViewById(R.id.fragment_item_empty_view);
        //リストの準備
        mAbsListView = (GridView) mView.findViewById(R.id.fragment_use_list_grid_view);
        mAbsListView.setEmptyView(tvEmptyView);

        // オブジェクト準備
        mTxTitle = (TextView) mView.findViewById(R.id.fragment_use_list_title);
        mCondition = "";
        mIsDeleteItemList = false;
        // 商品リストからの削除かを判定(Adapterで名称を表示するか制御)
        mIsDeleteItemList = !searchIId.equals("");
        if (mIsDeleteItemList) {
            mCondition = searchWord;
        } else {
            if (!searchWord.equals("")) {
                mCondition = mCondition + getString(R.string.message_in_item_name) + searchWord + getString(R.string.message_contain_in_item_name);
            }
            if (!searchBarcode.equals("")) {
                mCondition = mCondition + getString(R.string.message_title) + searchBarcode + " ";
            }
            if (searchCondition.equals("")) {
                if (mCondition.equals("")) {
                    mCondition = getString(R.string.message_all_item);
                }
            } else {
                mCondition = searchCondition;
            }
        }

        mTxTitle.setText(R.string.message_explain_tap_to_select);
        mTxMessage = (TextView) mView.findViewById(R.id.fragment_use_list_message);
        mTxMessage.setText(mCondition);

        writeLog(mCondition);

        mBtDeleteSelectedItem = (Button) mView.findViewById(R.id.fragment_use_list_del_selected_item);
        mBtDeleteSelectedItem.setOnTouchListener(CKUtil.doOnTouchAnimation());
        mBtCancelSelectedItem = (Button) mView.findViewById(R.id.fragment_use_list_cancel_selected_item);
        mBtCancelSelectedItem.setOnTouchListener(CKUtil.doOnTouchAnimation());

        //ListViewへの一覧設定
        setItemListView();

        // 閉じるボタン
        mBtClose = (Button) mView.findViewById(R.id.fragment_use_list_button_close);
        mBtClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 画面を閉じる
                closeDialogUseList();
            }
        });

        // ダイアログ情報を取得
        // 画面いっぱいに表示
        dialog = new Dialog(getActivity(), R.style.NoDimDialogFragmentStyle);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(mView);
        //dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        //dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationX;

        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationFade;

        // キーボード非表示
        CKUtil.hideKeyboad(mView);

        if (mSourceFragment != null) {
            mSourceFragment.dismissThisFragment();
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshItemList(mSearchCondition);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mParentActivity == null) {
            mParentActivity = (AppCompatActivity) context;
        }
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mParentActivity == null) {
            mParentActivity = (AppCompatActivity) activity;
        }
    }

    @Override
    public void onDetach() { super.onDetach(); }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.gc();
    }

    //ListViewへの一覧設定
    public void setItemListView() {

        // 表示データを抽出
        DAItemStock daItemStock = CKDBUtil.getDAItemStock();
        ArrayList<Map<String, String>> stocks = daItemStock.getEachItemDateCountBySearchCondition(mSearchCondition
                                                                    , DAItemStock.col_LimitDt + " asc ");

        mListItems = new ArrayList<>();

        // 数量分行数を膨らませる(数量=0は含めない)
        for (Map<String, String> stock : stocks) {
            String val = stock.get(DAItemStock.col_ItemCount);
            int itemCount = Integer.valueOf( (val == null || val.equals("")) ? "0" : val );
            for (int count = 0; count < itemCount; count++) {
                // new しないと同じ参照先がaddされるため、
                // 1要素の変更で同じ参照先の内容がすべて変わってしまう。
                mListItems.add(new HashMap<>(stock));
            }
        }

        //一覧へ表示
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }
        if (mAbsListView != null) {
            mAbsListView.setAdapter(null);
        }
        mAdapter = new ItemListAdapter(CKUtil.getMyContext(), R.layout.grid_item_info, mListItems);
        mAbsListView.setAdapter(mAdapter);

        // ヘッダー表示
        setCountToTitle(0);

        // フッターの表示制御(標準モード)
        changeFooterMenu(EditMode.NORMAL);

        // カードタップ時のイベント
        mAbsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 選択されたカードの位置を保存
                nowPosition = position;
                // 削除対象を選択する。
                setDeleteCardFlag();
            }
        });

        //スクロール時のイベント
        mAbsListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            boolean isBottom = false;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) { }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                isBottom = (totalItemCount > 0 && totalItemCount == firstVisibleItem + visibleItemCount);
            }
        });

        // 選択削除ボタン
        mBtDeleteSelectedItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLog();

                mDeleteSelectTopItemBarcode = "";
                mIsOneTypeItem = true;

                mDeleteItemIId = null;
                mDeleteItemIId = new ArrayList<>();

                // 選択されたカードを削除する
                int selected = 0;
                int delcount = 0;
                DAItem item = CKDBUtil.getDAItem();
                for (int i = 0; i < mAbsListView.getCount(); i++) {
                    item.setAllValue((Map<String, String>) mAbsListView.getItemAtPosition(i));

                    if (item.getValue(DAItem.col_IsHidden).equals(DAItem.val_HiddenOn)) {
                        // 選択されたバーコード情報を保存
                        selected++;

                        if (! mDeleteItemIId.contains(item.getValue(DAItem.col_IId))) {
                            mDeleteItemIId.add(delcount, item.getValue(DAItem.col_IId));
                            delcount++;
                        }

                        if (mDeleteSelectTopItemBarcode.equals("")) {
                            mDeleteSelectTopItemBarcode = item.getValue(DAItem.col_IId);
                        } else {
                            if (mIsOneTypeItem && ! mDeleteSelectTopItemBarcode.equals(item.getValue(DAItem.col_IId))) {
                                mIsOneTypeItem = false;
                            }
                        }
                    }
                }

                if (selected == 0) {
                    CKUtil.showLongToast(getString(R.string.message_please_select));
                    return;
                }

                // 削除確認ダイアログ表示ボタン
                new AlertDialog.Builder(view.getContext(), R.style.MyAlertDialogStyle)
                        .setCancelable(false)
                        .setTitle(R.string.alert_dialog_title_confirm)
                        .setMessage(R.string.message_confirm_delete)
                        .setPositiveButton(
                        R.string.alert_dialog_button_yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whitch) {

                                // 選択されたカードを削除する
                                DAItem daItem = CKDBUtil.getDAItem();
                                for (int i = 0; i < mAbsListView.getCount(); i++) {
                                    daItem.setAllValue((Map<String, String>) mAbsListView.getItemAtPosition(i));
                                    if (daItem.getValue(DAItem.col_IsHidden).equals(DAItem.val_HiddenOn)) {
                                        if (!daItem.getValue(DAItemStock.col_IId).equals("") && !daItem.getValue(DAItemStock.col_LimitDt).equals("")) {

                                            // 商品の数量を1つ減算
                                            DAItemStock delStock = CKDBUtil.getDAItemStock();
                                            Map<String, String> stock = delStock.getItemByKey(daItem.getValue(DAItemStock.col_IId), daItem.getValue(DAItemStock.col_LimitDt));
                                            delStock.setAllValue(stock);
                                            int count = Integer.valueOf(stock.get(DAItemStock.col_ItemCount)) - 1;

                                            // 数量0になったらレコード自体を非表示(ItemStock)
                                            /*
                                            if (count == 0) {
                                                delStock.setValue(DAItemStock.col_IsHidden, DAItemStock.val_HiddenOn);
                                            }
                                            */
                                            delStock.setValue(DAItemStock.col_ItemCount, String.valueOf(count));
                                            delStock.upsertData();
                                        }
                                    }

                                    /*
                                    // ver2.0.1～ 数量0をレコードとして残して非表示。Web同期対応
                                    // 商品の非表示フラグを立てる
                                    DAItemStock checkStock = CKDBUtil.getDAItemStock();
                                    ArrayList<Map<String, String>> stock = checkStock.getItemByIId(daItem.getValue(DAItem.col_IId));
                                    if (stock == null || stock.size() == 0 || stock.get(0).get(DAItemStock.col_ItemCount).equals("0")) {
                                        //daItem.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOn);
                                        daItem.deleteDataLogical();
                                    }
                                    */
                                    /* 削除前
                                    // Stock明細のデータがなくなったらItem自体を削除
                                    DAItemStock checkStock = CKDBUtil.getDAItemStock();
                                    ArrayList<Map<String, String>> stock = checkStock.getItemByIId(daItem.getValue(DAItem.col_IId));
                                    if (stock == null || stock.size() == 0 || stock.get(0).get(DAItemStock.col_ItemCount).equals("0")) {
                                        daItem.deleteData();
                                        // 画像ファイルを削除
                                        CKUtil.deletePictureFile(daItem.getValue(DAItem.col_ImagePath));
                                    }
                                    */
                                }

                                // フッターボタンの制御(標準モード)
                                changeFooterMenu(EditMode.NORMAL);
                                // リストリフレッシュ
                                refreshItemList(mSearchCondition);
                                // タイトルバーをクリア
                                setCountToTitle(0);

                                // 全体リフレッシュ
                                if (mParentActivity.getClass().equals(ActivityMain.class)) {
                                    ActivityMain activityMain = (ActivityMain) mParentActivity;
                                    activityMain.refreshRelationInfo();
                                }

                                // 削除通知画面を表示
                                String dialogTag = "notice_deleted";
                                if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                                    Bundle arg = new Bundle();
                                    arg.putString(DialogDeleteAffiliate.ARG_ITEM_ID, mIsOneTypeItem ? mDeleteSelectTopItemBarcode :  "");
                                    arg.putStringArrayList(DialogDeleteAffiliate.ARG_DELETE_ITEMS_IID, mDeleteItemIId);
                                    arg.putSerializable(DialogDeleteAffiliate.ARG_DELETE_TYPE, DialogDeleteNotice.enmDeleteType.deleteEachItem);
                                    arg.putBoolean(DialogDeleteAffiliate.ARG_FROM_NO_STOCK_LIST, false);

                                    DialogDeleteNotice dialogDeleteNotice = new DialogDeleteNotice();
                                    dialogDeleteNotice.setCancelable(false);
                                    dialogDeleteNotice.setArguments(arg);
                                    dialogDeleteNotice.show(getFragmentManager(), dialogTag);
                                }

                                // 画面を閉じる
                                //closeDialogUseList();
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
            }
        });

        // 削除モードでのキャンセルボタン
        mBtCancelSelectedItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLog();
                // フッターボタンの制御(標準モード)
                changeFooterMenu(EditMode.NORMAL);
                // 選択数量=0
                setCountToTitle(0);
            }
        });
    }

    // 選択カードの削除On/Off切り替え
    private void setDeleteCardFlag() {

        // 削除選択された場合のView表示
        DAItem item = CKDBUtil.getDAItem();
        item.setAllValue((Map<String, String>) mAbsListView.getItemAtPosition(nowPosition));

        View currentView = mAbsListView.getChildAt(nowPosition - mAbsListView.getFirstVisiblePosition());
        TextView tv = (TextView) currentView.findViewById(R.id.row_item_info_delete_select);

        if (item.getValue(DAItem.col_IsHidden).equals(DAItem.val_HiddenOn)) {
            tv.setVisibility(View.GONE);
            item.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOff);
        } else {
            Animation anim = AnimationUtils.loadAnimation(CKUtil.getMyContext(), R.anim.slide_up);
            anim.setFillBefore(true);

            tv.setVisibility(View.VISIBLE);
            tv.startAnimation(anim);
            item.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOn);
        }

        mListItems.set(nowPosition, item.getAllValue());

        // メニュー：選択されたカード数を更新
        int selectedCount = 0;
        for (int i = 0; i < mListItems.size(); i++) {
            if (mListItems.get(i).get(DAItem.col_IsHidden).equals(DAItem.val_HiddenOn)) {
                // 選択数
                selectedCount++;
            }
        }
        setCountToTitle(selectedCount);

    }

    // メニュータイトルを変更
    private void setCountToTitle(int selectedCount) {

        if (mTxMessage == null) {
            return;
        }

        if (selectedCount==0) {
            mTxMessage.setText(mCondition);
        } else {
            String text = getString(R.string.message_select_count) + String.valueOf(selectedCount);
            mTxMessage.setText(text);
        }
    }

    // 選択カードの削除Off切り替え(アニメーションなし)
    private void setDeleteCardFlagOff(int position) {

        // 削除選択された場合のView表示
        DAItem item = CKDBUtil.getDAItem();
        item.setAllValue((Map<String, String>) mAbsListView.getItemAtPosition(position));

        if (item.getValue(DAItem.col_IsHidden).equals(DAItem.val_HiddenOn)) {

            View currentView = mAbsListView.getChildAt(position - mAbsListView.getFirstVisiblePosition());
            if (currentView != null) {
                TextView tv = (TextView) currentView.findViewById(R.id.row_item_info_delete_select);
                tv.setVisibility(View.GONE);
            }
            item.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOff);
            mListItems.set(position, item.getAllValue());
        }

    }
    // リストからのカード削除
    private void deleteCard(int deletePosition) {

        if (mAbsListView == null) {return;}

        nowPosition = deletePosition;

        // リストからのカード削除
        View currentView = mAbsListView.getChildAt(nowPosition - mAbsListView.getFirstVisiblePosition());
        if (currentView != null) {

            Animation anim = changeCardBackground(CardAnim.DELETE);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    mAbsListView.post(new Runnable() {
                        @Override
                        public void run() {
                            // リストから削除
                            if (nowPosition < mAbsListView.getCount()) {
                                ArrayAdapter<Map<String, String>> adapter = (ArrayAdapter<Map<String, String>>) mAbsListView.getAdapter();
                                adapter.remove((Map<String, String>) mAbsListView.getItemAtPosition(nowPosition));
                            }
                        }
                    });
                    // アニメーション処理解放
                    mAbsListView.setAnimation(null);
                    mAbsListView.invalidate();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            currentView.startAnimation(anim);
        }
    }

    static class ViewHolder {
        View vwLimitMark;
        ImageView ivImage;
        LinearLayout llItemCountLayer;
        TextView tvLimitDate;
        TextView tvItemName;
        TextView tvRemainDaysLabel;
        TextView tvRemainDays;
        TextView tvSelected;
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
                holder.ivImage = (ImageView) convertView.findViewById(R.id.row_item_info_image);
                holder.llItemCountLayer = (LinearLayout) convertView.findViewById(R.id.row_item_info_item_count_layer);
                // 数量(非表示)
                holder.llItemCountLayer.setVisibility(View.GONE);
                holder.tvLimitDate = (TextView) convertView.findViewById(R.id.row_item_info_limit_date);
                holder.tvItemName = (TextView) convertView.findViewById(R.id.row_item_info_item_name);
                // 期限
                holder.tvLimitDate.setVisibility(View.VISIBLE);
                // 商品名
                holder.tvItemName.setVisibility(mIsDeleteItemList ? View.GONE : View.VISIBLE);
                holder.tvRemainDaysLabel = (TextView) convertView.findViewById(R.id.row_item_info_remain_days_label);
                holder.tvRemainDays = (TextView) convertView.findViewById(R.id.row_item_info_remain_days);
                holder.tvSelected = (TextView) convertView.findViewById(R.id.row_item_info_delete_select);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            mListItemStock.setAllValue(getItem(position));

            // 画像の表示
            //ImageTask task = new ImageTask(holder.ivImage);
            //task.execute(itemStock.getValue(DAItem.col_ImagePath));
            holder.ivImage.setImageBitmap(CKUtil.decodeSampledBitmapFromFile(mListItemStock.getValue(DAItem.col_ImagePath), mImageWidth, mImageHeight));
            String itemName = mListItemStock.getValue(DAItem.col_Explain);
            holder.tvItemName.setText(itemName.equals("") ? getString(R.string.message_not_regist_item_name) : itemName);

            // 期限と残日数計算
            int rid = CKUtil.calcRemainDays(mListItemStock.getValue(DAItemStock.col_LimitDt), holder.tvLimitDate, holder.tvRemainDaysLabel, holder.tvRemainDays);
            if (rid != -1) {
                holder.vwLimitMark.setBackgroundResource(rid);
            }

            // 削除選択された場合のView表示
            if (mListItemStock.getValue(DAItem.col_IsHidden).equals(DAItem.val_HiddenOn)) {
                holder.tvSelected.setVisibility(View.VISIBLE);
            } else {
                holder.tvSelected.setVisibility(View.GONE);
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

    // カード背景色の一時変更
    private Animation changeCardBackground(CardAnim cardAnim) {

        // 初期化
        mActiveIId = "";

        Animation anim;
        if (cardAnim == CardAnim.DELETE) {
            anim = AnimationUtils.loadAnimation(CKUtil.getMyContext(), R.anim.card_delete);
        } else {
            anim = AnimationUtils.loadAnimation(CKUtil.getMyContext(), R.anim.card_update);
        }
        anim.setDuration(300);
        anim.setFillBefore(true);
        return anim;
    }

    // フッターの表示変更、各ボタンの表示制御など
    private void changeFooterMenu(EditMode mode) {

        // 削除モードのフッター
        LinearLayout llFooterSelecteItem = (LinearLayout) mView.findViewById(R.id.fragment_use_list_footer_delete);

        // DBにデータがなければ標準モードに強制変更
        // DAItemのレコード数で判断
        long databaseDataCount = CKDBUtil.getDAItem().getRowCount("", new String[0]);
        if (databaseDataCount == 0) {
            // ボタン非表示
            llFooterSelecteItem.setVisibility(View.GONE);
            return;
        }
        if (mAbsListView.getCount() == 0) {
            llFooterSelecteItem.setVisibility(View.GONE);
            return;
        }

        llFooterSelecteItem.setVisibility(View.VISIBLE);
        mEditMode = mode;

        // モード判定
        if (mEditMode.equals(EditMode.NORMAL)) {

            // カードをすべて未選択に戻す
            for (int i = 0; i < mAbsListView.getCount(); i++) {
                setDeleteCardFlagOff(i);
            }
        } else {
            setCountToTitle(0);
        }

    }

    // 商品リストリフレッシュ処理
    public void refreshItemList(ObjectSearchCondition searchCondition) {

        if (mView == null) { return; }

        // 検索・ソート条件を保存
        mSearchCondition = searchCondition;

        // リストを再描画
        setItemListView();
        mAdapter.notifyDataSetChanged();
        mAdapter.notifyDataSetInvalidated();
    }

    // 画面を閉じる
    public void closeDialogUseList() {

        if (mParentActivity.getClass().equals(ActivityMain.class)) {
            // リスト・グラフを再描画
            ((ActivityMain) mParentActivity).refreshRelationInfo();

        } else if (mParentActivity.getClass().equals(ActivityNotificationResult.class)) {
            // Activityを閉じる
            ((ActivityNotificationResult) mParentActivity).onDissMissReturn();
        }

        dismiss();
    }

    // 商品名欄関連のイベント定義
    private void callEventRowMemo(boolean isShow) {

        // レイヤー
        mLlItemNameLayer = (LinearLayout) mView.findViewById(R.id.fragment_item_search_item_name);
        mLlItemSelectStatus = (LinearLayout) mView.findViewById(R.id.fragment_item_search_select_status);

        // 商品名入力欄と商品選択状態欄は排他関係

        // 表示制御
        if (! isShow) {
            mLlItemNameLayer.setVisibility(View.GONE);
            mLlItemSelectStatus.setVisibility(View.VISIBLE);
            return;
        }

        mLlItemNameLayer.setVisibility(View.VISIBLE);
        mLlItemSelectStatus.setVisibility(View.GONE);

        // 入力
        mEtItemNameInput = (EditText) mView.findViewById(R.id.row_edit_item_info_item_name_input);
        mTvTextCount = (TextView) mView.findViewById(R.id.row_edit_item_info_text_count);
        mTvTextCountMax = (TextView) mView.findViewById(R.id.row_edit_item_info_text_count_max);
        mIbKeyboard = (ImageButton) mView.findViewById(R.id.row_edit_item_memo_keyboard);
        mIbKeyboard.setOnTouchListener(CKUtil.doOnTouchAnimation());
        mIbSearch = (ImageButton) mView.findViewById(R.id.fragment_item_search_button_search);
        mIbSearch.setOnTouchListener(CKUtil.doOnTouchAnimation());

        // 類語リスト(初期非表示)
        ArrayList<String> list = new ArrayList<>();
        mLvItemNameWords = (ListView) mView.findViewById(R.id.row_edit_item_info_synonym_words);
        mItemNameWordsAdapter = new ArrayAdapter<>(CKUtil.getMyContext(), R.layout.grid_synonym_word, list);
        mLvItemNameWords.setAdapter(mItemNameWordsAdapter);
        setVisibilityRefreshItemNameWordsList(false);

        // キーボード表示制御
        mIbKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CKUtil.hideKeyboad(mView);
            }
        });

        // フォーカス処理
        mEtItemNameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // キーボードの表示制御
                if (! hasFocus) {
                    CKUtil.hideKeyboad(mView);
                }
            }
        });

        // 検索ボタン
        mIbSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLog();
                // 条件変更後のリスト再表示
                mSearchCondition.setSearchWord(mEtItemNameInput.getText().toString());
                setItemListView();
                // 検索したらキーボードを非表示
                CKUtil.hideKeyboad(mView);
                mView.requestFocus();
            }
        });

        // 商品名
        setTextValueWithEvent("", false);

        // 最初はキーボードを非表示
        CKUtil.hideKeyboad(mView);
        mView.requestFocus();
    }

    // テキストを設定
    private void setTextValueWithEvent(String text, boolean fireEvent) {

        if (fireEvent) {
            mEtItemNameInput.setText(text);
        } else {
            removeInputNameTextWatcher();
            mEtItemNameInput.setText(text);
            addInputNameTextWatcher();
        }

        if (mEtItemNameInput.getVisibility() == View.VISIBLE && ! text.equals("")) {
            mEtItemNameInput.setSelection(text.length());
        }
        mTvTextCount.setText(String.valueOf(mEtItemNameInput.getText().length()));
        String message = " / " + String.valueOf(CKDBUtil.MAX_LENGTH_ITEM_NAME);
        mTvTextCountMax.setText(message);
    }

    // TextWatcherあたり
    private void removeInputNameTextWatcher() {
        if (mTextWatcher != null) {
            mEtItemNameInput.removeTextChangedListener(mTextWatcher);
        }
    }
    private void addInputNameTextWatcher() {

        // イベント停止中、更新されないので文字を数えなおし
        mTvTextCount.setText(String.valueOf(mEtItemNameInput.length()));

        if (mTextWatcher == null) {

            mTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override
                public void afterTextChanged(Editable s) {

                    // 入力文字に変化がある場合
                    String itemName = s.toString();

                    if (! itemName.equals(mPreInputWord)) {
                        mPreInputWord = itemName;

                        // 改行禁止
                        if (itemName.contains("\n")) {
                            itemName = itemName.replaceAll("\n", "");
                            s.replace(0,s.length(), itemName);
                        }
                        if (itemName.length() > CKDBUtil.MAX_LENGTH_ITEM_NAME) {
                            itemName = itemName.substring(0, CKDBUtil.MAX_LENGTH_ITEM_NAME);
                            s.replace(0,s.length(), itemName);
                        }
                        mTvTextCount.setText(String.valueOf(itemName.length()));

                        // 類語テキストをDBから取得してリストへ設定
                        refreshItemNameWordsList(CKDBUtil.getDAItem().getSynonimItemName(itemName));
                    }

                    if (itemName.equals("")) {
                        hideItemNameWordList();
                    }
                }
            };
        }

        mEtItemNameInput.addTextChangedListener(mTextWatcher);
    }

    // リストを非表示
    private void hideItemNameWordList() {

        mItemNameWordsAdapter.clear();
        setVisibilityRefreshItemNameWordsList(false);
    }

    // 類語リストの表示制御
    private void setVisibilityRefreshItemNameWordsList(boolean isShow) {

        if (mLvItemNameWords != null) {
            mLvItemNameWords.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    // 単語数によるListViewの高さ制限
    private void setListMaxHeight(ListView listView, int rowCount) {

        final int _MAX_ROW = 7;

        View childView = listView.getAdapter().getView(0, null, listView);
        childView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                , View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        ViewGroup.LayoutParams lp = listView.getLayoutParams();
        if (rowCount > _MAX_ROW) {
            rowCount = _MAX_ROW;
        }
        lp.height = childView.getMeasuredHeight() * rowCount + 10;
    }

    // 類語リストリフレッシュ
    private void refreshItemNameWordsList(ArrayList<String> result) {

        if (mLvItemNameWords == null) { return; }

        // 類語リスト
        if (result == null || result.size() == 0) {
            // リストを隠す
            hideItemNameWordList();
        } else {
            mItemNameWordsAdapter.clear();
            mItemNameWordsAdapter.addAll(result);
            // 高さ調整
            setListMaxHeight(mLvItemNameWords, result.size());
            setVisibilityRefreshItemNameWordsList(true);

            // 単語選択されたら入力欄へ設定してリストを閉じる
            mLvItemNameWords.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String select = (String) parent.getItemAtPosition(position);
                    if (!select.equals("")) {
                        // 値設定
                        setTextValueWithEvent(select, false);
                        mEtItemNameInput.requestFocus();
                        // リストを隠す
                        hideItemNameWordList();
                    }
                }
            });
        }
    }
}

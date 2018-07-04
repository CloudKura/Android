package com.cloudkura.light;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudkura.light.ui.camera.CameraSource;
import com.cloudkura.light.ui.camera.CameraSourcePreview;
import com.cloudkura.light.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.Calendar;
import java.util.Map;

public class DialogItemRegist extends DialogFragment
{

    //引数名定義
    public final static String ARG_IID = "searchIid";
    public final static String ARG_LIMIT_DATE = "limitDate";
    public final static String ARG_IS_WISHLIST = "isWishList";
    // 検索キー
    static String mArgIid = "";
    static String mArgLimitDate = "";
    static boolean mArgIsWishList = false;

    // 親Activity
    static ActivityMain mParentActivity;

    // 画面の部品
    Dialog mDialog;
    View mView;
    SectionsPagerAdapter mSectionsPagerAdapter;
    MyNoScrollViewPager mViewPager;

    // ViewPagerのページ番号
    static final int _VIEW_PAGE_BARCODE = 0;
    static final int _VIEW_PAGE_IMAGE = 1;
    static final int _VIEW_PAGE_INFO = 2;

    // モード判定用
    enum CameraViewMode {
        inShowBackground
        ,inBarcode
        ,inPreview
        ,inImage
    }

    // データ保持
    // 品目・ストック表示情報(DB登録値)
    static DAItem mItem;
    static DAItemStock mItemStock;
    static String mShowBarcodeValue = "";

    // DBに登録済みデータの有無
    static boolean mIsNewData = true;
    // 期限変更
    static boolean mIsLimitDateChanged = false;

    public static DialogItemRegist newInstance() {
        return new DialogItemRegist();
    }

    public static DialogItemRegist newInstance(Fragment target, int requestCode) {
        DialogItemRegist fragment = new DialogItemRegist();
        fragment.setTargetFragment(target , requestCode);
        return fragment;
    }

    // ログ書き出し
    static private void writeLog() {
        CKFB.writeLog(DialogItemRegist.class.getSimpleName());
    }
    static private void writeLog(String value) {
        CKFB.writeLog(DialogItemRegist.class.getSimpleName(), value);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // 引数取得
        mArgIid = getArguments().getString(ARG_IID);
        mArgIid = (mArgIid == null ? "" : mArgIid);
        mArgLimitDate = getArguments().getString(ARG_LIMIT_DATE);
        mArgLimitDate = (mArgLimitDate == null ? "" : mArgLimitDate);
        mArgIsWishList = getArguments().getBoolean(ARG_IS_WISHLIST);
        mShowBarcodeValue = "";

        // データ設定
        refreshItemData(null);

        mView = inflater.inflate(R.layout.fragment_item_regist_view_pager, container, false);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), this);

        // バーコード欄を初期表示
        mViewPager = (MyNoScrollViewPager) mView.findViewById(R.id.fragment_item_new_container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // 初期表示画面を選択
        if (mIsNewData) {
            // 新規の場合はバーコード
            mViewPager.setCurrentItem(_VIEW_PAGE_BARCODE);
        } else {
            // 更新の場合は商品情報
            mViewPager.setCurrentItem(_VIEW_PAGE_INFO);
        }

        return mView;
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writeLog();

        //ダイアログの作成
        // 画面いっぱいに表示
        mDialog = new Dialog(getActivity(), R.style.NoDimDialogFragmentStyle);
        mDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mDialog.setContentView(inflater.inflate(R.layout.fragment_item_regist_view_pager, null, false));
        //mDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        //mDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationX;
        mDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationFade;

        CKUtil.hideKeyboad(mView);

        return mDialog;
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
    public void onDetach() { super.onDetach(); }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        writeLog();

        if (resultCode == Activity.RESULT_OK) {
            // 自分画面を閉じる
            dismiss();
        }else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // 画面表示用データを設定
    // 新規画面ロード時、またはweb情報取得時に呼ばれる。
    static private void refreshItemData(Map<String, String> webInfo) {

        // 初期化
        mItem = null;
        mItem = CKDBUtil.getDAItem();
        mItemStock = null;
        mItemStock = CKDBUtil.getDAItemStock();

        // データ存在フラグを初期化
        mIsNewData = true;
        String limitDate = CKUtil.getCurrentDate();

        String iid = "";
        if (webInfo == null) {
            // webからの取得値なし

            // 検索キーでの既存データ有無判定
            // IIDを初期登録。
            iid = (mArgIid.equals("") ? iid : mArgIid);
            iid = (mShowBarcodeValue.equals("") ? iid : mShowBarcodeValue);
            iid = (iid.equals("") ?  CKUtil.createObjectId() : iid);
            Map<String, String> item = CKDBUtil.getDAItem().getUpsertByIId(iid);

            // 既存値がなければ新規モード
            if (item == null) {
                mItem.setValue(DAItem.col_IId, iid);
                mItem.setValue(DAItem.col_Barcode, mShowBarcodeValue);
                mItem.setValue(DAItem.col_IsHandFlag, DAItem.val_HandOn);

            } else {
                mIsNewData = false;
                mItem.setAllValue(item);
                mItem.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOff);  // 非表示フラグを強制解除
            }
            // 既存商品があればストックも取得
            if (mArgLimitDate.equals("")) {
                mItemStock.setValue(DAItemStock.col_IId, iid);
                mItemStock.setLimitDate(limitDate);
                mItemStock.setValue(DAItemStock.col_ItemCount, "1");
            } else {
                mItemStock.setAllValue(CKDBUtil.getDAItemStock().getItemByKey(iid, mArgLimitDate));
            }
        } else {

            // webからの取得値があれば設定
            iid = webInfo.get(CKWebService._KEY_BARCODE);
            Map<String, String> item = CKDBUtil.getDAItem().getUpsertByIId(iid);

            // Webから取得した情報があって同一バーコードの既存値がなければ保存する。
            if (item != null) {
                mIsNewData = false;
                mItem.setAllValue(item);
                mItem.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOff);
            } else {
                // バーコードをキーとして上書き
                mItem.setValue(DAItem.col_IId, iid);
                String explain = webInfo.get(CKWebService._KEY_ITEM_NAME);
                if (explain != null && explain.length() > CKDBUtil.MAX_LENGTH_ITEM_NAME) {
                    explain = explain.substring(0, CKDBUtil.MAX_LENGTH_ITEM_NAME);
                }
                mItem.setValue(DAItem.col_Explain, explain);
                mItem.setValue(DAItem.col_ImagePath, webInfo.get(CKWebService._KEY_IMAGE_PATH));
                mItem.setValue(DAItem.col_Calorie, webInfo.get(CKWebService._KEY_LIMIT_DAYS));      // ToDo: カロリー列に期限日数を保存(暫定処置)
                mItem.setValue(DAItem.col_Barcode, webInfo.get(CKWebService._KEY_BARCODE));
                mItem.setValue(DAItem.col_IsHandFlag, webInfo.get(CKWebService._KEY_SOURCE_FLG));
                mItem.setValue(DAItem.col_IsHidden, DAItem.val_HiddenOff);
            }

            // ストックの初期値設定
            String limitDays = webInfo.get(CKWebService._KEY_LIMIT_DAYS);
            if (! (limitDays == null || limitDays.equals(""))) {
                limitDate = CKUtil.calcDate(limitDate, Integer.valueOf(limitDays));
            }
            mItemStock.setValue(DAItemStock.col_IId, iid);
            mItemStock.setLimitDate(limitDate);
            mItemStock.setValue(DAItemStock.col_ItemCount, "1");
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        // ViewPager - Page Title
        static final String _PAGE_BARCODE = "barcode";
        static final String _PAGE_IMAGE = "image";
        static final String _PAGE_INFO = "info";

        private DialogFragment mDF;

        SectionsPagerAdapter(FragmentManager fm, DialogFragment df) {
            super(fm);
            mDF = df;
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(mDF, getTargetRequestCode(), position, mArgIid);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case _VIEW_PAGE_BARCODE:
                    return _PAGE_BARCODE;
                case _VIEW_PAGE_IMAGE:
                    return _PAGE_IMAGE;
                case _VIEW_PAGE_INFO:
                    return _PAGE_INFO;
            }
            return null;
        }

    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public class PlaceholderFragment extends Fragment {
        // ログ出力用
        private static final String TAG = "Barcode-reader";
        // ページ指定
        static final String ARG_SECTION_NUMBER = "section_number";

        Toast mToast;

        View mView;
        ViewPager mViewPager;
        TextView mTvTitle;

        CameraViewMode mCameraViewMode = CameraViewMode.inShowBackground;

        // バーコード、画像共通
        ImageView mIvFlashIcon;
        ProgressBar mPbCountDown;
        BarcodeDetectorCountDown mBarcodeDetectorCountDown;
        CameraSourcePreview mCameraPreviewBarcode;
        CameraSourcePreview mCameraPreviewImage;
        CameraSource mCameraSource;
        Button mBtCamera;
        ImageView mIvSwitchFrame;

        // バーコード欄
        GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

        // 画像欄
        ImageView mImgPhoto;
        LinearLayout mLnExplainArea;
        LinearLayout mLnMemoDisplayContainer;
        TextView mTvMemoDisplayTitle;
        TextView mTvMemoDisplay;

        // 商品名欄
        LinearLayout mLlInfoInput;
        EditText mEtItemNameInput;
        TextView mTvItemName;
        TextView mTvTextCount;
        TextView mTvTextCountMax;
        TextWatcher mTextWatcher;
        ImageButton mIbKeyboard;
        /*
        ListView mLvSynonymWords;
        ArrayAdapter<String> mSynonymWordsAdapter;
        */

        // ストック欄
        LinearLayout mLlInfoStock;
        MyDatePicker mDpLimitDt;
        NumberPicker mNpCount10;
        NumberPicker mNpCount01;

        public PlaceholderFragment() {}

        static public PlaceholderFragment newInstance(Fragment target, int requestCode, int sectionNumber, String searchKey) {

            PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.setTargetFragment(target, requestCode);
            Bundle args = new Bundle();
            args.putString(ARG_IID, searchKey);
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                 final Bundle savedInstanceState) {

            Button btPrev;
            Button btNext;

            mViewPager = (ViewPager) container;
            View rootView = getParentFragment().getView();

            // 画面タイトル設定
            mTvTitle = (TextView) rootView.findViewById(R.id.fragment_item_new_view_pager_title);
            // キャンセルボタン
            Button btCancel = (Button) rootView.findViewById(R.id.fragment_item_new_view_pager_button_cancel);
            btCancel.setOnTouchListener(CKUtil.doOnTouchAnimation());
            btCancel.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    setOnCancerClicked();
                }
            });

            mView = null;
            Bundle arg = getArguments();

            switch (arg.getInt(ARG_SECTION_NUMBER)){
                case _VIEW_PAGE_BARCODE:
                    // バーコード欄設定
                    mView = inflater.inflate(R.layout.fragment_item_regist_barcode, container, false);

                    // 戻るボタン(非表示)
                    btPrev = (Button) mView.findViewById(R.id.fragment_item_new_prev_button);
                    btPrev.setOnTouchListener(CKUtil.doOnTouchAnimation());
                    btPrev.setVisibility(View.INVISIBLE);
                    // 次へボタン
                    btNext = (Button) mView.findViewById(R.id.fragment_item_new_next_button);
                    btNext.setOnTouchListener(CKUtil.doOnTouchAnimation());
                    btNext.setVisibility(View.INVISIBLE);

                    // バーコード制御
                    callEventRowBarcode();

                    break;

                case _VIEW_PAGE_IMAGE:
                    // 画像欄設定
                    mView = inflater.inflate(R.layout.fragment_item_regist_image, container, false);

                    // 戻るボタン(非表示)
                    btPrev = (Button) mView.findViewById(R.id.fragment_item_new_prev_button);
                    btPrev.setOnTouchListener(CKUtil.doOnTouchAnimation());
                    btPrev.setVisibility(View.INVISIBLE);

                    // 次へボタン
                    btNext = (Button) mView.findViewById(R.id.fragment_item_new_next_button);
                    btNext.setOnTouchListener(CKUtil.doOnTouchAnimation());
                    btNext.setText(R.string.regist_next_button);
                    btNext.setBackground(CKUtil.getMyDrawable(R.drawable.ic_button_action));
                    btNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            writeLog();
                            // ページ移動(更新の場合はストック入力をスキップ)
                            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                        }
                    });

                    // 画像指定
                    callEventRowImage();

                    // キーボードを隠す
                    CKUtil.hideKeyboad(mView);

                    break;

                case _VIEW_PAGE_INFO:
                    // 商品情報入力欄設定
                    mView = inflater.inflate(R.layout.fragment_item_regist_info, container, false);

                    // 戻るボタン
                    btPrev = (Button) mView.findViewById(R.id.fragment_item_new_prev_button);
                    btPrev.setOnTouchListener(CKUtil.doOnTouchAnimation());
                    btPrev.setText(R.string.regist_prev_button);
                    btPrev.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v) {
                            writeLog();
                            // ページ移動
                            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
                        }
                    });
                    // 新規モードの場合のみ表示
                    btPrev.setVisibility(mIsNewData ? View.VISIBLE : View.INVISIBLE);

                    // 次へボタン
                    btNext = (Button) mView.findViewById(R.id.fragment_item_new_next_button);
                    btNext.setOnTouchListener(CKUtil.doOnTouchAnimation());
                    btNext.setText( mIsNewData ? CKUtil.getMyString(R.string.regist_regist_button) : CKUtil.getMyString(R.string.regist_update_button));
                    btNext.setBackground(CKUtil.getMyDrawable(R.drawable.ic_button_regist));
                    btNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            writeLog();
                            // 確定時のチェック
                            checkRegistDataCloseDialogValidity();
                        }
                    });

                    // サムネイル
                    callEventRowThumbnail();
                    // 商品名
                    callEventRowMemo();
                    // ストック
                    callEventRowStock();

                    break;

            }

            return mView;
        }

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);

            if (! isVisibleToUser) { return; }
            int sectionNo = getArguments().getInt(ARG_SECTION_NUMBER);

            if (mTvTitle != null) {
                switch (sectionNo) {
                    case _VIEW_PAGE_BARCODE:
                        mTvTitle.setText(R.string.regist_title_scan_barcode);

                        // プレビュー表示を開始
                        startInitPreview();

                        // ネットワーク未接続の場合は手入力への遷移確認
                        if (mCameraViewMode == CameraViewMode.inBarcode && ! CKUtil.isConnectNetwork()) {
                            // ネットワーク未接続時のダイアログを表示
                            showAlertDialogDisconnectNetwork();
                            return;
                        }

                        break;

                    case _VIEW_PAGE_IMAGE:
                        mTvTitle.setText(R.string.regist_title_image);

                        if (mBtCamera != null) {
                            if (mIsNewData) {
                                String imagePath = mItem.getValue(DAItem.col_ImagePath);
                                if (imagePath == null || imagePath.equals("")) {
                                    // 画像の保存がなければプレビュー
                                    setCameraPreviewMode(CameraViewMode.inPreview, mCameraPreviewImage);
                                } else {
                                    // 新規の場合で画像がある
                                    stopCameraToSetImageView();
                                    // 商品名を設定
                                    String itemName = mItem.getValue(DAItem.col_Explain);
                                    if (itemName.trim().equals("")) {
                                        mLnMemoDisplayContainer.setVisibility(View.INVISIBLE);
                                    } else {
                                        mLnMemoDisplayContainer.setVisibility(View.VISIBLE);
                                        mTvMemoDisplay.setText(itemName);
                                    }
                                }
                            } else {
                                // 更新の場合はイメージ
                                stopCameraToSetImageView();
                            }
                        }

                        break;

                    case _VIEW_PAGE_INFO:

                        // カメラを閉じる
                        releaseCameraSource();

                        // 戻るボタン
                        Button btPrev = (Button) mView.findViewById(R.id.fragment_item_new_prev_button);
                        btPrev.setOnTouchListener(CKUtil.doOnTouchAnimation());

                        if (mIsNewData) {
                            mTvTitle.setText(R.string.regist_title_add);
                            btPrev.setVisibility(View.VISIBLE);
                        } else {
                            mTvTitle.setText(R.string.regist_title_edit);
                            btPrev.setVisibility(View.INVISIBLE);
                        }

                        // サムネイル
                        callEventRowThumbnail();
                        // 商品名
                        callEventRowMemo();
                        // ストック
                        callEventRowStock();

                        break;
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            // ViewPagerは生成時に1,2ページをonResumeまで実行、2ページ移動時には3ページのonResumeまで実行
            // 画面切り替え時のイベントには適さないため、setUserVisibleHintへ処理を記述する。
            setUserVisibleHint(getUserVisibleHint());
        }

        @Override
        public void onPause() {
            super.onPause();
            releaseCameraSource();
            finishCountDown();
        }

        @Override
        public void onStop() {
            super.onStop();
            releaseCameraSource();
            finishCountDown();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            releaseCameraSource();
            finishCountDown();
            System.gc();
        }

        // 手入力切替確認メッセージ(ネットワーク未接続)
        private void showAlertDialogDisconnectNetwork() {

            new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                    .setCancelable(false)
                    .setTitle(R.string.alert_dialog_title_confirm)
                    .setMessage(R.string.message_not_connect_change_manual)
                    .setPositiveButton(
                            R.string.alert_dialog_button_yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whitch) {
                                    writeLog();
                                    // 画像撮影
                                    moveToPageImage();
                                }
                            })
                    .setNegativeButton(
                            R.string.alert_dialog_button_no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whitch) {
                                    writeLog();
                                    // カメラの初期プレビュー開始
                                    startInitPreview();
                                }
                            })
                    .show();

        }

        // 手入力切替確認メッセージ(商品見つからず)
        private void showAlertDialogItemNotFound() {

            mParentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    new AlertDialog.Builder(getContext(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle(R.string.alert_dialog_title_confirm)
                            .setMessage(R.string.message_item_not_found_change_manual)
                            .setPositiveButton(
                                    R.string.alert_dialog_button_yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            writeLog(mShowBarcodeValue);
                                            // 画像撮影
                                            moveToPageImage();
                                        }
                                    })
                            .setNegativeButton(
                                    R.string.alert_dialog_button_no,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            writeLog();
                                            // カメラの初期プレビュー開始
                                            startInitPreview();
                                        }
                                    })
                            .show();
                        }
            });

        }

        // バーコード画面へ移動
        private void moveToPageBarcode() {
            // 保存バーコードをクリア
            mShowBarcodeValue = "";
            // フラッシュ消灯
            switchFlashMode(false);
            releaseCameraSource();
            mViewPager.setCurrentItem(_VIEW_PAGE_BARCODE);
        }

        // 商品イメージ画面へ移動
        private void moveToPageImage() {
            // 保存バーコードをクリア
            mShowBarcodeValue = "";
            // フラッシュ消灯
            switchFlashMode(false);
            releaseCameraSource();
            mViewPager.setCurrentItem(_VIEW_PAGE_IMAGE);
        }

        // 商品情報画面へ移動
        private void moveToPageItemInfo() {
            switchFlashMode(false);
            mViewPager.setCurrentItem(_VIEW_PAGE_INFO);
        }

        // キャンセルボタン
        private void setOnCancerClicked() {

            // バーコード画面の場合は確認なしに終了
            if ((mItem.getValue(DAItem.col_IId).equals("") || mItem.getValue(DAItem.col_ImagePath).equals("")) &&
                    (mCameraViewMode == CameraViewMode.inBarcode || mCameraViewMode == CameraViewMode.inShowBackground)) {
                // 画面を閉じる
                closeDialogItemEdit();
                return;
            }

            new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                    .setCancelable(false)
                    .setTitle(R.string.alert_dialog_title_confirm)
                    .setMessage(R.string.message_cancel_editing_close_dialog)
                    .setPositiveButton(
                            R.string.alert_dialog_button_yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whitch) {
                                    // 画面を閉じる
                                    closeDialogItemEdit();
                                }
                            })
                    .setNegativeButton(
                            R.string.alert_dialog_button_no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whitch) {
                                    // 何もしない
                                }
                            })
                    .show();
        }

        // バーコード欄関連のイベント定義
        private void callEventRowBarcode() {

            // 部品定義
            if (mIvFlashIcon == null) {
                // フラッシュアイコン
                mIvFlashIcon = (ImageView) mView.findViewById(R.id.row_edit_item_barcode_flash_icon);
                // カメラ用
                mPbCountDown = (ProgressBar) mView.findViewById(R.id.row_edit_item_barcode_progress);
                mLnExplainArea = (LinearLayout) mView.findViewById(R.id.row_edit_item_barcode_explain);
                mBtCamera = (Button) mView.findViewById(R.id.row_edit_item_image_detect_barcode);
                mBtCamera.setOnTouchListener(CKUtil.doOnTouchAnimation());
                // バーコード用
                mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) mView.findViewById(R.id.row_edit_item_image_barcode_graphic_overlay);
                // カメラへの変更用
                mIvSwitchFrame = (ImageView) mView.findViewById((R.id.row_edit_item_barcode_switch_frame));
                mIvSwitchFrame.setVisibility(View.VISIBLE); // デザインでは非表示にしているので表示する。（バーコード検索画面で表示させないので）
            }

            // プレビュー表示サイズ
            mCameraPreviewBarcode = (CameraSourcePreview) mView.findViewById(R.id.row_edit_item_barcode_camera_preview);
            ViewGroup.LayoutParams lp = mCameraPreviewBarcode.getLayoutParams();

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            lp.height = (int) (metrics.heightPixels * 0.6);
            mCameraPreviewBarcode.setLayoutParams(lp);
            // 説明画像を表示
            mLnExplainArea.setVisibility(View.VISIBLE);
            // プログレスバーを隠す
            mPbCountDown.setVisibility(View.INVISIBLE);

            // フラッシュのOn/Off
            mIvFlashIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchFlashMode(! isFlashOn());
                }
            });

            // カメラへの切り替え
            mIvSwitchFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moveToPageImage();
                }
            });

            // カメラの初期プレビュー開始
            //startInitPreview();

            // 初期表示
            //setUserVisibleHint(getUserVisibleHint());

            // イベント定義
            mBtCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        // バーコード読み取り開始
                        startToDetectBarcode();
                }
            });
        }

        //フラッシュの状態
        private boolean isFlashOn() {
            if (mCameraSource == null) { return false; }
            return mCameraSource.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH);
        }

        // 画像欄関連のイベント定義
        private void callEventRowImage() {

            // 部品定義
            if (mIvFlashIcon == null) {
                // フラッシュアイコン
                mIvFlashIcon = (ImageView) mView.findViewById(R.id.row_edit_item_image_flash_icon);
                // カメラ用
                mImgPhoto = (ImageView) mView.findViewById(R.id.row_edit_item_image_photo) ;
                mBtCamera = (Button) mView.findViewById(R.id.row_edit_item_image_camera_shutter);
                mBtCamera.setOnTouchListener(CKUtil.doOnTouchAnimation());
                mCameraPreviewImage = (CameraSourcePreview) mView.findViewById(R.id.row_edit_item_image_camera_preview);
                // 商品名
                mLnMemoDisplayContainer =(LinearLayout) mView.findViewById(R.id.row_edit_item_image_item_name_layer);
                mTvMemoDisplayTitle = (TextView) mView.findViewById(R.id.row_edit_item_image_item_name_title);
                mTvMemoDisplay = (TextView) mView.findViewById(R.id.row_edit_item_image_item_name);
                mLnMemoDisplayContainer.setVisibility(View.INVISIBLE);
                // バーコード認識への変更用
                mIvSwitchFrame = (ImageView) mView.findViewById((R.id.row_edit_item_image_switch_frame));
                mIvSwitchFrame.setVisibility(View.VISIBLE); // デザインでは非表示にしているので表示する。（バーコード検索画面で表示させないので）
            }

            // プレビュー表示と画像表示サイズを合わせる
            int padding = 4 * 2;
            ViewGroup.LayoutParams lp = mCameraPreviewImage.getLayoutParams();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            lp.width = metrics.widthPixels - padding;
            lp.height = lp.width;
            mCameraPreviewImage.setLayoutParams(lp);
            mImgPhoto.setLayoutParams(lp);

            // フラッシュのOn/Off
            mIvFlashIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchFlashMode(! isFlashOn());
                }
            });

            // バーコードへの切り替え
            mIvSwitchFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moveToPageBarcode();
                }
            });

            // 初期表示
            //setUserVisibleHint(getUserVisibleHint());

            // イベント定義
            mBtCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (mCameraViewMode) {
                        case inPreview:
                            writeLog("inPreview");
                            // プレビュー→イメージの切り替え
                            shutterCamera();
                            break;
                        case inImage:
                            writeLog("inImage");
                            // イメージ→プレビューの切り替え
                            setCameraPreviewMode(CameraViewMode.inPreview, mCameraPreviewImage);
                            break;
                    }
                }
            });
        }

        // カメラの初期プレビュー開始
        private void startInitPreview() {
            setCameraPreviewMode(CameraViewMode.inShowBackground, mCameraPreviewBarcode);
        }

        // バーコード読み取り開始
        private void startToDetectBarcode() {

            // ネットワーク未接続の場合は手入力への遷移確認
            if (! CKUtil.isConnectNetwork()) {
                // ネットワーク未接続時のダイアログを表示
                showAlertDialogDisconnectNetwork();
                return;
            }
            // バーコードモードでプレビュー表示
            setCameraPreviewMode(CameraViewMode.inBarcode, mCameraPreviewBarcode);
            // カウントダウン開始
            startCountDown();
        }

        // カウントダウン開始
        private void startCountDown() {
            int countDownMilliSecondTime = CKUtil.getCountDownSecondTime() * 1000;

            //// バーコード読取ボタンを無効化
            //mBtCamera.setEnabled(false);

            // バーコード読取ボタンのイベントをキャンセルへ変更
            mBtCamera.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_action_stop, 0, 0);
            mBtCamera.setOnClickListener(null);
            mBtCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // カウントダウン停止
                    finishCountDown();
                    // プレビュー再開
                    startInitPreview();
                }
            });

            // プログレスバー準備
            mPbCountDown.setMax(countDownMilliSecondTime);
            mPbCountDown.setProgress(0);
            mPbCountDown.setVisibility(View.VISIBLE);
            // カウントダウン開始
            mBarcodeDetectorCountDown = new BarcodeDetectorCountDown(countDownMilliSecondTime, 50);
            mBarcodeDetectorCountDown.start();

            // カメラ表示ボタンを非表示
            mIvSwitchFrame.setVisibility(View.GONE);
        }

        // カウントダウン終了
        private void finishCountDown() {
            if (mBarcodeDetectorCountDown != null) {
                // フラッシュ消灯
                switchFlashMode(false);
                // 進捗停止
                mBarcodeDetectorCountDown.cancel();
                mBarcodeDetectorCountDown = null;
                mPbCountDown.setProgress(0);
                mPbCountDown.setVisibility(View.INVISIBLE);
                //// バーコード読取ボタンを有効化
                //mBtCamera.setEnabled(true);
                // バーコード読取ボタンのイベントを読取へ変更
                mBtCamera.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_action_barcode_snap, 0, 0);
                mBtCamera.setOnClickListener(null);
                mBtCamera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // バーコード読み取り開始
                        startToDetectBarcode();
                    }
                });
                // カメラ表示ボタンを表示
                mIvSwitchFrame.setVisibility(View.VISIBLE);
            }
        }

        // カウントダウンクラス
        private class BarcodeDetectorCountDown extends CountDownTimer {

            long mMaxInterval = 0;
            BarcodeDetectorCountDown(long millisInFuture, long countDownInterval) {
                super(millisInFuture, countDownInterval);
                mMaxInterval = millisInFuture;
            }

            @Override
            public void onTick(long millisUntilFinished) {
                // プログレスバーの進捗更新
                mPbCountDown.setProgress((int)(mMaxInterval - millisUntilFinished));
            }

            @Override
            public void onFinish() {
                if (mPbCountDown == null) {
                    return;
                }
                // バーコードを取得できていたら抜ける(ぎりぎりで認識したケース)
                if (! mShowBarcodeValue.equals("")) {
                    return;
                }

                // カウントダウン停止
                finishCountDown();
                // 最後までバーコード認識できなかった場合は手入力画面への移動を確認する。
                new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                        .setCancelable(false)
                        .setTitle(R.string.alert_dialog_title_confirm)
                        .setMessage(R.string.message_not_scan_barcode_change_manual)
                        .setPositiveButton(
                                R.string.alert_dialog_button_yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whitch) {
                                        writeLog();
                                        // 画像撮影
                                        moveToPageImage();
                                    }
                                })
                        .setNegativeButton(
                                R.string.alert_dialog_button_no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whitch) {
                                        writeLog();
                                        // カメラの初期プレビュー開始
                                        startInitPreview();
                                    }
                                })
                        .show();
            }
        }

        // カメラ撮影
        private void shutterCamera() {

            if (mCameraSource == null) { return; }

            mIvFlashIcon.setVisibility(View.INVISIBLE);

            // カメラ撮影シャッター
            CameraSource.ShutterCallback shutterCallback = new CameraSource.ShutterCallback() {
                @Override
                public void onShutter() { }
            };
            // 撮影した画像データを取得
            CameraSource.PictureCallback pictureCallback = new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data) {
                    try
                    {

                        // 前回画像を削除
                        CKUtil.deletePictureFile(mItem.getValue(DAItem.col_ImagePath));

                        // 画像をトリミングして上書き
                        //String imagePath = CKUtil.savePictureSD(data);
                        //Bitmap bmCamera = CKUtil.trimCameraBitmap(imagePath, 4 * 2);
                        Bitmap bmCamera = CKUtil.trimCameraByteArray(data, 4 * 2);
                        String imagePath = CKUtil.getSavePicturePath();
                        CKUtil.overWritePictureSD(bmCamera, imagePath);
                        if (bmCamera != null) {
                            bmCamera.recycle();
                            bmCamera = null;
                        }

                        // 保存
                        mItem.setValue(DAItem.col_ImagePath, imagePath);

                        // カメラを止めて画像を表示
                        stopCameraToSetImageView();

                    } catch (Exception ex) {
                        CKUtil.showLongToast(ex.getMessage());
                        CKUtil.showLongToast(getString(R.string.message_failed_save_image));
                    }
                }
            };

            // 撮影モード開始
            mCameraSource.takePicture(shutterCallback, pictureCallback);
        }

        // カメラを止めて画像を表示
        private void stopCameraToSetImageView() {

            // モード変更
            mCameraViewMode = CameraViewMode.inImage;
            mIvFlashIcon.setVisibility(View.INVISIBLE);

            // フラッシュ消灯
            switchFlashMode(false);
            // カメラを閉じる
            releaseCameraSource();

            // 戻るボタンを非表示
            Button btPrev = (Button) mView.findViewById(R.id.fragment_item_new_prev_button);
            btPrev.setOnTouchListener(CKUtil.doOnTouchAnimation());
            btPrev.setVisibility(View.INVISIBLE);

            // 次へボタンを表示
            Button btNext = (Button) mView.findViewById(R.id.fragment_item_new_next_button);
            btNext.setVisibility(View.VISIBLE);
            btNext.setOnTouchListener(CKUtil.doOnTouchAnimation());

            // 画像を表示
            try {
                mImgPhoto.setImageBitmap(CKUtil.getBitmap(mItem.getValue(DAItem.col_ImagePath)));
            } catch (Exception e) {
                CKUtil.showLongToast(e.getMessage());
            }

            // イメージとプレビューを入れ替える
            setShowImageVisibility(true);
        }

        // 画像表示アニメーション
        private void setShowImageVisibility(boolean isShow) {

            if (mImgPhoto == null) { return; }

            // アニメーション定義
            Animation anim = AnimationUtils.loadAnimation(CKUtil.getMyContext(), isShow ? R.anim.slide_up : R.anim.slide_down);
            anim.setDuration(50);
            anim.setFillBefore(true);
            mImgPhoto.startAnimation(anim);

            // 表示制御
            mCameraPreviewImage.setVisibility(isShow ? View.GONE : View.VISIBLE);
            mImgPhoto.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }

        // フラッシュ切り替え
        private void switchFlashMode(boolean flashOn) {
            if (mCameraSource == null) { return; }
            if (isFlashOn() == flashOn) { return; }

            if (flashOn) {
                mIvFlashIcon.setImageResource(R.drawable.ic_flash_on);
                mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                mIvFlashIcon.setImageResource(R.drawable.ic_flash_off);
                mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        }

        // カメラ開始
        private void setCameraPreviewMode(CameraViewMode viewMode, CameraSourcePreview cameraPreview) {

            if (cameraPreview == null) { return; }

            // 保存バーコードをクリア
            mShowBarcodeValue = "";
            // 指定されたモードを保存
            mCameraViewMode = viewMode;

            // プレビュー中のボタン制御
            Button btPrev = (Button) mView.findViewById(R.id.fragment_item_new_prev_button);
            btPrev.setOnTouchListener(CKUtil.doOnTouchAnimation());
            if (mCameraViewMode != CameraViewMode.inPreview) {
                btPrev.setVisibility(View.INVISIBLE);
            } else {
                String imagePath = mItem.getValue(DAItem.col_ImagePath);
                if (imagePath != null && ! imagePath.equals("")) {
                    // 戻るボタンに撮影キャンセル機能を割り当て
                    btPrev.setText(R.string.stop_photographing);
                    btPrev.setVisibility(View.VISIBLE);
                    btPrev.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            writeLog(CKUtil.getMyString(R.string.stop_photographing));
                            stopCameraToSetImageView();
                        }
                    });
                }
            }

            // 次へボタンを非表示
            Button btNext = (Button) mView.findViewById(R.id.fragment_item_new_next_button);
            btNext.setOnTouchListener(CKUtil.doOnTouchAnimation());
            btNext.setVisibility(View.INVISIBLE);

            // バーコード読取/カメラボタンを有効化
            mBtCamera.setEnabled(true);

            // 商品名欄を非表示
            if (mLnMemoDisplayContainer != null) {
                mLnMemoDisplayContainer.setVisibility(View.INVISIBLE);
            }
            // イメージとプレビューを入れ替える
            setShowImageVisibility(false);

            // 説明画面の制御
            if (mLnExplainArea != null) {
                if (viewMode == CameraViewMode.inShowBackground) {
                    mLnExplainArea.setVisibility(View.VISIBLE);
                } else {
                    mLnExplainArea.setVisibility(View.INVISIBLE);
                }
            }

            try {
                // フラッシュ制御
                boolean flashMode = isFlashOn();
                switchFlashMode(flashMode);
                mIvFlashIcon.setVisibility(View.VISIBLE);

                if (mCameraSource == null){
                    // カメラをリリース
                    //releaseCameraSource();
                    CameraSource.Builder builder = new CameraSource.Builder(getContext(), startBarcodeDetection())
                            .setFacing(CameraSource.CAMERA_FACING_BACK)
                            .setRequestedPreviewSize(800, 600)
                            .setRequestedFps(60.0f)
                            .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                            .setFlashMode(flashMode ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                    mCameraSource = builder.build();
                    cameraPreview.start(mCameraSource);
                }
            } catch (SecurityException e) {
                // カメラを解放
                releaseCameraSource();
            } catch (Exception e) {
                releaseCameraSource();
            }
        }

        // 商品サムネイル関連のイベント定義
        private void callEventRowThumbnail() {

            // レイヤー
            mLlInfoInput = (LinearLayout) mView.findViewById(R.id.row_edit_item_thumbnail_layer) ;

            LinearLayout lnBarcodeContainer = (LinearLayout) mView.findViewById(R.id.row_edit_item_regist_info_barcode_container);
            lnBarcodeContainer.setVisibility(View.VISIBLE);
            if (! mItem.getValue(DAItem.col_IId).equals("")) {
                // バーコード
                String barcode =mItem.getValue(DAItem.col_Barcode);
                TextView tvBarcode = (TextView) mView.findViewById(R.id.row_edit_item_regist_info_barcode);
                tvBarcode.setText(barcode);
                        /*
                        if (barcode != null && ! barcode.equals("")) {
                            lnBarcodeContainer.setVisibility(View.VISIBLE);
                        } else {
                            lnBarcodeContainer.setVisibility(View.GONE);
                        }
                        */
            }

            // 編集モードの場合のみ表示
            mLlInfoInput.setVisibility(mIsNewData ? View.GONE : View.VISIBLE);

            if (! mIsNewData) {
                // 入力
                if (mImgPhoto == null) {
                    mImgPhoto = (ImageView) mView.findViewById(R.id.row_edit_item_thumbnail_photo);
                    mTvItemName = (TextView) mView.findViewById(R.id.row_edit_item_thumbnail_item_name);
                }

                // 値設定
                try {
                    mImgPhoto.setImageBitmap(CKUtil.getBitmap(mItem.getValue(DAItem.col_ImagePath)));
                } catch (Exception e) {
                    CKUtil.showLongToast(e.getMessage());
                }

                String itemName = mItem.getValue(DAItem.col_Explain);
                if (itemName == null || itemName.equals("")) {
                    mTvItemName.setText(CKUtil.getMyString(R.string.item_no_name));
                } else {
                    mTvItemName.setText(itemName);
                }
            }
        }

        // 商品名欄関連のイベント定義
        private void callEventRowMemo() {

            // レイヤー
            mLlInfoInput = (LinearLayout) mView.findViewById(R.id.row_edit_item_info_input) ;

            // 新規モードの場合のみ表示
            mLlInfoInput.setVisibility(mIsNewData ? View.VISIBLE : View.GONE);
            // 入力
            if (mEtItemNameInput == null) {
                mEtItemNameInput = (EditText) mView.findViewById(R.id.row_edit_item_info_item_name_input);
                mTvTextCount = (TextView) mView.findViewById(R.id.row_edit_item_info_text_count);
                mTvTextCountMax = (TextView) mView.findViewById(R.id.row_edit_item_info_text_count_max);
                mIbKeyboard = (ImageButton) mView.findViewById(R.id.row_edit_item_memo_keyboard);
                mIbKeyboard.setOnTouchListener(CKUtil.doOnTouchAnimation());
            }

            if (! mIsNewData) {
                return;
            }

            /*
            // 類語リスト(初期非表示)
            ArrayList<String> list = new ArrayList<>();
            mLvSynonymWords = (ListView) mView.findViewById(R.id.row_edit_item_info_synonym_words);
            mSynonymWordsAdapter = new ArrayAdapter<String>(CKUtil.getMyContext(), R.layout.grid_synonym_word, list);
            mLvSynonymWords.setAdapter(mSynonymWordsAdapter);
            setVisibilityRefreshSynonymWordsList(false);
            */

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

            // Wish追加時以外はキーボード初期非表示
            if (mArgIsWishList) {
                mEtItemNameInput.requestFocus();
                CKUtil.showKeyboad(mEtItemNameInput);
            } else {
                CKUtil.hideKeyboad(mView);
            }

            // 商品名
            setTextValueWithEvent(mItem.getValue(DAItem.col_Explain), false);
        }

        // テキストを設定
        private void setTextValueWithEvent(String text, boolean fireEvent) {

            mItem.setValue(DAItem.col_Explain, text);

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
            mItem.setValue(DAItem.col_Explain, mEtItemNameInput.getText().toString());

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
                        String preName = mItem.getValue(DAItem.col_Explain);

                        if (! itemName.equals(preName)) {
                            // 保存
                            mItem.setValue(DAItem.col_Explain, itemName);

                            //String itemName = mEtItemNameInput.getText().toString();
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

                            /*
                            // 入力後から類語テキストを検索して取得
                            if (! CKUtil.isConnectNetwork()) { return; }

                            // 類語テキストをWebから取得(非同期)
                            AsyncGetSynonymWords asyncConnectWebDb = new AsyncGetSynonymWords(itemName);
                            asyncConnectWebDb.setOnCallBack(new AsyncGetSynonymWords.CallbackTask() {
                                @Override
                                public void CallBack(ArrayList<String> result) {
                                    super.CallBack(result);
                                    // リストリフレッシュ
                                    refreshSynonymWordsList(result);
                                }
                            });
                            // 実行
                            asyncConnectWebDb.execute();
                            */
                        }
                    }
                };
            }

            mEtItemNameInput.addTextChangedListener(mTextWatcher);
        }

        /*
        // リストを非表示
        private void hideSynonymWordList() {

            mSynonymWordsAdapter.clear();
            setVisibilityRefreshSynonymWordsList(false);
        }

        // 類語リストの表示制御
        private void setVisibilityRefreshSynonymWordsList(boolean isShow) {

            if (mLvSynonymWords != null) {
                mLvSynonymWords.setVisibility(isShow ? View.VISIBLE : View.GONE);
            }
        }

        // 単語数によるListViewの高さ制限
        private void setListMaxHeight(ListView listView, int rowCount) {

            final int _MAX_ROW = 3;

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
        private void refreshSynonymWordsList(ArrayList<String> result) {

            if (mLvSynonymWords == null) { return; }

            // 類語リスト
            if (result == null || result.size() == 0) {
                // リストを隠す
                hideSynonymWordList();
            } else {
                mSynonymWordsAdapter.clear();
                mSynonymWordsAdapter.addAll(result);
                // 高さ調整
                setListMaxHeight(mLvSynonymWords, result.size());
                setVisibilityRefreshSynonymWordsList(true);

                // 単語選択されたら入力欄へ設定してリストを閉じる
                mLvSynonymWords.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String select = (String) parent.getItemAtPosition(position);
                        if (!select.equals("")) {
                            // 値設定
                            setTextValueWithEvent(select, false);
                            mEtItemNameInput.requestFocus();
                            // リストを隠す
                            hideSynonymWordList();
                        }
                    }
                });
            }
        }
        */

        // ストック関連のイベント定義
        private void callEventRowStock() {

            // レイヤー
            mLlInfoStock = (LinearLayout) mView.findViewById(R.id.row_edit_item_stock_container);

            // Wish追加時はストック欄を表示させずに終了
            if (mArgIsWishList) {
                mLlInfoStock.setVisibility(View.GONE);
                return;
            }

            mLlInfoStock.setVisibility(View.VISIBLE);

            // 日付・数量
            if (mDpLimitDt == null) {
                mDpLimitDt = (MyDatePicker) mView.findViewById(R.id.popup_edit_item_stock_limit_date);
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
            String limitDate = mItemStock.getLimitDate();
            if (limitDate == null || limitDate.equals("")) {
                limitDate = CKUtil.getCurrentDate();
                mItemStock.setLimitDate(limitDate);
            }
            calendarToday.setTime(CKUtil.isDate(limitDate));
            int year = calendarToday.get(Calendar.YEAR);
            int month = calendarToday.get(Calendar.MONTH);
            int day = calendarToday.get(Calendar.DAY_OF_MONTH);
            mDpLimitDt.init(year, month, day, new MyDatePicker.OnDateChangedListener(){
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    String newDt = CKUtil.getFormatDate(year, monthOfYear + 1, dayOfMonth);
                    // 日付変更判定
                    mIsLimitDateChanged = ! mItemStock.getLimitDate().equals(newDt);
                    // 変更値を保存
                    mItemStock.setLimitDate(newDt);
                }
            });

            // 数量
            String itemCountBuff = mItemStock.getValue(DAItemStock.col_ItemCount);
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

        }

        private void setItemStockCount() {
            mItemStock.setValue(DAItemStock.col_ItemCount, String.valueOf(mNpCount10.getValue() * 10 + mNpCount01.getValue()));
        }

        // カメラを解放
        private void releaseCameraSource() {
            if (mCameraSource != null) {
                mCameraSource.stop();
                mCameraSource.release();
                mCameraSource = null;
            }
            if (mCameraPreviewBarcode != null) {
                mCameraPreviewBarcode.stop();
                mCameraPreviewBarcode.release();
            }
            if (mCameraPreviewImage != null) {
                mCameraPreviewImage.stop();
                mCameraPreviewImage.release();
            }
        }

        // 終了時チェック
        private void checkRegistDataCloseDialogValidity() {

            String iid = mItem.getValue(DAItem.col_IId);

            // 入力チェック(新規モードの場合のみ)
            if (mIsNewData) {
                if (mItem.getValue(DAItem.col_ImagePath).equals("")) {
                    new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle(R.string.message_title_image)
                            .setMessage(R.string.message_appoint_image)
                            .setPositiveButton(
                                    R.string.alert_dialog_button_yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            // 画像撮影
                                            moveToPageImage();
                                        }
                                    })
                            .show();
                    return;
                }

                // 画像をトリミングして上書き
                String imagePath = mItem.getValue(DAItem.col_ImagePath);
                //Bitmap bitmap = CKUtil.decodeSampledBitmapFromFile(imagePath, 180, 180);
                Bitmap bitmap = CKUtil.decodeSampledBitmapFromFile(imagePath, 240, 240);
                CKUtil.overWritePictureSD(bitmap, imagePath);
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }

                // 画像パスと商品名を保存
                mItem.upsertData();
            }

            // 数量
            String inputCountBuff = mItemStock.getValue(DAItemStock.col_ItemCount);
            if (inputCountBuff == null || inputCountBuff.equals("") || inputCountBuff.equals("0")) {
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

            // 更新モードなら既存データを論理削除
            DAItemStock itemStock = CKDBUtil.getDAItemStock();
            if (! mArgIid.equals("") && ! mArgLimitDate.equals("")) {
                // キー設定
                itemStock.setValue(DAItemStock.col_IId, mArgIid);
                itemStock.setLimitDate(mArgLimitDate);
                // #v2.0.0～
                itemStock.deleteDataLogical();
                //itemStock.deleteData();
                // #v2.0.0～
            }

            // 画面で入力された消費期限で同一キーの判定
            Map<String, String> existData = CKDBUtil.getDAItemStock().getItemByKey(iid, mItemStock.getSystemFormatLimitDate());
            if (existData == null) {
                // 保存して画面を閉じる
                dismissDialogAfterUpdateData();

            } else {

                String preCount = existData.get(DAItemStock.col_ItemCount);
                if (preCount == null || preCount.equals("")) {
                    preCount = "0";
                }
                final String registCount = String.valueOf(Integer.valueOf(preCount)  + Integer.valueOf(inputCountBuff));

                if (Integer.valueOf(registCount) > CKDBUtil.MAX_ITEM_COUNT) {
                    // 追加不可を通知する。
                    new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle)
                            .setCancelable(false)
                            .setTitle(CKUtil.getMyString(R.string.message_add_stock_limit_day) + mItemStock.getLimitDate())
                            .setMessage(CKUtil.getMyString(R.string.message_not_add_by_limit_over) + String.valueOf(CKDBUtil.MAX_ITEM_COUNT))
                            .setPositiveButton(
                                    R.string.alert_dialog_button_yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whitch) {
                                            writeLog(CKUtil.getMyString(R.string.message_not_add_by_limit_over));
                                            // 何もしない
                                        }
                                    })
                            .show();

                } else {
                    if (preCount.equals("0")) {
                        // 画面編集後の値を設定
                        itemStock.setValue(DAItemStock.col_ItemCount, inputCountBuff);
                        // 保存して画面を閉じる
                        dismissDialogAfterUpdateData();

                    } else {

                        // 期限を変更している場合は数量の加算チェック
                        if (! mIsLimitDateChanged) {
                            // 画面編集後の値を設定
                            mItemStock.setValue(DAItemStock.col_ItemCount, inputCountBuff);
                            // 保存して画面を閉じる
                            dismissDialogAfterUpdateData();
                        } else {
                            String msg = CKUtil.getMyString(R.string.message_add_stock_limit_day) + mItemStock.getLimitDate() + "\n" +
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
                                                    writeLog(CKUtil.getMyString(R.string.message_already_regsited_same_limitday_item) + "/count:" + registCount);
                                                    // 画面編集後の値を設定
                                                    mItemStock.setValue(DAItemStock.col_ItemCount, registCount);
                                                    // 保存して画面を閉じる
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

        // 更新して画面を閉じる
        private void dismissDialogAfterUpdateData() {
            writeLog("更新");

            // Wishリスト登録の場合
            if (mArgIsWishList) {

                // 既存回数の取得(新規登録なら数量)
                String itemCount = CKDBUtil.getDAItemStock().getAddWishCount(mItemStock.getValue(DAItemStock.col_IId));
                if (itemCount.equals("0")) {
                    mItemStock.setValue(DAItemStock.col_ItemCount, itemCount);
                    mItemStock.setValue(DAItemStock.col_IsHidden, DAItemStock.val_HiddenOff);
                }

                // 期限・数量は固定
                mItemStock.setValue(DAItemStock.col_LimitDt, DAItemStock.val_ShoppingList);
                mItemStock.upsertData();
                // 商品タイプを指定
                mItem.setValue(DAItem.col_ItemType, DAItem.val_ItemType_Fill);
                mItem.upsertData();
            } else {
                // ストック情報を保存
                mItemStock.upsertData();
            }

            //登録先画面を表示
            mParentActivity.replaceToTabFragment(mArgIsWishList ? CKUtil.TabPage.TAB_NOSTOCK : CKUtil.TabPage.TAB_ITEMLIST);

            // 関連情報をリフレッシュ
            mParentActivity.refreshRelationInfo();

            // キーボードを隠す
            CKUtil.hideKeyboad(mView);

            // 追加メッセージ
            CKUtil.showLongToast(getString(R.string.message_add_stock_list));
            // 画面を閉じる
            closeDialogItemEdit();
        }

        // 画面を閉じる
        private void closeDialogItemEdit() {
            Fragment target = getTargetFragment();
            if (target != null) {
                // カメラを解放
                releaseCameraSource();
                target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            }
        }

        // 検索中のToast
        private void showSearchingToast() {
            if (mToast == null) {
                mToast = new Toast(CKUtil.getMyContext());
            } else {
                mToast.cancel();
            }

            View viewToast = getActivity().getLayoutInflater().inflate(R.layout.toast_layout, null);
            TextView toastText = (TextView) viewToast.findViewById(R.id.toast_text);
            toastText.setText("\n" + CKUtil.getMyString(R.string.recognise_barcode_search_item_info) + "\n\n" + mShowBarcodeValue + "\n");
            toastText.setPadding(20,20, 20, 20);
            toastText.setGravity(Gravity.CENTER);
            mToast.setView(viewToast);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }
        /**
         * Creates and starts the camera.  Note that this uses a higher resolution in comparison
         * to other detection examples to enable the barcode detector to detect small barcodes
         * at long distances.
         *
         * Suppressing InlinedApi since there is a check that the minimum version is met before using
         * the constant.
         */
        @SuppressLint("InlinedApi")
        private BarcodeDetector startBarcodeDetection() {

            Context context = getContext();

            // A barcode detector is created to track barcodes.  An associated multi-processor instance
            // is set to receive the barcode detection results, track the barcodes, and maintain
            // graphics for each barcode on screen.  The factory is used by the multi-processor to
            // create a separate tracker instance for each barcode.
            BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
            BarcodeTrackerFactory barcodeFactory
                    = new BarcodeTrackerFactory(mGraphicOverlay
                    , new BarcodeGraphicTracker.Callback() {
                @Override
                public void onFound(String barcodeValue) {

                    // バーコード読取モードでなければ抜ける
                    if (mCameraViewMode != CameraViewMode.inBarcode) {
                        return;
                    }
                    // カウントダウンが終了していたら抜ける
                    if (mBarcodeDetectorCountDown == null) {
                        return;
                    }
                    // 連続して同じバーコードを取得した場合は処理スキップ
                    if (barcodeValue.equals(mShowBarcodeValue)) {
                        return;
                    }

                    // 別スレッド作成
                    final HandlerThread handlerThread = new HandlerThread("HandlerThread");
                    handlerThread.start();
                    final Handler handler = new Handler(handlerThread.getLooper());

                    //　新規に認識したバーコードを保存
                    mShowBarcodeValue = barcodeValue;

                    // 取得したバーコードで検索
                    // ネットワーク接続時のみ
                    if ( ! CKUtil.isConnectNetwork() ) {
                        // メッセージ通知
                        mParentActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                // カウントダウン停止
                                finishCountDown();
                                // ネットワーク未接続時のダイアログを表示
                                showAlertDialogDisconnectNetwork();
                            }
                        });
                    } else {
                        // WebAPIからの商品情報取得
                        try {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // カウントダウン停止
                                    mParentActivity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            finishCountDown();
                                        }
                                    });

                                    // 検索開始の通知
                                    showSearchingToast();
                                }
                            });

                            // バーコードが数値以外なら検索をやめる
                            boolean isNumberBarcode = true;
                            for (int i = 0; i < mShowBarcodeValue.length(); i++) {
                                try {
                                    Integer.parseInt(mShowBarcodeValue.substring(i, i + 1));
                                } catch (NumberFormatException e) {
                                    isNumberBarcode = false;
                                    break;
                                }
                            }
                            Map<String, String>  webinfo = null;
                            if (isNumberBarcode) {

                                webinfo = CKDBUtil.getDAItem().getUpsertByIId(mShowBarcodeValue);
                                // ローカルDB→CKDB→Google→楽天→UPC検索の順で検索
                                // Cloud Kura 商品DB API
                                if (webinfo == null) {
                                    // 検索開始の通知
                                    webinfo = CKWebService.getItemInfoFromCloudKuraWebDB(mShowBarcodeValue);
                                }
                                // Google検索
                                if (webinfo == null) {
                                    webinfo = CKWebService.getItemInfoFromGoogleShoppingSearch(mShowBarcodeValue);
                                }
                                if (webinfo == null) {
                                    // 国内JANコードは49,45の2種類＋図書JANコード978
                                    String barcodeFilter = mShowBarcodeValue.substring(0,2);
                                    String barcodeBook = mShowBarcodeValue.substring(0,3);

                                    showSearchingToast();
                                    if (barcodeFilter.equals("49") || barcodeFilter.equals("45") || barcodeBook.endsWith("978")) {
                                        // 楽天API
                                        webinfo = CKWebService.getItemInfoFromRakuten(mShowBarcodeValue);
                                    } else {
                                        // search UPC
                                        webinfo = CKWebService.getItemInfoFromSearchUPCSite(mShowBarcodeValue);
                                    }
                                }
                                // 取得した値を設定
                                refreshItemData(webinfo);
                            }

                            // Toast通知を閉じる
                            if (mToast != null) {
                                mToast.cancel();
                                mToast = null;
                            }
                            if (webinfo == null || webinfo.size() == 0) {
                                // バーコードに紐づく商品情報を見つけられない場合

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 商品情報未取得時のダイアログを表示
                                        showAlertDialogItemNotFound();

                                        // カウントダウン停止
                                        mParentActivity.runOnUiThread(new Runnable() {
                                            public void run() {
                                                finishCountDown();
                                            }
                                        });
                                    }
                                });
                            } else {
                                // バーコードの商品情報を見つけられた場合
                                mParentActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (mIsNewData) {
                                            // 画像撮影
                                            moveToPageImage();
                                        } else {
                                            // すでに登録済みのデータの場合は商品情報画面へ
                                            moveToPageItemInfo();
                                        }
                                    }
                                });
                            }

                        } catch (Exception ex) {
                            Log.d(ex.getClass().toString(), ex.getMessage());
                        }
                    }
                    //handlerThread.quitSafely();
                }
            });
            barcodeDetector.setProcessor(
                    new MultiProcessor.Builder<>(barcodeFactory).build());

            if (!barcodeDetector.isOperational()) {
                Log.w(TAG, "Detector dependencies are not yet available.");
                if (mParentActivity.registerReceiver(null, new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)) != null) {
                    CKUtil.showLongToast(getString(R.string.message_not_ready_barcode_recognitive_by_not_enough_strage));
                }
            }

            return barcodeDetector;
        }
    }

}

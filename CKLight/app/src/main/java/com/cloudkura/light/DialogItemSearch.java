package com.cloudkura.light;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cloudkura.light.ui.camera.CameraSource;
import com.cloudkura.light.ui.camera.CameraSourcePreview;
import com.cloudkura.light.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class DialogItemSearch extends DialogFragment
{

    // ログ出力用
    private static final String TAG = "Barcode-reader";

    //引数名定義
    public final static String ARG_SEARCH_MODE = "searchMode";

    enum SearchMode {
        Barcode,
        ItemName
    }

    // モード判定用
    private enum CameraViewMode {
        inShowBackground
        ,inBarcode
    }

    // 検索キー
    static SearchMode mArgSearchMode = SearchMode.Barcode;

    // 親Activity
    static ActivityMain mParentActivity;

    // データ保持
    String mShowBarcodeValue = "";
    CameraViewMode mCameraViewMode = CameraViewMode.inShowBackground;
    String mPreInputWord = "";

    // 画面の部品
    Dialog mDialog;
    View mView;
    TextView mTvTitle;
    Button mBtnCancel;

    LinearLayout mLnBarcodeContainer;
    LinearLayout mLnItemNameContainer;

    // バーコード、画像共通
    ImageView mIvFlashIcon;
    ProgressBar mPbCountDown;
    BarcodeDetectorCountDown mBarcodeDetectorCountDown;
    CameraSourcePreview mCameraPreview;
    CameraSource mCameraSource;
    Button mBtCamera;

    // バーコード欄
    GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    TextView mTvMsg;
    LinearLayout mLnExplainArea;

    // 商品名欄
    LinearLayout mLlInfoInput;
    EditText mEtItemNameInput;
    TextView mTvTextCount;
    TextView mTvTextCountMax;
    TextWatcher mTextWatcher;
    ImageButton mIbKeyboard;
    ListView mLvItemNameWords;
    ArrayAdapter<String> mItemNameWordsAdapter;
    ImageButton mIbSearch;

    public static DialogItemSearch newInstance() {
        return new DialogItemSearch();
    }

    public static DialogItemSearch newInstance(Fragment target, int requestCode) {
        DialogItemSearch fragment = new DialogItemSearch();
        fragment.setTargetFragment(target , requestCode);
        return fragment;
    }

    // ログ書き出し
    private void writeLog() {
        CKFB.writeLog(this.getClass().getSimpleName());
    }
    private void writeLog(String value) {
        CKFB.writeLog(this.getClass().getSimpleName(), value);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        writeLog();

        // 引数取得
        mArgSearchMode = (SearchMode) getArguments().getSerializable(ARG_SEARCH_MODE);

        mView = inflater.inflate(R.layout.fragment_item_search, container, false);
        mTvTitle = (TextView) mView.findViewById(R.id.fragment_item_search_title);
        mBtnCancel = (Button) mView.findViewById(R.id.fragment_item_search_button_cancel);
        mLnBarcodeContainer = (LinearLayout) mView.findViewById(R.id.row_edit_item_barcode_layer);
        mLnItemNameContainer = (LinearLayout) mView.findViewById(R.id.fragment_item_search_item_name);

        // 初期表示画面を選択
        switch (mArgSearchMode) {
            case Barcode:
                // バーコード検索モード
                mLnBarcodeContainer.setVisibility(View.VISIBLE);
                mLnItemNameContainer.setVisibility(View.GONE);
                mTvTitle.setText(R.string.search_by_barcode);

                // バーコード制御
                callEventRowBarcode();

                break;

            case ItemName:
                // 商品名検索モード
                mLnBarcodeContainer.setVisibility(View.GONE);
                mLnItemNameContainer.setVisibility(View.VISIBLE);
                mTvTitle.setText(R.string.search_by_item_name);

                // カメラを閉じる
                releaseCameraSource();
                // 商品名
                callEventRowMemo();

                break;
        }

        // キャンセルボタン
        mBtnCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                writeLog();
                // カメラを解放
                releaseCameraSource();
                dismiss();
            }
        });

        return mView;
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ダイアログの作成
        // 画面いっぱいに表示
        mDialog = new Dialog(getActivity(), R.style.NoDimDialogFragmentStyle);
        mDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mDialog.setContentView(inflater.inflate(R.layout.fragment_item_regist_view_pager, null, false));
        //mDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        //mDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationX;

        mDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationFade;

        // キーボード非表示
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            // 自分画面を閉じる
            dismiss();
        }else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // ViewPagerは生成時に1,2ページをonResumeまで実行、2ページ移動時には3ページのonResumeまで実行
        // 画面切り替え時のイベントには適さないため、setUserVisibleHintへ処理を記述する。
        //setUserVisibleHint(getUserVisibleHint());
        // カメラの初期プレビュー開始
        startInitPreview();
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

    // バーコード欄関連のイベント定義
    private void callEventRowBarcode() {

        // 部品定義
        // フラッシュアイコン
        mIvFlashIcon = (ImageView) mView.findViewById(R.id.row_edit_item_barcode_flash_icon);
        // カメラ用
        mPbCountDown = (ProgressBar) mView.findViewById(R.id.row_edit_item_barcode_progress);
        mBtCamera = (Button) mView.findViewById(R.id.row_edit_item_image_detect_barcode);
        mBtCamera.setOnTouchListener(CKUtil.doOnTouchAnimation());
        mCameraPreview = (CameraSourcePreview) mView.findViewById(R.id.row_edit_item_barcode_camera_preview);
        // バーコード用
        mTvMsg = (TextView) mView.findViewById(R.id.row_edit_item_image_barcode_msg);
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) mView.findViewById(R.id.row_edit_item_image_barcode_graphic_overlay);
        mLnExplainArea = (LinearLayout) mView.findViewById(R.id.row_edit_item_barcode_explain);

        // プレビュー表示サイズ
        ViewGroup.LayoutParams lp = mCameraPreview.getLayoutParams();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        lp.height = (int) (metrics.heightPixels * 0.6);
        mCameraPreview.setLayoutParams(lp);
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

        // カメラの初期プレビュー開始
        startInitPreview();

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

    // カメラの初期プレビュー開始
    private void startInitPreview() {
        setCameraPreviewMode(CameraViewMode.inShowBackground);
    }

    // バーコード読み取り開始
    private void startToDetectBarcode() {
        // バーコードモードでプレビュー表示
        setCameraPreviewMode(CameraViewMode.inBarcode);
        // カウントダウン開始
        startCountDown();
    }

    // カウントダウン開始
    private void startCountDown() {
        int countDownMilliSecondTime = CKUtil.getCountDownSecondTime() * 1000;
        // バーコード読取ボタンを無効化
        mBtCamera.setEnabled(false);
        // プログレスバー準備
        mPbCountDown.setMax(countDownMilliSecondTime);
        mPbCountDown.setProgress(0);
        mPbCountDown.setVisibility(View.VISIBLE);
        // カウントダウン開始
        mBarcodeDetectorCountDown = new BarcodeDetectorCountDown(countDownMilliSecondTime, 50);
        mBarcodeDetectorCountDown.start();
    }

    // カウントダウン終了
    private void finishCountDown() {
        // カウントダウン停止
        if (mBarcodeDetectorCountDown != null) {
            mBarcodeDetectorCountDown.cancel();
            mPbCountDown.setProgress(0);
            mPbCountDown.setVisibility(View.INVISIBLE);
            // バーコード読取ボタンを有効化
            mBtCamera.setEnabled(true);
        }
    }

    // カウントダウンクラス
    private class BarcodeDetectorCountDown extends CountDownTimer {

        private long mMaxInterval = 0;
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
            // カウントダウン停止
            finishCountDown();
            // カメラの初期プレビュー開始
            startInitPreview();
            // 最後までバーコード認識できなかった
            String message = CKUtil.getMyString(R.string.message_not_scan_barcode);
            CKUtil.showLongToast(message);
            writeLog(message);
        }
    }

    //フラッシュの状態
    private boolean isFlashOn() {
        if (mCameraSource == null) { return false; }
        return mCameraSource.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH);
    }

    // フラッシュ切り替え
    private void switchFlashMode(boolean isFlashOn) {
        if (mCameraSource == null) { return; }
        if (isFlashOn() == isFlashOn) { return; }

        if (isFlashOn) {
            mIvFlashIcon.setImageResource(R.drawable.ic_flash_on);
            mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            mIvFlashIcon.setImageResource(R.drawable.ic_flash_off);
            mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
    }

    // カメラ開始
    private void setCameraPreviewMode(CameraViewMode viewMode) {

        if (mCameraPreview == null) { return; }

        // 指定されたモードを保存
        mCameraViewMode = viewMode;
        // バーコード読取/カメラボタンを有効化
        mBtCamera.setEnabled(true);

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

            if (mCameraSource == null) {
                // カメラをリリース
                //releaseCameraSource();
                // 常にAutoFocus-On
                CameraSource.Builder builder = new CameraSource.Builder(getContext(), startBarcodeDetection())
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(800, 600)
                        .setRequestedFps(60.0f)
                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                        .setFlashMode(flashMode ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                mCameraSource = builder.build();
                mCameraPreview.start(mCameraSource);
            }
        } catch (SecurityException e) {
            // カメラを解放
            releaseCameraSource();
        } catch (IOException e) {
            releaseCameraSource();
        }
    }

    // 商品名欄関連のイベント定義
    private void callEventRowMemo() {

        // レイヤー
        mLlInfoInput = (LinearLayout) mView.findViewById(R.id.row_edit_item_info_input) ;

        // 新規モードの場合のみ表示
        mLlInfoInput.setVisibility(View.VISIBLE);
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

                // 削除画面を表示
                String dialogTag = "use_list";
                if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                    DialogUseList useList = DialogUseList.newInstance();
                    useList.setTargetFragment(DialogItemSearch.this, 0);

                    //引数設定
                    Bundle args = new Bundle();
                    args.putString(DialogUseList.ARG_SEARCH_WORD, mEtItemNameInput.getText().toString());
                    useList.setArguments(args);
                    useList.setCancelable(false);
                    useList.show(getFragmentManager(), dialogTag);
                }
            }
        });

        // 最初はキーボードを非表示
        CKUtil.hideKeyboad(mView);

        // 商品名
        setTextValueWithEvent("", false);
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

    // カメラを解放
    private void releaseCameraSource() {
        if (mCameraSource != null) {
            mCameraSource.stop();
            mCameraSource.release();
            mCameraSource = null;
        }
        if (mCameraPreview != null) {
            mCameraPreview.stop();
            mCameraPreview.release();
        }
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
                writeLog(barcodeValue);

                // バーコード読取モードでなければ抜ける
                if (mCameraViewMode != CameraViewMode.inBarcode) {
                    return;
                }
                // 前回のバーコードが処理されていなければ抜ける
                if (! mShowBarcodeValue.equals("")) {
                    return;
                }
                // 連続して同じバーコードを取得した場合は処理スキップ
                if (barcodeValue.equals(mShowBarcodeValue)) {
                    return;
                }

                //　新規に認識したバーコードを保存
                mShowBarcodeValue = barcodeValue;
                mParentActivity.runOnUiThread(new Runnable() {
                    public void run() {

                        if (mShowBarcodeValue.equals("")) {
                            return;
                        }

                        // カウントダウン停止
                        finishCountDown();
                        // バーコード準備モードへ
                        setCameraPreviewMode(CameraViewMode.inShowBackground);

                        // 検索する商品が存在するかの事前判定
                        ObjectSearchCondition searchCondition = new ObjectSearchCondition();
                        searchCondition.setBarcode(mShowBarcodeValue);
                        DAItemStock daItemStock = CKDBUtil.getDAItemStock();
                        ArrayList<Map<String, String>> stocks = daItemStock.getEachItemDateCountBySearchCondition(searchCondition
                                , DAItemStock.col_LimitDt + " asc ");

                        boolean isExistItem = false;
                        if (stocks != null) {
                             for (int i = 0; i < stocks.size(); i++) {
                                 String value = stocks.get(i).get(DAItemStock.col_ItemCount);
                                 try {
                                     if (value != null && Integer.parseInt(value) > 0) {
                                         isExistItem = true;
                                         break;
                                     }
                                 } catch (Exception e) {}
                             }
                        }
                        if (! isExistItem) {
                            mShowBarcodeValue = "";
                            CKUtil.showLongToast(CKUtil.getMyString(R.string.message_no_item));
                            return;
                        }

                        // 削除画面を表示
                        String dialogTag = "use_list";
                        if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                            // フラッシュ消灯
                            switchFlashMode(false);

                            DialogUseList useList = DialogUseList.newInstance(DialogItemSearch.this);
                            //useList.setTargetFragment(DialogItemSearch.this, 0);

                            //引数設定
                            Bundle args = new Bundle();
                            args.putString(DialogUseList.ARG_SEARCH_BARCODE, mShowBarcodeValue);

                            // 保存バーコードをクリア
                            mShowBarcodeValue = "";
                            mCameraViewMode = CameraViewMode.inShowBackground;
                            useList.setArguments(args);
                            useList.setCancelable(false);
                            useList.show(getFragmentManager(), dialogTag);
                        }
                    }
                });
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

    // 自画面を閉じる
    public void dismissThisFragment() {
        dismiss();
    }
}

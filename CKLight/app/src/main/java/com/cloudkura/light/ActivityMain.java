package com.cloudkura.light;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

// Google Analytics用
// AppRate


public class ActivityMain extends AppCompatActivity {

    static final int RESULT_SIGN_IN = 9001;

    // オブジェクト
    RelativeLayout mRlRootContainer;
    ImageView mIvNav;
    NavigationView mNavView;
    DrawerLayout mDrawer;
    //LinearLayout mLnDevelopmentSettingContainer;  // Del@GP#25
    ActionBarDrawerToggle mToggle;
    LinearLayout mLlItemSearch;
    TextView mTvSearchItemName;
    TextView mTvSearchBarcode;
    LinearLayout mLlAddBarcode;
    LinearLayout mLlAddWishList;
    LinearLayout mLlDeleteItem;
    View mvFabBackground;
    FloatingActionButton mFabChildShow;
    FloatingActionButton mFabProgressSync;
    CoordinatorLayout mMainLayout;

    boolean mIsCloseActivity = false;
    //int mCountTappLogo = 0;

    // WEB同期中のfabアニメーション
    ObjectAnimator mFabAnimWebSyn;

    // 今日の日付を保存
    String mCurrentDate;

    // 現在のデータの有無
    boolean mIsExistData = false;
    // バーコードを持つデータの有無
    boolean mIsExistBarcodeData = false;

    // 子fabの表示制御
    private enum FabState {
        OPEN,
        CLOSE
    }
    FabState fabState;

    FragmentTab mFragmentTab = null;

    // ログ書き出し
    private void writeLog() {
        CKFB.writeLog(this.getClass().getName());
    }

    /* ---------------------------------------------------------------------- *
     *  画面起動時
     * ---------------------------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        writeLog();

        // Reciever用のIntentServiceを起動
        startService(new Intent(this, CKNotificationService.class));

        // 日付取得
        mCurrentDate = CKUtil.getCurrentDate();

        // AdModの事前ロード
        CKUtil.loadAdRequest();

        // 同期処理実施
        if (! CKUtil.isConnectNetwork()) {
            syncWebDB();
        }

        // CK画像非表示化対応 v2.2.9
        CKDBUtil.setImagesHidden();

        // 不正なストックデータを削除
        CKDBUtil.getDAItemStock().deleteInvalidData();
        // 紐づきの切れた商品画像を削除
        CKUtil.deleteNotEntryImageFiles();

        // 起動日時を更新
        CKDBUtil.getDASettings().updateLastLaunchedDateTime();

        // サマリーを初期表示
        mFragmentTab = replaceToTabFragment(CKUtil.TabPage.TAB_HOME);

        // ToDo: 判定処理を有効化する
        // 設定情報を読み込み、未設定時のみスタート画面を表示
        /*
        if (CKUtil.isFirstLaunch()) {
            // 初期表示の場合のアニメーション
            // TODO: 新規追加の説明アニメーションを追加
        }
        */

        // パーミッション確認
        isAllPermissionsGranted();

        // ユーザセッションを投稿
        CKUtil.postUserTraceLog("");

        // ToDo: AppRateを実装(要望記入画面を追加したいが)
        /*
        // AppRateにより評価誘導
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.app_rate_layout, null);
        AppRate.with(this)
                .setView(view)
                .setInstallDays(0)          // 起動した回数のカウントを開始する日をインストール日基準で指定。デフォルトは 10(日)後。0だとインストール初日
                .setLaunchTimes(3)          // レイティングのダイアログを表示するまでの起動した回数。 デフォルトは10
                .setRemindInterval(2)       // "後で"をクリックしたときのリマインドの間隔。デフォルトは 1(日)
                .setShowLaterButton(true)   // "後で"のボタンを表示するか。デフォルトは true
                .setDebug(false)            // デバッグログを吐き出すか。デフォルトは false
                .setOnClickButtonListener(new OnClickButtonListener() { // ボタンクリックのコールバック
                    @Override
                    public void onClickButton(int which) {

                    }
                })
                .monitor();

        // 条件に合致したら表示
        AppRate.showRateDialogIfMeetsConditions(this);
        */


        // メインコンテナを保存
        mRlRootContainer = (RelativeLayout) findViewById(R.id.fragment_item_list_container);

        // 左ナビゲーションドロワーのイベント定義
        mNavView = (NavigationView) findViewById(R.id.activity_main_left_drawer);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        mToggle = new ActionBarDrawerToggle(
                this, mDrawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if (slideOffset < 0.2) {
                    // NavigationViewの項目へ値を設定
                    setNavigationItemValue();
                }
            }

            // ドロワーが開かれた
            @Override
            public void onDrawerOpened(View drawerView) {
                writeLog();
                // NavigationViewの項目へ値を設定
                setNavigationItemValue();
            }
        };
        mDrawer.setDrawerListener(mToggle);
        mToggle.syncState();

        // ナビゲーションドロワーの表示処理(ボタン)
        mIvNav = (ImageView) findViewById(R.id.activity_main_sub_header_drawer_icon);
        mIvNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawer.openDrawer(mNavView, true);
            }
        });

        // 子fabボタンオブジェクト表示制御用
        mLlItemSearch = (LinearLayout) findViewById(R.id.activity_main_search_layout);
        mTvSearchItemName = (TextView) findViewById(R.id.activity_main_search_item_name);
        mTvSearchBarcode = (TextView) findViewById(R.id.activity_main_search_barcode);
        mLlAddBarcode = (LinearLayout) findViewById(R.id.activity_main_add_barcode_layout);
        mLlAddWishList = (LinearLayout) findViewById(R.id.activity_main_add_wishlist_layout);
        mLlDeleteItem = (LinearLayout) findViewById(R.id.activity_main_delete_item_layout);
        // 初期非表示
        mLlItemSearch.setVisibility(View.GONE);
        mLlAddBarcode.setVisibility(View.GONE);
        mLlAddWishList.setVisibility(View.GONE);
        mLlDeleteItem.setVisibility(View.GONE);

        fabState = FabState.CLOSE;
        mvFabBackground = findViewById(R.id.activity_main_fab_background);

        mvFabBackground.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                writeLog();
                // 子ボタンを表示/非表示切り替え
                switchChildFabVisibility();
            }
        });

        // 子ボタン表示Fab
        mFabChildShow = (FloatingActionButton) findViewById(R.id.activity_action_fab);
        mFabChildShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLog();

                // 子ボタンを表示/非表示切り替え
                switchChildFabVisibility();
            }
        });

        // 商品名検索画面を表示
        mLlItemSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLog();
                // 2018.03.10 商品削除画面を表示するように変更
                //showSearchDialog(DialogItemSearch.SearchMode.ItemName);
                //---
                showSearchDialogWord();
                // 2018.03.10 商品削除画面を表示するように変更
            }
        });

        // バーコード削除画面を表示
        mLlDeleteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLog();
                showSearchDialogBarcode(DialogItemSearch.SearchMode.Barcode);
            }
        });

        // お気に入り追加入力画面を表示
        mLlAddWishList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLog();
                showWishListRegistDialog();
            }
        });

        // バーコード入力画面を表示
        mLlAddBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLog();
                showItemRegistDialog();
            }
        });

        // 告知画面を表示
        showPopupNotice();

        // サービスからのメッセージ受信
        UpdateReceiverFcmListenerService receiverFcmListenerService = new UpdateReceiverFcmListenerService(getRootView());
        IntentFilter filter = new IntentFilter();
        filter.addAction(CKFcmListenerService.class.getName());
        registerReceiver(receiverFcmListenerService, filter);

        // サブヘッダーへの値表示
        setSubHeader();
        if (! mIsExistData) {
            fabOpenNoBackground();
        }

    }

    /*
    @Override
    public void onPause() {
        super.onPause();
        // WEB同期
        syncWebDB();
        // アニメーション停止
        webSyncFabAnimStop();
    }

    @Override
    public void onStop() {
        super.onStop();
        // WEB同期
        syncWebDB();
        // アニメーション停止
        webSyncFabAnimStop();
    }
    */

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.gc();
        // WEB同期
        syncWebDB();
        // アニメーション停止
        webSyncFabAnimStop();
    }

    private View getRootView() {
        return findViewById(R.id.drawer_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        writeLog();

        // 復帰時に画面をリフレッシュ
        refreshRelationInfo();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            writeLog();

            if (! mIsCloseActivity) {

                // 削除確認ダイアログ表示ボタン
                new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
                        .setCancelable(false)
                        .setTitle(R.string.alert_dialog_title_confirm)
                        .setMessage(R.string.message_confirm_quit)
                        .setPositiveButton(
                                R.string.alert_dialog_button_yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whitch) {

                                        finish();

                                        mIsCloseActivity = true;
                                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));

                                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
                                    }
                                })
                        .setNegativeButton(
                                R.string.alert_dialog_button_no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whitch) {

                                        mIsCloseActivity = false;
                                    }
                                })
                        .show();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    // パーミッション確認
    public boolean isAllPermissionsGranted() {

        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        // カメラ・ストレージのパーミッション確認
        String[] permission = new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (CKUtil.getMyContext().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                || CKUtil.getMyContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            this.requestPermissions(permission, CKUtil.PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        writeLog();

        final String ALERT_MESSAGE_REPLACE1 = "[@1]";
        final String ALERT_TITLE = CKUtil.getMyString(R.string.message_permission_alert_title);
        final String ALERT_MESSAGE = CKUtil.getMyString(R.string.message_permission_alert_confirm);
        final String ALERT_MESSAGE_RESET = CKUtil.getMyString(R.string.message_permission_alert_reset);

        // 許可されたかチェック
        for (int i = 0; i < permissions.length; i++) {
            final String permission = permissions[i];
            final int grantResult = grantResults[i];
            String manifestPermission = "";
            String messagePermisssionName = "";

            switch (permission) {
                case Manifest.permission.CAMERA:
                    manifestPermission = Manifest.permission.CAMERA;
                    messagePermisssionName = getString(R.string.permission_name_camera);
                    break;

                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    manifestPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                    messagePermisssionName = getString(R.string.permission_name_strage);
                    break;

                default:
                    break;
            }

            if (!manifestPermission.equals("")) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, manifestPermission)) {
                        // 許可されなかった場合のアクション
                        new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
                                .setCancelable(false)
                                .setTitle(ALERT_TITLE)
                                .setMessage(ALERT_MESSAGE.replace(ALERT_MESSAGE_REPLACE1, messagePermisssionName))
                                .setPositiveButton(
                                        R.string.alert_dialog_button_yes,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int whitch) {
                                            // パーミッション確認
                                            isAllPermissionsGranted();
                                    }
                                })
                                .setNegativeButton(
                                        R.string.alert_dialog_button_no,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int whitch) {
                                            }
                                        })
                                .show();
                    } else {
                        new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
                                .setCancelable(false)
                                .setTitle(ALERT_TITLE)
                                .setMessage(ALERT_MESSAGE_RESET)
                                .setPositiveButton(
                                        R.string.alert_dialog_button_yes,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int whitch) {
                                            openSettings();
                                        }
                                    })
                                .setNegativeButton(
                                        R.string.alert_dialog_button_no,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int whitch) {
                                            }
                                    })
                                .show();
                    }
                    break;
                }
            }
        }
    }

    // アプリ設定を表示
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    // 最終更新日時の表示更新とタップイベント追加
    private void refreshLastSyncDateTimeDisplay(boolean isSyncingNow) {

        LinearLayout llNavWebSyncLayer = (LinearLayout) findViewById(R.id.nav_web_sync_layer);
        if (llNavWebSyncLayer == null) {
            return;
        }

        TextView tvWebSyncSetting = (TextView) findViewById(R.id.nav_head_text_web_sync);
        //ImageView ivUserIcon = (ImageView) findViewById(R.id.nav_head_cloud_user_icon);
        TextView tvUserName = (TextView) findViewById(R.id.nav_head_text_sync_user);
        String webSyncTitle = CKUtil.getMyString(R.string.left_menu_web_sync);

        // 同期中の場合はタップ時のイベントを無効に
        if (isSyncingNow) {
            tvUserName.setText(CKUtil.getMyString(R.string.sync_in_process));
            tvWebSyncSetting.setText(tvWebSyncSetting.getText());
            llNavWebSyncLayer.setOnClickListener(null);
            return;
        }

        /* GP#29で追加 */
        SpannableStringBuilder spannableStringBuilderSyncTitle;
        SpannableStringBuilder spannableStringBuilderUserName;
        if (CKUtil.getUserID().equals("")) {
            // 未認証の場合
            String userName = CKUtil.getMyString(R.string.signin);
            tvUserName.setText(userName);
            //ivUserIcon.setVisibility(View.INVISIBLE);
            // 認証画面呼出し
            spannableStringBuilderSyncTitle = createCallWebSyncSettingFragmentString(webSyncTitle);
            spannableStringBuilderUserName = createCallWebSyncSettingFragmentString(userName);
            llNavWebSyncLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Web同期認証
                    setNavCallWebAuth();
                }
            });
        } else {
            // 認証済みの場合
            String userName = CKUtil.getLastSyncDateTime();
            if (userName.equals(CKUtil.PREF_INIT_WEBSYNC_DATETIME)) {
                userName = tvUserName.getText().toString();
            }
            tvUserName.setText(userName);
            //ivUserIcon.setVisibility(View.VISIBLE);
            //ivUserIcon.setImageBitmap(CKUtil.getUserIcon());
            // タップで同期、同期日時を更新
            spannableStringBuilderSyncTitle = createCallWebSyncString(webSyncTitle);
            spannableStringBuilderUserName = createCallWebSyncString(userName);
            llNavWebSyncLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Web同期認証
                    setNavCallWebSync();
                }
            });
        }
        tvWebSyncSetting.setMovementMethod(LinkMovementMethod.getInstance());
        tvWebSyncSetting.setText(spannableStringBuilderSyncTitle);
        tvUserName.setText(spannableStringBuilderUserName);
    }

    // NavigationView の設定
    public void setNavigationItemValue() {

        // 設定情報を読み込み、画面に設定
        setNavigationItemDisplay();

        // 設定変更（通知設定画面を呼出し）
        SpannableStringBuilder linkLimitDaySetting = createCallLimitDaySettingFragmentString(CKUtil.getMyString(R.string.left_menu_setting));
        TextView tvLimitDaySetting = (TextView) findViewById(R.id.nav_head_text_setting);
        tvLimitDaySetting.setMovementMethod(LinkMovementMethod.getInstance());
        tvLimitDaySetting.setText(linkLimitDaySetting);

        // Web同期日時の表示更新
        refreshLastSyncDateTimeDisplay(false);

        /* 開発者用メニュー(GP#25で削除)
        // 設定変更（その他設定画面を呼出し）
        SpannableStringBuilder linkOtherSetting = createCallOtherSettingFragmentString(CKUtil.getMyString(R.string.left_menu_other_setting));
        TextView tvOtherSetting = (TextView) findViewById(R.id.nav_head_text_other_setting);
        tvOtherSetting.setMovementMethod(method);
        tvOtherSetting.setText(linkOtherSetting);

        // ナビの開発者設定(初期非表示)
        mLnDevelopmentSettingContainer = (LinearLayout) findViewById(R.id.nav_drawer_development_mode);
        mLnDevelopmentSettingContainer.setVisibility(View.GONE);
        */

        /* GP#25で追加 */
        // WEBサイトへのリンクを設定
        SpannableStringBuilder linkWebSite = createUrlString(CKUtil.getMyString(R.string.left_menu_move_to_web_link), CKUtil.getMyString(R.string.web_cloudkura_howtouse));
        TextView txtMoveToWebsitLink = (TextView) findViewById(R.id.nav_head_text_cklink_howtouse);
        txtMoveToWebsitLink.setMovementMethod(LinkMovementMethod.getInstance());
        txtMoveToWebsitLink.setText(linkWebSite);

        // サイトへのリンク作成(copyrightでaboutus)
        TextView txtMoveToAboutUs = (TextView) findViewById(R.id.nav_copyright);
        txtMoveToAboutUs.setMovementMethod(LinkMovementMethod.getInstance());
        txtMoveToAboutUs.setText(createUrlString(CKUtil.getMyString(R.string.left_menu_copy_right), CKUtil.getMyString(R.string.web_cloudkura_about)));

        // ロゴ画像
        //mCountTappLogo = 0;
        ImageView ivNavLogo = (ImageView) findViewById(R.id.nav_drawer_logo);
        ivNavLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // クラウド蔵サイトを表示
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(CKUtil.getMyString(R.string.web_cloudkura_main))));
                /* GP#25 -- 隠し機能を保留
                mCountTappLogo++;
                if (mCountTappLogo > 20) {
                    mLnDevelopmentSettingContainer.setVisibility(View.VISIBLE);
                    mCountTappLogo = 0;
                }
                */
            }
        });

    }

    // Navigationの表示を変更
    public void setNavigationItemDisplay() {

        /*
        // ユーザ情報
        ImageView ivUserIcon = (ImageView) findViewById(R.id.nav_head_cloud_user_icon);
        ivUserIcon.setImageBitmap(CKUtil.getUserIcon());
        */
        // 最終同期日時に変更
        TextView tvUserName = (TextView) findViewById(R.id.nav_head_text_sync_user);
        //tvUserName.setText(CKUtil.getUserName());
        tvUserName.setText(CKUtil.getLastSyncDateTime());

        // 登録値を取得
        DASettings daSettings = CKDBUtil.getDASettings();
        daSettings.getAlertSettingValue();
        boolean isAlertOn = daSettings.getAlertOnOff();
        String alertSpan = String.valueOf(daSettings.getAlertSpan());

        // 通知・通知日
        TextView tvAlertOnOff = (TextView) findViewById(R.id.nav_drawer_text_alert_onoff_value);
        TextView tvAlertSpan = (TextView) findViewById(R.id.nav_drawer_text_alert_span_value);
        // 同期日時
        TextView tvLastWebSync = (TextView) findViewById(R.id.nav_head_text_sync_user);

        String lastLaunchedDateTime = CKUtil.getLastLaunchedDateTime();
        if (lastLaunchedDateTime.equals("")) {
            lastLaunchedDateTime = CKUtil.getMyString(R.string.tap_to_sync);
        }
        tvLastWebSync.setText(lastLaunchedDateTime);

        if (isAlertOn) {
            tvAlertOnOff.setText(R.string.left_menu_do);
            tvAlertSpan.setText(alertSpan);
        } else {
            tvAlertOnOff.setText(R.string.left_menu_not_do);
            tvAlertSpan.setText("---");
        }
    }

    // サブヘッダーの設定
    public void setSubHeader() {

        // 背景色・メッセージの設定
        LinearLayout llStatus = (LinearLayout) findViewById(R.id.fragment_item_list_display_status);
        TextView tvMessage = (TextView) findViewById(R.id.fragment_sub_header_message);
        int nowColor = CKStatus.getStatusInfo(llStatus, tvMessage);
        // 商品タブに表示するデータの存在判定
        mIsExistData = (nowColor != R.color.colorNoData);
        // バーコードを持つデータの存在判定
        mIsExistBarcodeData = mIsExistData && CKDBUtil.getDAItemStock().isExistHasBarcodeItemWithStock();
    }

    // 子fab表示制御
    private void switchChildFabVisibility() {

        if (fabState == FabState.CLOSE) {
            fabOpen();
        } else {
            fabClose();
        }
    }

    // 全fabの表示制御
    public void setFabVisibility(boolean isShow) {

        if (mFabChildShow == null) {
            return;
        }

        if (isShow) {
            mFabChildShow.setVisibility(View.VISIBLE);
        } else {
            mFabChildShow.setVisibility(View.GONE);
        }
    }

    // fabの表示制御
    public void fabOpen() {
        fabOpenBase(true);
    }
    public void fabOpenNoBackground() {
        fabOpenBase(false);
    }
    private void fabOpenBase(boolean displayBackground) {

        // fabサイズ(Dp to Px)
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        int iconWhile = (int) (55 * metrics.density);   // 66 * metrics.density
        int iconHeight = iconWhile;

        PropertyValuesHolder holderAlpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        PropertyValuesHolder holderTransY;
        ObjectAnimator anime;

        int duration = 100;
        int durationInterval = 100;

        // バーコード子fab
        mLlAddBarcode.setEnabled(true);
        mLlAddBarcode.setVisibility(View.VISIBLE);
        iconHeight += iconWhile;
        holderAlpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        holderTransY = PropertyValuesHolder.ofFloat("translationY", 0f, -iconHeight);
        anime = ObjectAnimator.ofPropertyValuesHolder(mLlAddBarcode, holderAlpha, holderTransY);
        duration += durationInterval;
        anime.setDuration(duration);
        anime.start();

        // お気に入り追加子fab
        mLlAddWishList.setEnabled(true);
        mLlAddWishList.setVisibility(View.VISIBLE);
        iconHeight += iconWhile;
        holderTransY = PropertyValuesHolder.ofFloat("translationY", 0f, -iconHeight);
        anime = ObjectAnimator.ofPropertyValuesHolder(mLlAddWishList, holderAlpha, holderTransY);
        duration += durationInterval;
        anime.setDuration(duration);
        anime.start();

        if (mIsExistData) {
            if (mIsExistBarcodeData) {
                // 削除ボタン子fab
                mLlDeleteItem.setEnabled(true);
                mLlDeleteItem.setVisibility(View.VISIBLE);
                iconHeight += iconWhile;
                holderTransY = PropertyValuesHolder.ofFloat("translationY", 0f, -iconHeight);
                anime = ObjectAnimator.ofPropertyValuesHolder(mLlDeleteItem, holderAlpha, holderTransY);
                duration += durationInterval;
                anime.setDuration(duration);
                anime.start();
            }
            // 検索子fab
            mLlItemSearch.setEnabled(true);
            mLlItemSearch.setVisibility(View.VISIBLE);
            holderAlpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
            iconHeight += iconWhile;
            holderTransY = PropertyValuesHolder.ofFloat("translationY", 0f, -iconHeight);
            anime = ObjectAnimator.ofPropertyValuesHolder(mLlItemSearch, holderAlpha, holderTransY);
            duration += durationInterval;
            anime.setDuration(duration);
            anime.start();
        }

        // 表示元fab
        anime = ObjectAnimator.ofFloat(mFabChildShow, "rotation", 360);
        duration += durationInterval;
        anime.setDuration(duration);
        anime.start();

        // 状態変更
        fabState = FabState.OPEN;
        mvFabBackground.setVisibility(displayBackground ? View.VISIBLE : View.GONE);
    }
    private void fabClose() {

        // fabサイズ(Dp to Px)
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        int iconWhile = (int) (55 * metrics.density);   // 66 * metrics.density

        PropertyValuesHolder holderAlpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
        ObjectAnimator anime;
        PropertyValuesHolder holderTransY;

        // お気に入り追加子fab
        holderTransY = PropertyValuesHolder.ofFloat("translationY", -iconWhile * 5, 0f);
        anime = ObjectAnimator.ofPropertyValuesHolder(mLlAddWishList, holderAlpha, holderTransY);
        anime.setDuration(400);
        anime.start();

        // 商品削除子fab
        holderTransY = PropertyValuesHolder.ofFloat("translationY", -iconWhile * 4, 0f);
        anime = ObjectAnimator.ofPropertyValuesHolder(mLlDeleteItem, holderAlpha, holderTransY);
        anime.setDuration(300);
        anime.start();

        // バーコード子fab
        holderTransY = PropertyValuesHolder.ofFloat("translationY", -iconWhile * 3, 0f);
        anime = ObjectAnimator.ofPropertyValuesHolder(mLlAddBarcode, holderAlpha, holderTransY);
        anime.setDuration(200);
        anime.start();

        // 検索子fab
        holderTransY = PropertyValuesHolder.ofFloat("translationY", -iconWhile * 2, 0f);
        anime = ObjectAnimator.ofPropertyValuesHolder(mLlItemSearch, holderAlpha, holderTransY);
        anime.setDuration(100);
        anime.start();

        // 表示元fab
        anime = ObjectAnimator.ofFloat(mFabChildShow, "rotation", -360);
        anime.setDuration(400);
        anime.start();

        // 状態変更
        fabCloseForce();
        mvFabBackground.setVisibility(View.GONE);
        mLlAddBarcode.setEnabled(false);
    }

    public void fabCloseForce() {

        // 状態変更
        fabState = FabState.CLOSE;
        mvFabBackground.setVisibility(View.GONE);
        mLlAddBarcode.setVisibility(View.GONE);
        mLlAddWishList.setVisibility(View.GONE);
        mLlDeleteItem.setVisibility(View.GONE);
        mLlItemSearch.setVisibility(View.GONE);
    }
    // WEB同期中のfabのアニメーション開始
    private void webSyncFabAnimStart() {
        // 表示元fab
        if (mFabAnimWebSyn == null) {
            if (mFabProgressSync == null) {
                mFabProgressSync = (FloatingActionButton) findViewById(R.id.activity_action_fab_sync);
            }
            mFabProgressSync.setVisibility(View.VISIBLE);
            mFabAnimWebSyn = ObjectAnimator.ofFloat(mFabProgressSync, "rotation", 360.0f);
            mFabAnimWebSyn.setDuration(1500);
            mFabAnimWebSyn.setRepeatCount(Animation.INFINITE);
        }
        mFabAnimWebSyn.start();
        // 処理中表示
        refreshLastSyncDateTimeDisplay(true);
    }
    private void webSyncFabAnimStop() {
        // 表示元fab
        if (mFabAnimWebSyn != null) {
            mFabAnimWebSyn.ofFloat(mFabProgressSync, "rotation", 0.0f).start();
            mFabAnimWebSyn.cancel();
            mFabProgressSync.setVisibility(View.GONE);
            mFabAnimWebSyn = null;
        }
        // 処理中表示解除
        refreshLastSyncDateTimeDisplay(false);
    }

    // ClickableSpan処理(通知設定画面呼出し)
    private SpannableStringBuilder createCallLimitDaySettingFragmentString(String showString) {

        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (showString.equals("")) {
            return builder;
        }

        builder.append(showString);
        builder.setSpan(new ClickableSpan() {
            // クリック時の処理
            @Override
            public void onClick(View view) {
                // 左ナビゲーションドロワーを閉じる
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                // 設定画面呼出し
                DialogLimitDaySettings settings = DialogLimitDaySettings.newInstance();
                settings.show(getFragmentManager(),"settings");
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);

                // 下線を非表示
                ds.setUnderlineText(false);
            }
        },
        0, showString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }

    // ClickableSpan処理(同期認証画面呼出し)
    private SpannableStringBuilder createCallWebSyncSettingFragmentString(String showString) {

        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (showString.equals("")) {
            return builder;
        }

        builder.append(showString);
        builder.setSpan(new ClickableSpan() {
                            // クリック時の処理
                            @Override
                            public void onClick(View view) {
                                // Web同期認証
                                setNavCallWebAuth();
                            }

                            @Override
                            public void updateDrawState(TextPaint ds) {
                                super.updateDrawState(ds);

                                // 下線を非表示
                                ds.setUnderlineText(false);
                            }
                        },
                0, showString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }

    private void setNavCallWebAuth() {
        // 左ナビゲーションドロワーを閉じる
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        // Web同期認証呼出し
        Intent intent = new Intent(CKUtil.getMyContext(), ActivityCloudAuth.class);
        startActivityForResult(intent, RESULT_SIGN_IN);
    }

    private void setNavCallWebSync() {
        // 左ナビゲーションドロワーを閉じる
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        // 強制WEB同期実施
        syncWebDBRefreshLocal();
    }

    // ClickableSpan処理(WEB同期処理呼出し)
    private SpannableStringBuilder createCallWebSyncString(String showString) {

        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (showString.equals("")) {
            return builder;
        }

        builder.append(showString);
        builder.setSpan(new ClickableSpan() {
                            // クリック時の処理
                            @Override
                            public void onClick(View view) {
                                // Webデータ同期
                                setNavCallWebSync();
                            }

                            @Override
                            public void updateDrawState(TextPaint ds) {
                                super.updateDrawState(ds);

                                // 下線を非表示
                                ds.setUnderlineText(false);
                            }
                        },
                0, showString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_SIGN_IN && ! CKUtil.getUserID().equals("")) {
            CKUtil.showLongToast(CKUtil.getMyString(R.string.sync_setting_start));
            // 承認コードクリア
            CKUtil.clearApprovalCode();
            // 初回の強制WEB同期実施
            syncWebDBRefreshLocal();
        }
    }

    private SpannableStringBuilder createCallOtherSettingFragmentString(String showString) {

        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (showString.equals("")) {
            return builder;
        }

        builder.append(showString);
        builder.setSpan(new ClickableSpan() {
                // クリック時の処理
                @Override
                public void onClick(View view) {
                    System.gc();

                    // 左ナビゲーションドロワーを閉じる
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        drawer.closeDrawer(GravityCompat.START);
                    }
                    // 設定画面呼出し
                    DialogOtherSettings settings = DialogOtherSettings.newInstance();
                    settings.show(getFragmentManager(),"other_settings");
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);

                    // 下線を非表示
                    ds.setUnderlineText(false);
                }
            },
            0, showString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }

    // ClickableSpan処理(Webブラウザ呼出し)
    private SpannableStringBuilder createUrlString(String showString, final String urlString) {

        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (showString.equals("")) {
            return builder;
        }

        builder.append(showString);
        builder.setSpan(new ClickableSpan() {
            // クリック時の処理
            @Override
            public void onClick(View view) {
                // ブラウザ起動
                Uri uri = Uri.parse(urlString);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);

                // 下線を非表示
                ds.setUnderlineText(false);
            }
        },
        0, showString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }

    // 左ナビゲーションドロワー制御
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    // 商品タブの詳細画面の表示制御
    public void setItemDetailFooterVisibility(boolean isVisible) {
        try {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment != null && fragment.getClass() != null) {
                    if (fragment.getClass().equals(FragmentTab.class)) {
                        ((FragmentTab) fragment).mFragmentItemList.setItemDetailFooterVisibility(isVisible);
                        return;
                    }
                }
            }

        } catch (Exception ex) {
        }
    }

    // 関連情報をリフレッシュ(全タブ)
    public void refreshRelationInfo() {

        try {

            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment != null && fragment.getClass() != null) {
                    if (fragment.getClass().equals(FragmentTab.class)) {
                        ((FragmentTab) fragment).mFragmentItemList.updateDataNotify();
                        ((FragmentTab) fragment).mFragmentGraphList.refreshGraph();
                        ((FragmentTab) fragment).mFragmentNoStockItemList.refreshNoStockItemList();
                        break;
                    }
                }
            }

        } catch (Exception ex) {
        }

        // ヘッダーのストック情報をリフレッシュ
        setSubHeader();

        // アラート発火
        CKStatus.setAlertNotification();
    }

    // 表示する商品の有無を返す。
    public boolean isExistsDisplayItem() {
        return mIsExistData;
    }

    // 関連情報をリフレッシュ(補充リストのみ)
    public void tabRefreshNoStockItemList() {

        try {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment != null && fragment.getClass() != null) {
                    if (fragment.getClass().equals(FragmentTab.class)) {
                        ((FragmentTab) fragment).mFragmentNoStockItemList.refreshNoStockItemList();
                        break;
                    }
                }
            }

        } catch (Exception ex) {
        }
    }

    // Fragment切り替え
    public FragmentTab replaceToTabFragment(CKUtil.TabPage tabPage) {

        System.gc();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
        );

        FragmentTab fragmentTab = new FragmentTab().newInstance();

        Bundle bundle = new Bundle();
        bundle.putSerializable(FragmentTab.ARG_TAB_NO, tabPage);
        fragmentTab.setArguments(bundle);

        transaction.replace(R.id.fragment_tab_container, fragmentTab, fragmentTab.getClass().toString());
        transaction.addToBackStack(null);
        transaction.commit();

        return fragmentTab;
    }

    // 商品名検索画面を表示
    public void showSearchDialogWord() {
        // 子ボタンを強制非表示
        fabCloseForce();

        // 詳細画面を表示
        String dialogTag = "use_list";
        if (getSupportFragmentManager().findFragmentByTag(dialogTag) == null) {

            System.gc();

            // 画面呼出し
            DialogUseList useList = DialogUseList.newInstance();
            useList.setCancelable(false);
            useList.show(getSupportFragmentManager(), dialogTag);
        }
    }

    // バーコード検索画面を表示
    public void showSearchDialogBarcode(DialogItemSearch.SearchMode mode) {
        // 子ボタンを強制非表示
        fabCloseForce();

        // 権限確認
        if (isAllPermissionsGranted()) {

            String dialogTag = "item_search";
            if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                System.gc();

                // 検索画面呼出し
                DialogItemSearch dialogItemSearch = DialogItemSearch.newInstance();
                Bundle arg = new Bundle();
                arg.putSerializable(DialogItemSearch.ARG_SEARCH_MODE, mode);
                dialogItemSearch.setArguments(arg);
                dialogItemSearch.setCancelable(false);
                dialogItemSearch.show(getSupportFragmentManager(),dialogTag);
            }
        }
    }

    // 商品登録・編集画面を表示（バーコード登録）
    private void showItemRegistDialog() {

        // 子ボタンを強制非表示
        fabCloseForce();

        // 権限確認
        if (isAllPermissionsGranted()) {

            String dialogTag = "item_new";
            if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                System.gc();

                // 引数設定
                Bundle arg = new Bundle();
                arg.putString(DialogItemRegist.ARG_IID, "");
                arg.putString(DialogItemRegist.ARG_LIMIT_DATE, "");
                arg.putBoolean(DialogItemRegist.ARG_IS_WISHLIST, false);

                // 新規バーコード
                DialogItemRegist dialogItemRegist = DialogItemRegist.newInstance();
                dialogItemRegist.setArguments(arg);
                dialogItemRegist.setCancelable(false);
                dialogItemRegist.show(getSupportFragmentManager(), dialogTag);
            }
        }
    }

    // 商品登録・編集画面を表示（お気に入り登録）
    private void showWishListRegistDialog() {

        // 子ボタンを強制非表示
        fabCloseForce();

        // 権限確認
        if (isAllPermissionsGranted()) {

            String dialogTag = "item_wishlist";
            if (getFragmentManager().findFragmentByTag(dialogTag) == null) {

                System.gc();

                // 引数設定
                Bundle arg = new Bundle();
                arg.putString(DialogItemRegist.ARG_IID, "");
                arg.putString(DialogItemRegist.ARG_LIMIT_DATE, "");
                arg.putBoolean(DialogItemRegist.ARG_IS_WISHLIST, true);

                // お気に入り登録
                DialogItemRegist dialogItemRegist = DialogItemRegist.newInstance();
                dialogItemRegist.setArguments(arg);
                dialogItemRegist.setCancelable(false);
                dialogItemRegist.show(getSupportFragmentManager(), dialogTag);
            }
        }
    }

    // 告知画面を表示
    public void showPopupNotice() {

        // 設定に応じて告知画面の表示切替
        if (CKUtil.isShowNoticePopup()) {

            String dialogTag = "popup_notice";
            if (getFragmentManager().findFragmentByTag(dialogTag) == null) {
                System.gc();

                DialogNotice dialogNotice = DialogNotice.newInstance();
                dialogNotice.setCancelable(false);
                dialogNotice.show(getSupportFragmentManager(), dialogTag);
            }
        }
    }

    public void syncWebDB() {
        syncWebDB(false);
    }
    // Web同期処理の呼出し
    // isForce:
    //      true:   強制更新
    //      false:  自分が更新データを持つ場合のみ更新
    public void syncWebDB(boolean isForce) {

        if (CKUtil.getUserID().equals("")) {
            // アニメーション停止
            webSyncFabAnimStop();
            return;
        }

        // ネットワーク接続確認
        if (! CKUtil.isConnectNetwork()) {
            // アニメーション停止
            webSyncFabAnimStop();
            return;
        }

        // アニメーション開始
        webSyncFabAnimStart();
        // 同期処理開始
        AsyncPostUserDbData postUserDbData = new AsyncPostUserDbData(isForce);
        postUserDbData.setOnCallBack(new AsyncPostUserDbData.CallbackTask() {
            @Override
            public void CallBack(String result) {
                super.CallBack(result);
                if (result.equals(CKWebService._WEBSYNC_NO_SYNC_DATA)) {
                    // メッセージ表示
                    //showSnackBar(CKUtil.getMyString(R.string.sync_result_no_sync_data), 3000);
                } else if (result.equals(CKWebService._WEBSYNC_UPDATED)) {
                    // 最終同期日時を更新
                    CKUtil.setLastSyncDateTime();
                    // ナビの同期時刻をリフレッシュ
                    refreshLastSyncDateTimeDisplay(false);
                    // 画面リフレッシュ
                    refreshRelationInfo();
                    // fabを閉じる
                    fabCloseForce();
                    //CKUtil.showLongToast(CKUtil.getMyString(R.string.message_add_stock_list));
                }
                // アニメーション停止
                webSyncFabAnimStop();

            }
        });
        postUserDbData.execute();
    }

    // Web同期処理の呼出し(強制同期。ローカルをWEBDBの値で上書き)
    public void syncWebDBRefreshLocal() {

        // ネットワーク接続確認
        if (! CKUtil.isConnectNetwork()) {
            // アニメーション停止
            webSyncFabAnimStop();
            CKUtil.showLongToast(CKUtil.getMyString(R.string.message_not_connect_network));
            return;
        }

        // アニメーション開始
        webSyncFabAnimStart();

        // ローカルデータPOST
        AsyncPostUserDbData postUserDbData = new AsyncPostUserDbData(true);
        postUserDbData.setOnCallBack(new AsyncPostUserDbData.CallbackTask() {
            @Override
            public void CallBack(String result) {
                super.CallBack(result);

                // ----
                /*
                if (result.equals(CKWebService._WEBSYNC_NO_SYNC_DATA)) {
                    // メッセージ表示
                    showSnackBar(CKUtil.getMyString(R.string.sync_result_no_sync_data), 3000);
                }
                */
                // 最終同期日時を更新
                CKUtil.setLastSyncDateTime();

                // アニメーション停止
                webSyncFabAnimStop();
                // 画面リフレッシュ
                refreshRelationInfo();
                // fabを閉じる
                fabCloseForce();
                //CKUtil.showLongToast(CKUtil.getMyString(R.string.message_add_stock_list));
                // ----

                /*
                // WEBデータGET
                AsyncGetWebDbData getWebDbData = new AsyncGetWebDbData(true);
                getWebDbData.setOnCallBack(new AsyncGetWebDbData.CallbackTask() {
                    @Override
                    public void CallBack(String result) {
                        super.CallBack(result);

                        // ----
                        // ここに↑の処理があった。
                        // ----
                    }
                });
                getWebDbData.execute();
                */
            }
        });
        postUserDbData.execute();
    }

    // Web同期処理の呼出し(強制同期。ローカルをWEBDBの値で上書き)
    public void getWebDBtoLocal() {

        // ネットワーク接続確認
        if (! CKUtil.isConnectNetwork()) {
            // アニメーション停止
            webSyncFabAnimStop();
            CKUtil.showLongToast(CKUtil.getMyString(R.string.message_not_connect_network));
            return;
        }

        // アニメーション開始
        webSyncFabAnimStart();

        // WEBデータGET
        AsyncGetWebDbData getWebDbData = new AsyncGetWebDbData(true);
        getWebDbData.setOnCallBack(new AsyncGetWebDbData.CallbackTask() {
            @Override
            public void CallBack(String result) {
                super.CallBack(result);
                if (result.equals(CKWebService._WEBSYNC_NO_SYNC_DATA)) {
                    // メッセージ表示
                    //showSnackBar(CKUtil.getMyString(R.string.sync_result_no_sync_data), 3000);
                }
                // 最終同期日時を更新
                CKUtil.setLastSyncDateTime();

                // アニメーション停止
                webSyncFabAnimStop();
                // 画面リフレッシュ
                refreshRelationInfo();
                // fabを閉じる
                fabCloseForce();
                //CKUtil.showLongToast(CKUtil.getMyString(R.string.message_add_stock_list));
            }
        });
        getWebDbData.execute();
    }

    // Service からのメッセージ受信時イベント
    protected class UpdateReceiverFcmListenerService extends BroadcastReceiver {
        View mView;
        public UpdateReceiverFcmListenerService(View view) {
            mView = view;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            // アニメーション停止
            webSyncFabAnimStop();
            // 同期終了のメッセージ表示
            Bundle bundle = intent.getExtras();
            int snackbarDuration = 3000;
            if (bundle.getString(CKFcmListenerService.EXTRA_MODE).equals(CKFcmListenerService.MODE_SYNC_INVALID)) {
                // データ不整合が生じている場合、全件再同期
                CKUtil.clearLastSyncDateTime();
                syncWebDB(true);
            } else {
                // 他端末での更新の場合は画面リフレッシュ
                if (bundle.getString(CKFcmListenerService.EXTRA_MODE).equals(CKFcmListenerService.MODE_SYNC_OTHER)) {
                    snackbarDuration = 3000;
                    // 同期開始(WEBデータを取得)
                    getWebDBtoLocal();
                    // 画面リフレッシュ
                    refreshRelationInfo();
                    // fabを閉じる
                    fabCloseForce();
                    // メッセージ表示
                    showSnackBar(bundle.getString(CKFcmListenerService.EXTRA_MESSAGE), snackbarDuration);
                }
            }
        }
    }

    public void showSnackBar(String msg, int duration) {
        // fabとsnackbarを重ねない
        if (mMainLayout == null) {
            mMainLayout = (CoordinatorLayout) findViewById(R.id.main_content);
        }
        Snackbar snackbar = Snackbar.make(mMainLayout, msg, Snackbar.LENGTH_LONG);
        snackbar.getView().setBackgroundColor(CKUtil.getMyColor(R.color.colorNoInformation));
        snackbar.getView().setMinimumHeight((int)(snackbar.getView().getHeight() * 0.6));
        snackbar.setDuration(duration);
        snackbar.show();
    }
}

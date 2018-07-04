package com.cloudkura.light;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

// ユーティリティクラス
public class CKUtil {

    // 日付の基準
    static final int IN_1_MONTH = 30;
    static final int IN_1_YEAR = 365;

    // Permission Request
    static final int PERMISSION_REQUEST = 1001;

    // ユーザデータPOST結果
    static final int _POST_RETURN_CODE = 0;
    static final int _POST_RETURN_VALUE = 1;

    // タブページ番号定義
    enum TabPage {
        TAB_HOME,
        TAB_ITEMLIST,
        TAB_NOSTOCK
    }

    // 消費期限列挙体
    enum SelectLimitDay {
        ALL,
        OVER,
        IN_1MONTH,
        IN_1YEAR,
        OTHER,
        ALERT_SPAN
    }

    // Preference関連定数
    static String PREF_USER_INFO = "user_info";
    static String PREF_SESSION_ID = "session_id";
    static String PREF_AUTH_ID = "auth_id";
    static String PREF_MAIL_ADDRESS = "mail_address";
    static String PREF_USER_NAME = "user_name";
    static String PREF_USER_ICON = "user_icon";
    static String PREF_LAST_WEBSYNC_DATETIME = "websync_lastdatetime";
    static String PREF_APPROVAL_CODE = "approval_code";

    static String PREF_INIT_WEBSYNC_DATETIME = "1900/01/01 00:00:00";

    // AdMod表示用
    static AdRequest mAdRequest;

    // 初期メニューの表示フラグ
    private static boolean mIsDisplayStartMenu = true;
    static boolean isFirstLaunch() {

        if (mIsDisplayStartMenu && !CKDBUtil.getDASettings().isFirstLaunch()) {
            mIsDisplayStartMenu = false;
        }
        return mIsDisplayStartMenu;
    }

    // 起動時のお知らせ表示フラグ
    static boolean isShowNoticePopup() {

        return CKDBUtil.getDASettings().isShowNoticePopup();
    }

    // 最終アクセス日時を取得
    static String getLastLaunchedDateTime() {
        String dateTime = CKDBUtil.getDASettings().getLastLaunchedDateTime();
        if (dateTime == null || dateTime.equals("")) {
            dateTime = "--/--/-- --:--:--";
        }
        return dateTime;
    }

    // AdModロード
    static void loadAdRequest() {
        mAdRequest = new AdRequest.Builder().build();
    }
    static AdRequest getmAdRequest() {
        if (mAdRequest == null) {
            loadAdRequest();
        }
        return mAdRequest;
    }

    static String replaceHtmlFormat(String value) {
        String buff = value.replace("&amp;", "&");
        buff = buff.replace("&amp;", "&");
        buff = buff.replace("&quot;", "\"");
        buff = buff.replace("&lt;", "<");
        buff = buff.replace("&gt;", ">");
        buff = buff.replace("&nbsp;", " ");
        buff = buff.replace("&copy;", "©");
        return buff;
    }

    // Context取得
    static Context getMyContext() {
        return AppController.getInstance().getApplicationContext();
    }

    // Toastを表示(Long)
    public static void showLongToast(String msg) {
        Toast.makeText(getMyContext(), msg, Toast.LENGTH_LONG).show();
    }

    // null → 指定された初期値 変換
    static Object nullTo(Object val, Object defaultValue){
        return (val == null ? defaultValue : val);
    }

    // ネットワーク接続判定
    static boolean isConnectNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getMyContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    //SDカードへ保存するファイル名を生成
    private static final String _PICT_EXT = ".png";
    //private static final String _PICT_EXT = ".jpg";
    private static String getSaveImageName() {
        return System.currentTimeMillis() + _PICT_EXT;
    }

    // 保存先ディレクトリ名を合成
    @NonNull
    private static String getFileSaveStrageDir() {

        // CK画像の非表示化対応
        //File dataDir = new File(Environment.getExternalStorageDirectory(),  getMyContext().getPackageName());
        // SDカード/パッケージ名のディレクトリ名を作成
        File dataDir = new File(replaceSaveDirHiddenDir());
        if (! dataDir.exists()) {
            if (! dataDir.mkdir()) {
                showLongToast(getMyString(R.string.message_failed_create_directory));
            }
        }
        return dataDir.getPath();
    }

    // 保存先ディレクトリ名を変更
    public static String getImageSaveDirOld() {
        return new File(Environment.getExternalStorageDirectory(), getMyContext().getPackageName()).getPath();
    }
    public static String getImageSaveDirNew() {
        return new File(Environment.getExternalStorageDirectory(), "." + getMyContext().getPackageName()).getPath();
    }
    @NonNull
    public static String replaceSaveDirHiddenDir() {

        // 移行先フォルダが存在しなければフォルダ作成して全ファイルコピー
        // 移行元フォルダと含まれるファイルは削除

        //SDカード/パッケージ名のディレクトリ名を作成
        File dataDirOld = new File(getImageSaveDirOld() + "/");
        File dataDirNew = new File(getImageSaveDirNew() + "/");
        if (! dataDirNew.exists()) {
            try {
                dataDirNew.mkdir();
            } catch (Exception ex) {
                showLongToast(getMyString(R.string.message_failed_create_directory) + "(new dir)");
                return "";
            }
        }
        if (dataDirOld.exists() && dataDirNew.exists()) {
            try {
                // ファイルコピー処理
                File[] files = dataDirOld.listFiles();
                for(int i = 0; i < files.length; i++) {
                    File copyToFile = new File(dataDirNew.getPath() + "/" + files[i].getName());
                    boolean result = files[i].renameTo(copyToFile);
                    if (! result) {
                        showLongToast("failed to copy files");
                    }
                }
                dataDirOld.delete();
            } catch (Exception ex) {
                showLongToast(getMyString(R.string.message_failed_create_directory) + "(new dir)");
                return "";
            }
        }
        // 画像ファイルのリンクのゴミを削除
        ArrayList<Map<String, String>>  oldFiles = CKDBUtil.getDAItem().getAllItemImagePath();
        Context context = CKUtil.getMyContext();
        String replacePath = dataDirNew.getPath();
        String searchPath = dataDirOld.getPath();
        for(int i = 0; i < oldFiles.size(); i++) {
            try {
                context.getContentResolver().delete(
                        MediaStore.Files.getContentUri("external"),
                        MediaStore.Files.FileColumns.DATA + "=?",
                        new String[]{ oldFiles.get(i).get(DAItem.col_ImagePath).replace(replacePath, searchPath) }
                );
            } catch (Exception ex) {
            }
        }

        return dataDirNew.getPath();
    }

    // SessionIdをプリファレンスに保存
    private static void setPreferenceSessionId(String sessionId) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_SESSION_ID, sessionId).apply();
    }
    // SessionIdの取得
    private static String getPreferenceSessionId() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        return pref.getString(PREF_SESSION_ID, "");
    }

    // 認証Idをプリファレンスに保存
    private static void setPreferenceAuthId(String authId) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_AUTH_ID, authId).apply();
    }
    // 認証Idの取得
    private static String getPreferenceAuthId() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        return pref.getString(PREF_AUTH_ID, "");
    }

    // メールアドレスをプリファレンスに保存
    private static void setPreferenceMailAddress(String address) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_MAIL_ADDRESS, address).apply();
    }
    // メールアドレスの取得
    private static String getPreferenceMailAddress() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        return pref.getString(PREF_MAIL_ADDRESS, "");
    }

    // ユーザ名をプリファレンスに保存
    private static void setPreferenceUserName(String userName) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_USER_NAME, userName).apply();
    }
    // ユーザ名の取得
    private static String getPreferenceUserName() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        return pref.getString(PREF_USER_NAME, "");
    }

    // ユーザアイコンをプリファレンスに保存
    public static void setPreferenceUserIconBase64(String userIconBase64) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_USER_ICON, userIconBase64).apply();
    }
    // ユーザアイコンの取得
    private static String getPreferenceUserIconBase64() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        return pref.getString(PREF_USER_ICON, "");
    }

    // 最終同期日時をプリファレンスに保存
    private static void setPreferenceLastWebSyncDateTime(String lastWebSyncDateTime) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_LAST_WEBSYNC_DATETIME, lastWebSyncDateTime).apply();
    }
    // 最終同期日時を取得
    private static String getPreferenceLastWebSyncDateTime() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        return pref.getString(PREF_LAST_WEBSYNC_DATETIME, PREF_INIT_WEBSYNC_DATETIME);
    }

    // 承認コードをプリファレンスに保存
    private static void setPreferenceApprovalCode(String approvalCode) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_APPROVAL_CODE, approvalCode).apply();
    }
    // 承認コードをクリア
    private static void clearPreferenceApprovalCode() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().remove(PREF_APPROVAL_CODE).apply();
    }
    // 承認コードの取得
    private static String getPreferenceApprovalCode() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        // 初期値は reg_user_dbdata.php の INIT_WORD の定義値と合わせること。
        return pref.getString(PREF_APPROVAL_CODE, "[ck#Init]");
    }
    // 承認コードの保存
    public static void saveApprovalCode(String approvalCode) {
        if (! approvalCode.equals("")) {
            setPreferenceApprovalCode(approvalCode);
        }
    }
    // 承認コードをクリア
    public static void clearApprovalCode() {
        clearPreferenceApprovalCode();
    }

    // 操作ログをプリファレンスに保存
    private static void setPreferenceTraceLog(String event) {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_APPROVAL_CODE, CKUtil.createLastUpdateDateTime()).apply();
    }
    // 操作ログの取得
    private static String getPreferenceTraceLog() {
        SharedPreferences pref = getMyContext().getSharedPreferences(PREF_USER_INFO, Context.MODE_PRIVATE);
        // 初期値は reg_user_dbdata.php の INIT_WORD の定義値と合わせること。
        return pref.getString(PREF_APPROVAL_CODE, "[ck#Init]");
    }

    // 操作ログの保存
    public static void saveTraceLog(String logValue) {
        if (! logValue.equals("")) {
            setPreferenceTraceLog(logValue);
        }
    }
    // 操作ログの取得
    public static void getTraceLog() {
        getPreferenceTraceLog();
    }

    // 操作ログをPOSTする
    static void postUserTraceLog(String log) {
        new AsyncRegUserSession().execute(log);
    }

    // Web同期用の認証情報をJSON形式で取得
    static JSONArray getJsonArrayAuthInfo() {
        ArrayList<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("user_id", getUserID());
        map.put("device_id", getDeviceID());
        map.put("approval_code", getPreferenceApprovalCode());
        map.put("last_websync_dt", CKUtil.getLastSyncDateTime());
        list.add(map);
        return getJsonArray(list);
    }

    // Web同期用のユーザ情報をJSON形式で取得
    static JSONArray getJsonArrayUserInfo() {
        ArrayList<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("user_id", getUserID());
        map.put("user_name", getUserName());
        map.put("user_icon_base64", getUserIconBase64());
        map.put("device_id", getDeviceID());
        map.put("device_name", Build.PRODUCT);
        map.put("device_bland", Build.BRAND);
        map.put("sdk_version", String.valueOf(Build.VERSION.SDK_INT));
        map.put("mail_address", getMailAddress());
        map.put("locale_lang", Locale.getDefault().toString());
        map.put("locale_country", Locale.getDefault().getCountry());
        map.put("approval_code", getPreferenceApprovalCode());
        map.put("token", getFbInstanceToken());
        map.put("last_websync_dt", CKUtil.getLastSyncDateTime());
        list.add(map);
        return getJsonArray(list);
    }

    // Itemの差分データをJSON形式で取得
    static JSONArray getJsonArray(ArrayList<Map<String, String>> list) {
        JSONArray jsonArray = new JSONArray();
        try {
            if (list != null) {
                for (Map<String, String> row: list) {
                    JSONObject jsonObject = new JSONObject();
                    for (Map.Entry<String, String> entry: row.entrySet()) {
                        // 一行分
                        jsonObject.put(entry.getKey(), entry.getValue());
                    }
                    // 取得行全部
                    jsonArray.put(jsonObject);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    // 保存先パスを合成(画像ファイル)
    static String getSavePicturePath() {
        return getFileSaveStrageDir() + "/" + getSaveImageName();
    }

    //SDカードへの保存処理(画像ファイル)
    static String savePictureSD(byte[] data) throws Exception {
        String filePath = getSavePicturePath();
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(data);
        fos.close();
        return filePath;
    }

    // DAItemにレコードがない画像を削除
    static void deleteNotEntryImageFiles() {

        // 画像のパスリストを取得
        File[] files = new File(getFileSaveStrageDir()).listFiles();
        if (files != null) {
            for (File fl : files) {
                String imagePath = fl.getPath();
                // パスで検索してレコードがなければそのファイルを削除
                if (! imagePath.equals("") && ! CKDBUtil.getDAItem().isExistByImagePath(imagePath)) {
                    deletePictureFile(imagePath);
                }
            }
        }
    }

    // 画像を削除
    static void deletePictureFile(String imageFilePath) {
        if (imageFilePath.equals("")) {
            return;
        }
        try {
            File file = new File(imageFilePath);
            if (file.exists()) {
                if (! file.delete()) {
                    showLongToast(getMyString(R.string.message_failed_delete_image));
                }
            }
        } catch (Exception e) {
            //showLongToast(getMyString(R.string.failed_delete_image) + "\n" + e.getMessage().toString());
        }
    }

    // 画像を保存(サイズ調整指定)
    static String saveWebImageToSD(Bitmap bitmapData, String imageFileName) {

        //CK画像非表示化対応
        //File dataDir = new File(Environment.getExternalStorageDirectory(),  getMyContext().getPackageName());
        //SDカード/パッケージ名のディレクトリ名を作成
        File dataDir = new File(replaceSaveDirHiddenDir());

        try {

            if (!dataDir.exists()) {
                if (! dataDir.mkdir()) {
                    showLongToast(getMyString(R.string.message_failed_create_directory));
                }
            }
            // 既存同名ファイルを削除
            deletePictureFile(imageFileName);

            // データ保存
            FileOutputStream fos = new FileOutputStream(dataDir + "/" + imageFileName);
            bitmapData.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            bitmapData.recycle();

        } catch (Exception e) {
            //showLongToast(e.getMessage().toString());
        }

        return dataDir + "/" + imageFileName;
    }

    // 画像を保存(サイズ調整指定)
    static String savePictureSD(Bitmap bitmapData, String imageFileName, boolean adjustSize) {

        //CK画像非表示化対応
        //File dataDir = new File(Environment.getExternalStorageDirectory(),  getMyContext().getPackageName());
        //SDカード/パッケージ名のディレクトリ名を作成
        File dataDir = new File(replaceSaveDirHiddenDir());

        try {

            if (!dataDir.exists()) {
                if (! dataDir.mkdir()) {
                    showLongToast(getMyString(R.string.message_failed_create_directory));
                }
            }
            // 既存同名ファイルを削除
            deletePictureFile(imageFileName);

            // 高さ、幅の長い方に調整
            if (adjustSize) {
                float baseLen;
                float sizeLen;
                float x, y;
                if (bitmapData.getHeight() >= bitmapData.getWidth()) {
                    baseLen = bitmapData.getWidth();
                    sizeLen = bitmapData.getHeight();
                    x = (sizeLen - baseLen) / 2;
                    y = 0;
                } else {
                    baseLen = bitmapData.getHeight();
                    sizeLen = bitmapData.getWidth();
                    x = 0;
                    y = (sizeLen - baseLen) / 2;
                }
                Bitmap newBitmap = Bitmap.createBitmap((int)sizeLen, (int)sizeLen, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(newBitmap);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(bitmapData, x, y, null);
                bitmapData = newBitmap;
            }

            // データ保存
            FileOutputStream fos = new FileOutputStream(dataDir + "/" + imageFileName);
            bitmapData.compress(Bitmap.CompressFormat.PNG, 100, fos);
            //bitmapData.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            bitmapData.recycle();
            bitmapData = null;

        } catch (Exception e) {
            //showLongToast(e.getMessage().toString());
        }

        return dataDir + "/" + imageFileName;
    }

    // 画像を保存(サイズ調整あり)
    static String savePictureSD(Bitmap bitmapData, String imageFileName) {
        return savePictureSD(bitmapData, imageFileName, true);
    }

    // 画像を上書き
    static boolean overWritePictureSD(Bitmap data, String imageFilePath) {

        // パスは必須
        if (imageFilePath.equals("")) {
            return false;
        }

        try {

            // 既存同名ファイルを削除
            File file = new File(imageFilePath);
            if (file.exists()) {
                if (! file.delete()) {
                    showLongToast(getMyString(R.string.message_failed_delete_image));
                }
            }

            // データ保存 TODO: 画像保存
            FileOutputStream fos = new FileOutputStream(imageFilePath);
            data.compress(Bitmap.CompressFormat.PNG, 100, fos);
            //data.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

        } catch (Exception e) {
            showLongToast(getMyString(R.string.message_failed_save_image) + "\n" + e.getMessage());
            return false;
        }

        return true;
    }

    // ObjectIdを生成
    static String createObjectId() {
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss", Locale.ENGLISH);
        String key = df.format(new Date(System.currentTimeMillis()));
        String uuid = UUID.randomUUID().toString();

        return key + "_" + uuid;
    }

    // LastUpdateDateTime文字列を生成
    static String createLastUpdateDateTime() {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
        return df.format(new Date(System.currentTimeMillis()));
    }

    // 共通書式の年月日を取得
    static String getFormatDate(int year, int month, int day) {
        if (isLocalJapan()){
            return String.format(Locale.ENGLISH, "%04d/%02d/%02d", year, month, day);
        } else {
            return String.format(Locale.ENGLISH, "%02d/%02d/%04d", month, day, year);
        }
    }

    // 共通書式の年月日を取得
    static String getFormatDate(Date date) {
        if (date == null) { return ""; }

        DateFormat df = new SimpleDateFormat(getLocalizedDateFormat(), Locale.ENGLISH);
        return df.format(date);
    }

    // ロケール取得
    static boolean isLocalJapan() {
        return Locale.getDefault().getLanguage().equals("ja");
    }

    // 日付書式を取得(ロケール判定)
    static private String getLocalizedDateFormat() {
        return isLocalJapan() ? "yyyy/MM/dd" : "MM/dd/yyyy";
    }

    // 日付書式を取得(内部用)
    static private String getSystemDateFormat() {
        return "yyyy/MM/dd";
    }

    static String getSystemFormatDate(Date date) {
        if (date == null) { return ""; }

        DateFormat df = new SimpleDateFormat(getSystemDateFormat(), Locale.ENGLISH);
        return df.format(date);
    }

    // 本日を取得
    static String getCurrentDate() {
        return getFormatDate(new Date(System.currentTimeMillis()));
    }

    // 本日を取得
    static String getSystemFormatCurrentDate() {
        return getSystemFormatDate(new Date(System.currentTimeMillis()));
    }

    // 基準日に加算した日付を取得
    static String calcDate(String baseDate, int addDays) {

        // 基準日が未指定の場合は本日に置き換え
        if (nullTo(baseDate, "").equals("")) { baseDate = getSystemFormatCurrentDate(); }

        Date baseDt = isSystemFormatDate(baseDate);
        if (baseDt == null) {
            baseDt = isDate(baseDate);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseDt);
        calendar.add(Calendar.DAY_OF_MONTH, addDays);

        DateFormat df = new SimpleDateFormat(getSystemDateFormat(), Locale.ENGLISH);
        return df.format(calendar.getTime());
    }

    static Date isDate(String value) {
        if (value == null || value.equals("")) { return null; }

        DateFormat df = new SimpleDateFormat(getLocalizedDateFormat(), Locale.ENGLISH);
        //DateFormat df = DateFormat.getDateInstance();
        df.setLenient(false);

        try {
            return df.parse(value);

        } catch (Exception e) {
            return null;
        }
    }

    static Date isSystemFormatDate(String value) {
        if (value == null || value.equals("")) { return null; }

        DateFormat df = new SimpleDateFormat(getSystemDateFormat(), Locale.ENGLISH);
        df.setLenient(false);

        try {
            Date dt = df.parse(value);
            df = null;
            return dt;

        } catch (Exception e) {
            return null;
        }
    }

    static String numFormat(int value) {
        return NumberFormat.getNumberInstance().format(value);
    }

    // 指定URLをブラウザ起動で表示
    static void openURL(String url) {

        // 指定URLを開く
        if (url != null && ! url.equals("")) {
            Uri uri = Uri.parse(url);
            Intent uriIntent = new Intent(Intent.ACTION_VIEW,uri);
            uriIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getMyContext().startActivity(uriIntent);
        } else {
            showLongToast(getMyString(R.string.message_not_specify_url));
        }
    }

    // NumberPickerの範囲指定
    static NumberPicker setNumberPickerRangeTargetDays(NumberPicker numberPicker) {
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(IN_1_MONTH);
        return numberPicker;
    }

    static void setNumberPickerDividerNone(NumberPicker numberPicker) {

        Class<?> numberPickerClass = null;
        try{
            numberPickerClass = Class.forName("android.widget.NumberPicker");
        } catch (ClassNotFoundException e) {
            //
        }

        if(numberPickerClass != null){
            java.lang.reflect.Field selectionDivider;
            try {
                selectionDivider = numberPickerClass.getDeclaredField("mSelectionDivider");
                selectionDivider.setAccessible(true);
                Drawable drawable = AppController.getInstance().getResources().getDrawable(R.drawable.ic_no_divider);
                selectionDivider.set(numberPicker, drawable);
            } catch (Exception e) {
                //
            }
        }
    }

    /* -------------------------------------------------------------------------------------------------------------------------
    *   画像関連処理
    *  ------------------------------------------------------------------------------------------------------------------------- */

    // 指定したファイルのBitmapを取得する
    static Bitmap getBitmap(String filePath) {

        Bitmap bitmap = null;
        if (filePath != null) {
            try {
                FileInputStream file = new FileInputStream(filePath);
                bitmap = BitmapFactory.decodeStream(file);
                file.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    // カメラから取得したバイトデータをトリミング
    static Bitmap trimCameraByteArray(byte[] data, int padding) {

        Bitmap bitmap = null;

        try {
            // バイトデータをBMP変換
            bitmap = decodeSampledBitmapFromByteArray(data, 300, 300);
            //bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

            // サイズ取得
            int imageWidth = bitmap.getWidth();
            int imageHeight = bitmap.getHeight();

            // サイズ判定（大きい場合は指定サイズまでリサイズ）
            final float _limitSize = (float)(512 * 1024);
            int byteSize = bitmap.getByteCount();
            if (_limitSize < byteSize) {
                float scale = (float)Math.sqrt(_limitSize / (float)byteSize);
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                Bitmap bitmapTemp = bitmap;
                bitmap = Bitmap.createBitmap(bitmapTemp, 0, 0, imageWidth, imageHeight, matrix, true);
                bitmapTemp.recycle();
                bitmapTemp = null;
                matrix = null;
                // リサイズ後に再取得
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();
            }

            // 画像が横長なら回転
            if (imageWidth > imageHeight) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap bitmapTemp = bitmap;
                bitmap = Bitmap.createBitmap(bitmapTemp, 0, 0, imageWidth, imageHeight, matrix, true);
                matrix = null;
                bitmapTemp.recycle();
                bitmapTemp = null;
                imageWidth = imageHeight;
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, imageWidth - padding, imageWidth - padding, null, true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    /*
    // カメラから取得した画像をトリミング
    static Bitmap trimCameraBitmap(String targetFilePath, int padding) {

        Bitmap bitmap = null;

        if (targetFilePath != null) {
            try {
                // 指定されたファイルを開く
                FileInputStream file = new FileInputStream(targetFilePath);

                bitmap = BitmapFactory.decodeStream(file);
                Matrix matrix = new Matrix();

                // 画像が横長なら回転
                int imageWidth = bitmap.getWidth();
                int imageHeight = bitmap.getHeight();
                if (imageWidth > imageHeight) {
                    matrix.postRotate(90);
                    Bitmap bitmapTemp = bitmap;
                    bitmap = Bitmap.createBitmap(bitmapTemp, 0, 0, imageWidth, imageHeight, matrix, true);
                    bitmapTemp.recycle();
                    bitmapTemp = null;
                    imageWidth = imageHeight;
                }
                matrix = null;

                Bitmap bitmapTemp = bitmap;
                bitmap = Bitmap.createBitmap(bitmapTemp, 0, 0, imageWidth - padding, imageWidth - padding, null, true);
                bitmapTemp.recycle();
                bitmapTemp = null;

                // ファイル・バッファを閉じる
                file.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }
    */

    // bitmapを64bitエンコード
    public static String encodeTobase64(Bitmap image)
    {
        if (image == null) {
            return "";
        }
        Bitmap immagex=image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.NO_WRAP);
        return imageEncoded;
    }

    // bitmap64bitエンコードをbitmapにデコード
    public static Bitmap decodeBase64(String input)
    {
        if (input.equals("")) {
            return null;
        }
        Bitmap bitmap = null;
        try {
            byte[] decodedByte = Base64.decode(input, 0);
            bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
        } catch (Exception ex) {
        }
        return bitmap;
    }

    // 以下、引用
    // http://y-anz-m.blogspot.jp/2012/08/android.html
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // 画像の元サイズ
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width < height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }
        return inSampleSize;
    }


    static Bitmap decodeSampledBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {

        // inJustDecodeBounds=true で画像のサイズをチェック
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // inSampleSize を計算
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // inSampleSize をセットしてデコード
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }


    static Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
        if (filePath == null || filePath.equals("")) {
            return null;
        }

        // inJustDecodeBounds=true で画像のサイズをチェック
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // inSampleSize を計算
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // inSampleSize をセットしてデコード
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, options);
    }

    // キーボードを表示
    static void showKeyboad(View v) {

        if (v == null) {return;}

        InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    // キーボードを隠す
    static void hideKeyboad(View v) {

        if (v == null) {return;}

        InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    // リソースから文字を取得
    static String getMyString(int resId) {
        return AppController.getInstance().getString(resId);
    }
    // リソースからDrawableを取得
    static Drawable getMyDrawable(int resId) {
        return AppController.getInstance().getResources().getDrawable(resId);
    }
    // リソースからColorを取得
    static int getMyColor(int resId) {
        return AppController.getInstance().getResources().getColor(resId);
    }

    // 残日数を算出して設定する
    @DrawableRes
    static int calcRemainDays(String limitDate, TextView tvLimitDate, TextView tvRemainDaysLabel, TextView tvRemainDays) {

        int rid = -1;

        if (tvRemainDaysLabel == null || tvRemainDays == null) {
            return rid;
        }

        // 期限と残日数計算
        String remainDays = "";
        if (nullTo(limitDate, "").equals("")) {

            tvRemainDaysLabel.setText(R.string.no_item);

            // 期限未設定
            int color = getMyColor(R.color.colorButtonTextNormal);
            //int color = ContextCompat.getColor(getMyContext(), R.color.colorExclamation);
            tvRemainDaysLabel.setTextColor(color);

            rid = R.color.colorButtonTextNormal;
            //rid = R.color.colorExclamation;

        } else {

            // ロケールが日本でない場合でyyyy/mm/dd形式の日付の場合はmm/dd/yyyyに変換する。
            // DBからの取得値の場合が該当
            if (isSystemFormatDate(limitDate) != null) {
                limitDate = getFormatDate(isSystemFormatDate(limitDate));
            }

            // 期限表示
            Date limitDt = isDate(limitDate);
            if (! limitDate.equals("")) {
                tvLimitDate.setText(getFormatDate(limitDt));

                // 残日数計算と表示
                long dateTimeLimit = limitDt.getTime();
                long dateTimeToday = isDate(getCurrentDate()).getTime();
                long diffDays = (dateTimeLimit - dateTimeToday) / (1000 * 60 * 60 * 24);

                if (diffDays >= 0) {
                    // 期限が未来
                    remainDays = String.format(getMyString(R.string.days_display), diffDays);
                    tvRemainDaysLabel.setText(getMyString(R.string.remain_days_display_label));

                    if (diffDays <= IN_1_MONTH) {
                        int color = ContextCompat.getColor(getMyContext(), R.color.colorAlertDark);
                        tvRemainDaysLabel.setTextColor(color);
                        tvRemainDays.setTextColor(color);
                        rid = R.color.colorAlert;

                    } else if (diffDays <= IN_1_YEAR) {
                        int color = ContextCompat.getColor(getMyContext(), R.color.colorInfoDark);
                        tvRemainDaysLabel.setTextColor(color);
                        tvRemainDays.setTextColor(color);
                        rid = R.color.colorInfo;

                    } else {
                        int color = ContextCompat.getColor(getMyContext(), R.color.colorNormal);
                        tvRemainDaysLabel.setTextColor(color);
                        tvRemainDays.setTextColor(color);
                        rid = R.color.colorNoInformation;
                    }

                } else {
                    // 期限が過去
                    remainDays = String.format(getMyString(R.string.days_display), diffDays );
                    //remainDays = String.format(getMyString(R.string.days_display), diffDays * -1);
                    tvRemainDaysLabel.setText(getMyString(R.string.passed_days_display_label));

                    int color = ContextCompat.getColor(getMyContext(),R.color.colorExclamationDark);
                    tvRemainDaysLabel.setTextColor(color);
                    tvRemainDays.setTextColor(color);
                    rid = R.color.colorExclamation;
                }

            }
        }
        tvRemainDays.setText(remainDays);

        return rid;
    }

    // 指定URLからText情報を取得
    static String getHttpText(String postUrl) {
        return getHttpText(postUrl, "UTF-8", false);
    }
    static String getHttpTextGoogle(String postUrl) {
        return getHttpText(postUrl, "SJIS", false);
    }
    static String getHttpTextGooglePcMode(String postUrl) {
        return getHttpText(postUrl, "UTF-8", true);
    }
    static String getHttpText(String postUrl, String responseEncode, boolean isPcMode) {

        HttpURLConnection con = null;
        URL url = null;
        InputStream in = null;
        StringBuilder sb = new StringBuilder();
        String st;

        try {
            // URLへ接続
            url = new URL(postUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(1000);
            if (isPcMode) {
                con.setRequestProperty("Accept-Encoding", "gzip");
                con.setRequestProperty("User-Agent","Mozilla/5.0");
                //con.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36");
            }
            //con.connect();

            // レスポンスを保存
            if (isPcMode && con.getContentEncoding().equals("gzip")) {
                in = new GZIPInputStream(con.getInputStream());
            } else {
                in = con.getInputStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(in, responseEncode));
            while((st = br.readLine()) != null) {
                sb.append(st);
            }
            in.close();

        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(con != null) {
                con.disconnect();
            }
        }

        return sb.toString();
    }

    // 指定URLへ送信する
    static String[] putHttpText(String postUrl, String postData) {

        HttpURLConnection con = null;
        URL url;
        String[] returnValue = new String[2];
        StringBuilder sb = new StringBuilder();
        String st;

        try {
            // URLへ接続
            url = new URL(postUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(1000);
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            // データを書き込む準備
            con.setDoOutput(true);

            OutputStream output = con.getOutputStream();
            output.write(postData.getBytes("UTF-8"));
            output.flush();
            try {
                output.close();
            } catch(Exception e) {
                e.printStackTrace();
            }

            // レスポンスを受け取る
            final int status = con.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {

                // レスポンスを保存
                InputStream in = con.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                while((st = br.readLine()) != null) {
                    sb.append(st);
                }
                try {
                    in.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }

                // レスポンスを受け取る処理等
                returnValue[_POST_RETURN_CODE] = CKWebService._WEBSYNC_HTTP_OK;
                returnValue[_POST_RETURN_VALUE] = sb.toString();
            }
            else{
                returnValue[_POST_RETURN_CODE] = CKWebService._WEBSYNC_HTTP_NG;
                returnValue[_POST_RETURN_VALUE] = String.valueOf(status);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if(con != null) {
                con.disconnect();
            }
        }

        return returnValue;
    }

    // 指定URLからJSON情報を取得
    static JSONObject getJSONObject(String httpText) {

        JSONObject item = null;
        try {
            Object json = new JSONTokener(httpText).nextValue();
            if (json instanceof JSONArray) {
                item = ((JSONArray) json).getJSONObject(0);
            } else if (json instanceof JSONObject) {
                item = (JSONObject)json;
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return item;
    }

    // 指定URLからJSON情報を取得
    static ArrayList<String> getJSONArray(String postUrl) {

        ArrayList<String> words = new ArrayList<>();

        try {
            String httpText = getHttpText(postUrl);
            JSONArray json = new JSONArray(httpText);
            try {
                for (int i = 0; i < json.length(); i++) {
                    words.add(json.getJSONObject(i).getString("memo"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return words;
    }

    // 指定URLから画像を取得
    static Bitmap downloadImage(String uri) {

        HttpURLConnection con = null;
        InputStream is = null;
        Bitmap bit = null;

        try {
            // URLの作成
            URL url = new URL(uri);
            // 接続用HttpURLConnectionオブジェクト作成
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            // 接続
            con.connect();
            switch (con.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    is = con.getInputStream();
                    bit = BitmapFactory.decodeStream(is);
                    is.close();
                    return bit;

                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    //
                }
            }
        }

        return bit;
    }

    // バーコード読取のカウントダウン時間を取得
    static int getCountDownSecondTime() {
        int countDownSecondTime = 10;   // デフォルトを10秒とする。
        String timeValue = getMyString(R.string.barcode_detect_count_down_second_time);
        try {
            countDownSecondTime = Integer.valueOf(timeValue);
        } catch (Exception ex) {
            //
        }
        return countDownSecondTime;
    }

    // 正規表現で文字を抜き出し
    static String getMatchString(String regexString, String textString) {

        if (regexString == null ||regexString.equals("") ||
                textString == null || textString.equals("")) {
            return "";
        }

        Pattern pattern = Pattern.compile(regexString);
        Matcher matcher = pattern.matcher(textString);

        if (! matcher.find()) {
            return "";
        } else {
            return matcher.group(0);
        }
    }

    // デバイスIDを取得
    static String getDeviceID() {
        String sessionId = getPreferenceSessionId();
        if (sessionId.equals("")) {
            sessionId = UUID.randomUUID().toString();
            setPreferenceSessionId(sessionId);
        }
        return sessionId;
    }

    // ユーザ情報を設定
    static void setUserInfo(FirebaseUser user) {
        if (user == null) {
            setPreferenceAuthId("");
            setPreferenceMailAddress("");
            setPreferenceUserName("");
            setPreferenceUserIconBase64("");
        } else {
            setPreferenceAuthId(user.getUid());
            setPreferenceMailAddress(user.getEmail());
            setPreferenceUserName(user.getDisplayName());
            // アイコン取得とbase64変換して保存
            setPreferenceUserIconBase64("");    // 一度初期化
            String iconUrl = "https://" + user.getPhotoUrl().getHost() + user.getPhotoUrl().getPath();

            if (user != null && !iconUrl.equals("")) {

                // アイコン画像を非同期で取得しPreferenceに保存
                new AsyncGetUserIcon().execute(iconUrl);
            }

        }
    }
    // ユーザIDを取得
    static String getUserID() {
        return getPreferenceAuthId();
    }
    // ユーザ名を取得
    static String getUserName() {
        return getPreferenceUserName();
    }
    // ユーザアイコンを取得
    static Bitmap getUserIcon() {
        Bitmap userIcon = decodeBase64(getPreferenceUserIconBase64());
        if (userIcon == null) {
            userIcon = BitmapFactory.decodeResource(AppController.getInstance().getResources(), R.mipmap.logo_for_app);
        }
        return userIcon;
    }
    static String getUserIconBase64() {
         return getPreferenceUserIconBase64();
    }
    // メールアドレスを取得
    static String getMailAddress() {
        return getPreferenceMailAddress();
    }

    // 最終同期日時を設定
    static void setLastSyncDateTime() {
        setPreferenceLastWebSyncDateTime(CKUtil.createLastUpdateDateTime());
    }
    // 最終同期日時をクリア
    static void clearLastSyncDateTime() {
        setPreferenceLastWebSyncDateTime(PREF_INIT_WEBSYNC_DATETIME);
    }
    // 最終同期日時を取得
    static String getLastSyncDateTime() {
        return getPreferenceLastWebSyncDateTime();
    }

    // FirebaseInstanceTokenの取得
    static String getFbInstanceToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }

    // タップ時のアニメーション定義
    static View.OnTouchListener doOnTouchAnimation() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                final View targetView = v;

                // アニメーションの時間を設定
                ValueAnimator anim = ValueAnimator.ofFloat(60f, 100f);
                anim.setDuration(300);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {

                        float ratio = (float)animation.getAnimatedValue() / 100f;
                        targetView.setAlpha(ratio);
                    }
                });
                anim.start();
                return false;
            }
        };
    }

    // ローカル情報の更新をWEBへ通知
    static void postToWebAboutLocalInfo() {
        // 同期処理(投げっぱなし)
        AsyncPostUserDbData postUserDbData = new AsyncPostUserDbData();
        postUserDbData.execute();
    }
}

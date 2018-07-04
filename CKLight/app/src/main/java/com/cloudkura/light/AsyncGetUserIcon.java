package com.cloudkura.light;


import android.graphics.Bitmap;
import android.os.AsyncTask;

// ユーザアイコン取得処理(Webサービス呼出し)
public class AsyncGetUserIcon extends AsyncTask<String, Integer, String> {

    private CallbackTask callbackTask;

    // コンストラクタ
    public AsyncGetUserIcon() {
    }

    @Override
    protected String doInBackground(String... params) {

        if (params != null) {
            Bitmap iconBitmap = CKUtil.downloadImage(params[0]);
            if (iconBitmap != null) {
                CKUtil.setPreferenceUserIconBase64(CKUtil.encodeTobase64(iconBitmap));
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {

        if (result != null) {
            // コールバック定義
            callbackTask.CallBack(result);
        }
    }

    // 非同期処理終了後のイベントを作成
    public void setOnCallBack(CallbackTask callBack) {
        callbackTask = callBack;
    }

    // コールバックタスク
    public static class CallbackTask {
        public void CallBack(String result) {

        }
    }
}


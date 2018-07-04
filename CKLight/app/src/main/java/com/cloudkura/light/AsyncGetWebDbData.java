package com.cloudkura.light;


import android.os.AsyncTask;

// Webサーバから情報を取得(Webサービス呼出し)
public class AsyncGetWebDbData extends AsyncTask<String, Integer, String> {

    private boolean mIsForceSync = false;
    private CallbackTask callbackTask;

    // コンストラクタ
    public AsyncGetWebDbData() {}
    // コンストラクタ
    public AsyncGetWebDbData(boolean isForceSync) {
        mIsForceSync = isForceSync;
    }

    @Override
    protected String doInBackground(String... params) {

        // 認証済みならWEBデータ同期
        String returnValue = "";
        if (! CKUtil.getUserID().equals("")) {
            // 同期処理実施
            returnValue = CKWebService.getWebDBData(mIsForceSync);
        }

        return returnValue;
    }

    @Override
    protected void onPostExecute(String result) {

        if (result != null && callbackTask != null) {
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


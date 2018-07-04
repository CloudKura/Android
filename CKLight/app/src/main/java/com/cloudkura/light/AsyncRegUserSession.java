package com.cloudkura.light;


import android.os.AsyncTask;

import java.util.ArrayList;

// ユーザセッション登録処理(Webサービス呼出し)
public class AsyncRegUserSession extends AsyncTask<String, Integer, String> {

    private CallbackTask callbackTask;

    // コンストラクタ
    public AsyncRegUserSession() {
    }

    @Override
    protected String doInBackground(String... params) {

        if (params == null) {
            // ユーザセッションを投稿
            CKWebService.postUserInfo();
        } else {
            // ユーザ操作ログを投稿
            CKWebService.postUserTraceLog(params[0]);
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


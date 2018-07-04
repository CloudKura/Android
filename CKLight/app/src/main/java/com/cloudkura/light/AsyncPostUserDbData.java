package com.cloudkura.light;


import android.os.AsyncTask;

// ローカルDBをWebサーバへPOST(Webサービス呼出し)
public class AsyncPostUserDbData extends AsyncTask<String, Integer, String> {

    private boolean mIsForceSync = false;
    private CallbackTask callbackTask;

    // コンストラクタ
    public AsyncPostUserDbData() {}
    // コンストラクタ
    public AsyncPostUserDbData(boolean isForceSync) {
        mIsForceSync = isForceSync;
    }

    @Override
    protected String doInBackground(String... params) {

        // ToDO: デバッグ用に承認コードをクリア
        //CKUtil.clearApprovalCode();

        // 認証済みならDBデータ同期
        String returnValue = "";
        if (! CKUtil.getUserID().equals("")) {

            // 同期処理実施
            returnValue = CKWebService.postUserDBData(mIsForceSync);
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


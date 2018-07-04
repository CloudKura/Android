package com.cloudkura.light;


import android.os.AsyncTask;

import com.cloudkura.light.CKUtil;
import com.cloudkura.light.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

// 類語検索処理(Webサービス呼出し)
public class AsyncGetSynonymWords extends AsyncTask<String, Integer, ArrayList<String>> {

    private CallbackTask callbackTask;
    private String mSearchWord = "";

    // コンストラクタ
    public AsyncGetSynonymWords(String searchWord) {
        mSearchWord = searchWord;
    }

    @Override
    protected ArrayList<String> doInBackground(String... params) {

        // Cloud Kura Webマスタから類語リストを取得
        String ckUrl = CKUtil.getMyString(R.string.webapi_root_url);
        String postUrl = ckUrl + CKUtil.getMyString(R.string.webapi_get_synonym) + "?memo=" + mSearchWord;

        // 類語リスト取得
        ArrayList<String> words = CKUtil.getJSONArray(postUrl);

        return words;
    }

    @Override
    protected void onPostExecute(ArrayList<String> result) {

        // コールバック定義
        callbackTask.CallBack(result);
    }

    // 非同期処理終了後のイベントを作成
    public void setOnCallBack(CallbackTask callBack) {
        callbackTask = callBack;
    }

    // コールバックタスク
    public static class CallbackTask {
        public void CallBack(ArrayList<String> result) {

        }
    }
}


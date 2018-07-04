package com.cloudkura.light;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/* ToDo: 2018.03.10 Del
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
*/

public class ActivityNotificationResult extends AppCompatActivity {

    /* ToDo: 2018.03.10 Del
    // AdMod用
    private InterstitialAd mInterstitialAd;
    */

    // ログ書き出し
    private void writeLog() {
        CKFB.writeLog(this.getClass().getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writeLog();

        // 画面デザインはスプラッシュ画面を利用
        setContentView(R.layout.activity_notification_result);
        //setContentView(R.layout.activity_splash);

        // 詳細画面を表示
        String dialogTag = "use_list";
        if (getSupportFragmentManager().findFragmentByTag(dialogTag) == null) {
            DialogUseList useList = DialogUseList.newInstance();
            //引数設定
            Bundle args = new Bundle();
            args.putString(DialogUseList.ARG_SEARCH_CONDITION, CKUtil.getMyString(R.string.limit_day_alert));
            useList.setArguments(args);
            useList.setCancelable(false);
            useList.show(getSupportFragmentManager(), dialogTag);
        }
    }

    // DialogFragmentからの戻り取得用
    public void onDissMissReturn() {
        writeLog();

        // ActivityMainの起動
        Intent intent = new Intent(this, ActivityMain.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        /* ToDo: 2018.03.10 Del
        // 全画面広告表示用
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(CKUtil.getMyString(R.string.interstitial_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            // 取得した広告を表示
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                mInterstitialAd.show();
            }

        });
        */

        // 画面を閉じる
        finish();
    }

}

package com.cloudkura.light;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.google.firebase.iid.FirebaseInstanceId;

public class ActivitySplash extends Activity {

    private final static int SPLASH_TIME = 400;
    private static final String TAG = ActivitySplash.class.getSimpleName();

    // ログ書き出し
    private void writeLog() {
        CKFB.writeLog(this.getClass().getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        writeLog();

        // ToDo: getFbInstanceToken
        Log.d(TAG, "Refreshed token: " + CKUtil.getFbInstanceToken());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ActivitySplash.this, ActivityMain.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // バックキー無効。
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}

package com.cloudkura.light;

import android.app.Application;
//import com.deploygate.sdk.DeployGate;
//import com.squareup.leakcanary.LeakCanary;

// Contextをアプリで使いまわすためのシングルトン
// manifest.xml <application> に以下を追加
//        android:name="AppController"
public class AppController extends Application {
    private static AppController sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // LeakCanary
        //LeakCanary.install(this);

        // Deploygate SDKの読み込み
        //DeployGate.install(this);

    }

    public static AppController getInstance() {
        return sInstance;
    }
}

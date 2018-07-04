package com.cloudkura.light;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class CKNotificationService extends Service {

    // 今日の日付を保存
    String mCurrentDate;
    BroadcastReceiver mReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 日付変更を検知するためのRecieverを作成
                if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                    String currentDate = CKUtil.getCurrentDate();
                    if (! currentDate.equals(mCurrentDate)) {
                        // アラート通知
                        mCurrentDate = currentDate;
                        CKStatus.setAlertNotification();
                    }
                }
            }
        };

        // インテントフィルタの設定
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mReceiver, filter);

        // 日付取得
        mCurrentDate = CKUtil.getCurrentDate();

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
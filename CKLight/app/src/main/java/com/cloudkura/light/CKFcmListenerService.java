package com.cloudkura.light;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class CKFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = CKFcmListenerService.class.getSimpleName();
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_SYNC_SELF = "self";
    public static final String MODE_SYNC_OTHER = "other";
    public static final String MODE_SYNC_INVALID = "invalid";

    @Override
    public void onMessageReceived(RemoteMessage message){
        String from = message.getFrom();
        Map data = message.getData();

        // 同期元フラグ値
        // php側 putNotification()の引数値と合わせる
        final String _UPDATE_SELF = "0";
        final String _UPDATE_OTHER = "1";
        final String _UPDATE_INVALID = "9";

        Log.d(TAG, "from:" + from);
        Log.d(TAG, "data:" + data.toString());

        Intent intent = new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0 , intent, PendingIntent.FLAG_ONE_SHOT);

        // 0: 自端末で同期/1:他端末で同期/9:不整合
        String noticeFlg = data.get("flg").toString();
        String serverMessage = data.get("msg").toString();

        // ActivityMainでメッセージ受信してアクション
        Intent broadcast = new Intent();
        broadcast.putExtra(EXTRA_MESSAGE, serverMessage);
        if (noticeFlg.equals(_UPDATE_SELF)) {
            broadcast.putExtra(EXTRA_MODE, MODE_SYNC_SELF);
        } else if (noticeFlg.equals(_UPDATE_OTHER)){
            broadcast.putExtra(EXTRA_MODE, MODE_SYNC_OTHER);
        } else if (noticeFlg.equals(_UPDATE_INVALID)){
            broadcast.putExtra(EXTRA_MODE, MODE_SYNC_INVALID);
        } else {
            return;
        }
        broadcast.setAction(CKFcmListenerService.class.getName());
        CKUtil.getMyContext().sendBroadcast(broadcast);
    }

}

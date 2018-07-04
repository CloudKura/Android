package com.cloudkura.light;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

class CKStatus {

    private final static int _NOTIFY_ID = 0;

    // 在庫状況を取得
    static int getStatusInfo(LinearLayout llStatus, TextView tvMessage) {

        int color = R.color.colorNoData;
        String message = "";

        DAItemStock daItemStock = CKDBUtil.getDAItemStock();
        ArrayList<Map<String, String>> listItems = daItemStock.getEachItemDateBySearchCondition(new ObjectSearchCondition(), "");

        if (listItems == null || listItems.size() == 0) {
            // ストック品なし
            color = R.color.colorNoData;
            message = message + (message.equals("") ? "" : "\n\n") + CKUtil.getMyString(R.string.status_explain_no_data);
        } else {

            ObjectSearchCondition searchCondition = new ObjectSearchCondition();
            searchCondition.setSearchWord("");
            searchCondition.setSelectedLimitDay(CKUtil.SelectLimitDay.OVER);
            if (CKDBUtil.getDAItemStock().getEachItemBySearchCondition(searchCondition, "").size() > 0) {
                // 消費期限期限切れあり
                color = R.color.colorExclamation;
                message = message + (message.equals("") ? "" : "\n\n") + CKUtil.getMyString(R.string.status_explain_passed_limit_day_item);
            } else {
                searchCondition.setSelectedLimitDay(CKUtil.SelectLimitDay.IN_1MONTH);
                if (CKDBUtil.getDAItemStock().getEachItemBySearchCondition(searchCondition, "").size() > 0) {
                    // 消費期限1か月以内
                    color = R.color.colorAlert;
                    message = message + (message.equals("") ? "" : "\n\n") + CKUtil.getMyString(R.string.status_explain_in_half_year_limit);
                }
            }
        }
        if (message.equals("")){
            // アクション不要
            color = R.color.colorInfo;
            message = CKUtil.getMyString(R.string.status_explain_satisfiction);
        }

        if (llStatus != null) {
            llStatus.setBackgroundColor(CKUtil.getMyColor(color));
        }
        if (tvMessage != null) {
            tvMessage.setText(message);
        }

        return color;
    }

    // アラート通知
    static void setAlertNotification() {

        // 通知設定情報を取得
        DASettings daSettings = CKDBUtil.getDASettings();

        // アラート通知判定
        if (! daSettings.getAlertOnOff()) { return; }

        // 期間判定
        int alertSpan = daSettings.getAlertSpan();
        DAItemStock daItemStock = CKDBUtil.getDAItemStock();

        // 期限チェック
        String today = CKUtil.getSystemFormatCurrentDate();

        int dataCountLimitPass = daItemStock.getItemCountByLimitDate("", CKUtil.calcDate(today, -1));
        int dataCountLimitToday = daItemStock.getItemCountByLimitDate(today, today);
        int dataCountLimitAlert = daItemStock.getItemCountByLimitDate(CKUtil.calcDate(today, 1), CKUtil.calcDate(today, alertSpan));

        // 表示メッセージ
        String msg = "";
        String submsg = "";
        String itemUnit = CKUtil.getMyString(R.string.notify_item_unit);
        if (dataCountLimitPass > 0){
            msg = CKUtil.getMyString(R.string.notify_over_limit_day) + String.valueOf(dataCountLimitPass) + itemUnit;
        }
        if (dataCountLimitToday > 0){
            submsg = CKUtil.getMyString(R.string.notify_today) + String.valueOf(dataCountLimitToday) + itemUnit;
        }
        if (dataCountLimitAlert > 0){
            if (! submsg.equals("")) { submsg += "　"; }
            if (CKUtil.isLocalJapan()) {
                submsg = submsg + String.valueOf(alertSpan) + CKUtil.getMyString(R.string.notify_in_days) + String.valueOf(dataCountLimitAlert) + itemUnit;
            } else {
                submsg = submsg + String.format(CKUtil.getMyString(R.string.notify_in_days), alertSpan) + String.valueOf(dataCountLimitAlert) + itemUnit;
            }
        }
        if (! submsg.equals("")) {
            submsg = CKUtil.getMyString(R.string.notify_in_future_limit) + submsg;
        }

        // 通知メッセージ
        Context context = CKUtil.getMyContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (msg.equals("") && submsg.equals("")) {
            // 通知を削除
            manager.cancel(_NOTIFY_ID);

        } else {

            // 通知優先度
            int alertPriority;
            if (dataCountLimitPass > 0){
                alertPriority = 2;
            } else if (dataCountLimitToday > 0){
                alertPriority = 1;
            } else if (dataCountLimitAlert > 0){
                alertPriority = 0;
            } else {
                alertPriority = -2;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

            // 通知ドロワータップ時の挙動
            Intent intent = new Intent(context, ActivityNotificationResult.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
            PendingIntent resultIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // スタックに積む
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            taskStackBuilder.addParentStack(ActivityNotificationResult.class);
            taskStackBuilder.addNextIntent(intent);

            // 通知の属性設定
            builder.setSmallIcon(R.drawable.menu_logo);
            builder.setColor(CKUtil.getMyColor(R.color.colorAccent));
            builder.setContentTitle(CKUtil.getMyString(R.string.notification_app_title));
            if (msg.equals("")) {
                if (! submsg.equals("")) {
                    builder.setContentText(submsg);
                }
            } else {
                builder.setContentText(CKUtil.getMyString(R.string.notification_app_message) + msg);
                if (! submsg.equals("")) {
                    builder.setSubText(submsg);
                }
            }
            builder.setPriority(alertPriority);
            builder.setAutoCancel(true);
            builder.setContentIntent(resultIntent);

            // 通知を表示
            Notification notification = builder.build();
            manager.notify(_NOTIFY_ID, notification);
        }
    }

}

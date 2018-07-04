package com.cloudkura.light;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CKAppLifeCycleLog extends BroadcastReceiver {
    public CKAppLifeCycleLog () {}

    @Override
    public void onReceive(Context context, Intent intent) {

        //CKUtil.showLongToast(intent.getAction());

        // インストール・更新・アンインストールのログ取得
        // http://sakebook.hatenablog.com/entry/2014/11/09/091929
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)
                && !intent.getExtras().getBoolean(Intent.EXTRA_REPLACING)) {
            CKUtil.postUserTraceLog("install");

        }else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)
                && intent.getExtras().getBoolean(Intent.EXTRA_REPLACING)) {
            if (intent.getExtras().getBoolean(Intent.EXTRA_DATA_REMOVED)) {
                CKUtil.postUserTraceLog("uninstall for update");
            } else {
                CKUtil.postUserTraceLog("update");
            }
        }else if(intent.getAction().equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
            CKUtil.postUserTraceLog("uninstall");
        }
    }
}

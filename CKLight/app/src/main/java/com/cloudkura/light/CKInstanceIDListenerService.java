package com.cloudkura.light;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class CKInstanceIDListenerService extends FirebaseInstanceIdService {

    private static final String TAG = CKInstanceIDListenerService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        //ここで取得したInstanceIDをサーバー管理者に伝える

        // ToDo: getFbInstanceToken
        Log.d(TAG, "Refreshed token: " + CKUtil.getFbInstanceToken());
    }
}

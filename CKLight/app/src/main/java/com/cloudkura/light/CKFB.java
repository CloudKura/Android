package com.cloudkura.light;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

public class CKFB {

    // Google Analytics用
    static private FirebaseAnalytics mFirebaseAnalytics;

    static public void writeLog(String itemId) {
        writeLog(itemId, Thread.currentThread().getStackTrace()[4].getMethodName(), "");
    }

    static public void writeLog(String itemId, String value) {
        writeLog(itemId, Thread.currentThread().getStackTrace()[4].getMethodName(), value);
    }

    static public void writeLog(String itemId, String itemName, String value) {

        // CKTraceLog
        Log.d(itemId + ":" + itemName, value);
        CKUtil.postUserTraceLog(itemId + ":" + itemName);

        // Google Analytics用
        if (mFirebaseAnalytics == null) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(CKUtil.getMyContext());
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, itemName);
        bundle.putString(FirebaseAnalytics.Param.VALUE, value);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }
}

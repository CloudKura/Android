package com.cloudkura.light;

import android.database.sqlite.SQLiteDatabase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class DASettings extends DABase
        implements Serializable
{

    // テーブル名定義
    static final String tableName = "Settings";
    // 列名定義
    static final String col_Id = "UerId";
    static final String col_AlertSpan = "AlertSpan";
    static final String col_AlertOnOff = "AlertOnOff";

    // 設定値
    private static boolean mAlertOn = true;
    private static int mAlertSpan = 0;

    // キー値
    static final String key_LastLaunchedDateTime = "0";
    static final String key_LimitDayAlertSetting = "1";
    static final String key_IsDisplayCheckedItem = "2";
    static final String key_IsShowNoticeDialog = "3";
    static final String key_ItemListDisplayOrderByItem = "4";
    static final String key_ItemListDisplayLimitDate = "5";

    // 商品リスト追加済みチェックの値
    static final String val_DisplayCheckedItemOff = "0";
    static final String val_DisplayCheckedItemOn = "1";

    // コンストラクタ
    DASettings() {
        super();

        // 列定義
        Map<String, String> colDefs = new HashMap <String, String>() {
            {
                put(col_Id, "text");
                put(col_AlertSpan, "integer");
                put(col_AlertOnOff, "integer"); // 0: off, 1: on
                put(col_IsHidden, "integer not null default 0");
                put(col_CreDt, "text not null default (DATETIME( 'now', 'localtime' ))");
                put(col_UpdDt, "text not null default (DATETIME( 'now', 'localtime' ))");
            }
        };

        // キー列定義(Key名のみ指定)
        Map <String, String> primaryKeyCols = new HashMap <String, String>() {
            {
                put(col_Id, "");
            }
        };

        // 親クラスへ必要情報を渡す。
        setTableName(tableName);
        setColDefs(colDefs);
        setPrimaryKeyDef(primaryKeyCols);

        // 履歴を取得するか
        setIsTakeHistory(false);
    }

    // 指定キーの存在判定
    private boolean isExistKeyRecord(String key) {

        ArrayList<String> whereList = new ArrayList<>();
        whereList.add(key);
        whereList.add("0");
        ArrayList<Map<String, String>> listItems = super.getItemListView(col_Id + " = ? and " + col_IsHidden + " = ? ", whereList.toArray(new String[whereList.size()]), "");
        whereList.clear();
        return ! (listItems == null || listItems.size() == 0);
    }

    // 初回表示モードかを判定
    boolean isFirstLaunch() {
        // 設定情報を取得できなければ初回起動と判定
        return isExistKeyRecord(key_LimitDayAlertSetting);
    }

    // 設定情報を取得
    void getAlertSettingValue() {
        ArrayList<Map<String, String>> listItem = getListItem(key_LimitDayAlertSetting);
        if (listItem == null || listItem.size() == 0) {
            mAlertOn = false;
            mAlertSpan =  CKUtil.IN_1_MONTH;
        } else {
            if (listItem.size() > 0) {
                Map<String, String> item = listItem.get(0);
                mAlertOn = item.get(col_AlertOnOff) != null && ! item.get(col_AlertOnOff).equals("0");
                mAlertSpan = item.get(col_AlertSpan) == null ? CKUtil.IN_1_MONTH : Integer.valueOf(item.get(col_AlertSpan));
            }
        }
    }

    int getAlertSpan() {
        getAlertSettingValue();
        return mAlertSpan;
    }
    boolean getAlertOnOff() {
        getAlertSettingValue();
        return mAlertOn;
    }

    // 最終起動日時を更新
    void updateLastLaunchedDateTime() {
        setValue(col_Id, key_LastLaunchedDateTime);
        super.upsertData();
    }

    // 最終起動日時を取得
    String getLastLaunchedDateTime() {
        String lastLaunchedDateTime = "";
        ArrayList<Map<String, String>> listItem = getListItem(key_LastLaunchedDateTime);
        if (listItem != null && listItem.size() > 0) {
            lastLaunchedDateTime = listItem.get(0).get(DASettings.col_UpdDt);
        }
        return lastLaunchedDateTime;
    }

    // お知らせ通知ポップアップの表示を判定
    boolean isShowNoticePopup() {
        return isExistKeyRecord(key_IsShowNoticeDialog);
    }

    // お気に入りでチェック済み商品の表示を判定
    boolean isDisplayCheckedItem() {
        ArrayList<Map<String, String>> listItem = getListItem(key_IsDisplayCheckedItem);
        if (listItem == null || listItem.size() == 0) {
            return false;
        } else {
            return listItem.get(0).get(DASettings.col_IsHidden).equals(val_DisplayCheckedItemOn);
        }
    }

    // 商品タブの商品の消費期限を日付で表示するか判定
    boolean isItemListDisplayLimitDate() {
        ArrayList<Map<String, String>> listItem = getListItem(key_ItemListDisplayLimitDate);
        if (listItem == null || listItem.size() == 0) {
            return false;
        } else {
            return listItem.get(0).get(DASettings.col_IsHidden).equals(val_DisplayCheckedItemOn);
        }
    }

    // 商品タブを商品順で表示するか判定
    boolean isItemListDisplayOrderByItem() {
        ArrayList<Map<String, String>> listItem = getListItem(key_ItemListDisplayOrderByItem);
        if (listItem == null || listItem.size() == 0) {
            return false;
        } else {
            return listItem.get(0).get(DASettings.col_IsHidden).equals(val_DisplayCheckedItemOn);
        }
    }

    ArrayList<Map<String, String>> getListItem(String key) {
        ArrayList<String> whereList = new ArrayList<>();
        String whereSql = col_Id + " = ? ";
        whereList.add(key);
        ArrayList<Map<String, String>> listItem = getItemListView(whereSql, whereList.toArray(new String[0]), "");
        whereList.clear();
        return listItem;
    }
}

package com.cloudkura.light;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DABase extends DBHelper {

    static final String col_IsHidden = "IsHidden";
    static final String col_CreDt = "CreDt";
    static final String col_UpdDt = "UpdDt";

    static final String val_HiddenOn = "1";
    static final String val_HiddenOff = "0";

    static final String val_HandOn = "1";
    static final String val_HandOff = "0";

    private String tableName = "";
    private Map<String, String> mColDefs = null;
    private Map<String, String> mPrimaryKeyCols = null;
    private Map<String, String> mValue = null;

    // ToDo: 履歴取得フラグの扱いを考える
    private boolean mTakeHistory = false;

    // テーブル名定義
    void setTableName(String value) {
        tableName = value;
    }

    // 列定義
    void setColDefs(Map<String, String> values) {

        mColDefs = values;

        if (mColDefs != null) {
            // データ用Mapを初期化
            mValue = mColDefs;
            for (Map.Entry<String, String> col: mColDefs.entrySet()){
                mValue.put(col.getKey(), "");
            }
        }
    }

    // PrimaryKey定義
    void setPrimaryKeyDef(Map<String, String> primaryKeyCols) {
        mPrimaryKeyCols = primaryKeyCols;
    }

    // 履歴取得有無
    void setIsTakeHistory(boolean takeHistory) {
        mTakeHistory = takeHistory;
    }

    // テーブル定義
    String getCreatTableSQL() {

        String sqlTableDef = "create table " + tableName;
        StringBuilder sqlColDef = new StringBuilder();
        StringBuilder sqlPrimaryKeyCols = new StringBuilder();
        String sqlPrimaryKeyDef = "";

        for(Map.Entry<String, String> col: mColDefs.entrySet()) {
            if (sqlColDef.length() > 0) { sqlColDef.append(","); }
            String val = col.getKey() + " " + col.getValue() + " ";
            sqlColDef.append(val);
        }

        if (mPrimaryKeyCols != null && ! mPrimaryKeyCols.isEmpty()) {
            for(Map.Entry<String, String> col: mPrimaryKeyCols.entrySet()) {
                if (sqlPrimaryKeyCols.length() > 0) { sqlPrimaryKeyCols.append(","); }
                sqlPrimaryKeyCols.append(col.getKey());
            }
            sqlPrimaryKeyDef = " ,primary key(" + sqlPrimaryKeyCols.toString() + ") ";
        }

        return sqlTableDef + "(" + sqlColDef.toString() + sqlPrimaryKeyDef + ");";
    }

    // コンストラクタ
    DABase() { super(); }

    // キー項目取得 getter
    Map<String, String> getPrimaryKeyColumns() {
        return mPrimaryKeyCols;
    }

    // 全項目 setter / getter
    void setAllValue(Map<String, String> value) {
        mValue = value;
    }
    Map<String, String> getAllValue() {
        return mValue;
    }

    // 項目名指定 getter
    String getValue(String colName) {
        if (mValue == null) {
            return "";
        }
        if (mValue.containsKey(colName)) {
            return mValue.get(colName) == null ? "" : mValue.get(colName);
        } else {
            return "";
        }
    }
    // 項目名指定 setter
    protected void setValue(String colName, String value) {
        if (mValue.containsKey(colName)) {
            mValue.put(colName, value);
        }
    }

    // 文字エスケープ
    String escapeString(String val) {
        return val.replace("'", "''");
    }

    // 任意のSQLの実行結果を取得
    ArrayList<Map<String, String>> getSqlData(String sql) {

        Cursor cursor = getReadableDatabase().rawQuery( sql, null );

        ArrayList<Map<String, String>> list = new ArrayList<>();
        while (cursor.moveToNext()) {

            Map<String, String> value = new HashMap<>();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                value.put(cursor.getColumnName(i), cursor.getString(i));
            }
            list.add(value);
            //value.clear();
        }
        cursor.close();

        return list;
    }

    // レコード数を取得(内部用)
    private int numberOfRows(String whereCondition)
    {
        if (whereCondition.trim().length() > 0){
            whereCondition = " where " + whereCondition;
        }
        Cursor cursor =  getReadableDatabase().rawQuery( "select count(*) cnt from " + tableName + whereCondition + ";", null );

        String val = "0";
        while (cursor.moveToNext()) {
            val = cursor.getString(0);
            break;
        }
        cursor.close();

        return Integer.parseInt(val);
    }

    // レコード数を取得
    int getRowCount(String whereCondition, String[] vals)
    {
        whereCondition = (String) CKUtil.nullTo(whereCondition, "");
        if (! whereCondition.equals("")) { whereCondition = " where " + whereCondition;}

        Cursor cursor =  getReadableDatabase().rawQuery( "select count(*) cnt from " + tableName + whereCondition + ";", vals );

        String val = "0";
        while (cursor.moveToNext()) {
            val = cursor.getString(0);
            break;
        }
        cursor.close();

        return Integer.parseInt(val);
    }

    // select
    Cursor getTableData(String whereCondition, String[] vals, String sortOrder) {
        StringBuilder cols = new StringBuilder();
        for (Map.Entry<String, String> col: mColDefs.entrySet()) {
            if (cols.length() > 0) { cols.append(" ,"); }
            cols.append(col.getKey());
        }

        whereCondition = (String) CKUtil.nullTo(whereCondition, "");
        if (! whereCondition.equals("")) { whereCondition = " where " + whereCondition;}

        sortOrder = (String) CKUtil.nullTo(sortOrder, "");
        if (! sortOrder.equals("")) { sortOrder = " order by " + sortOrder;}

        return getReadableDatabase().rawQuery( "select " + cols.toString() + " from " + tableName + whereCondition + sortOrder + ";", vals );
    }

    ArrayList<Map<String, String>> getItemListView(String whereCondition, String[] vals, String sortOrder) {

        Cursor cursor = getTableData(whereCondition, vals, sortOrder);

        ArrayList<Map<String, String>> list = new ArrayList<>();
        while (cursor.moveToNext()) {

            Map<String, String> value = new HashMap<>(mValue);
            for (Map.Entry<String, String> val: getAllValue().entrySet()) {
                String columnName = val.getKey();
                value.put(columnName, cursor.getString(cursor.getColumnIndex(columnName)));
            }
            mValue = value;
            list.add(value);
            //value.clear();
        }
        cursor.close();

        return list;
    }

    // insert/update
    boolean upsertData() {
        return upsertDataValue(true);
    }
    boolean upsertDataWebSync() {
        return upsertDataValue(false);
    }
    boolean upsertDataValue(boolean isUpdateDtCurrentDateTime)
    {
        ContentValues contentValues = new ContentValues();

        // キー列を合成
        StringBuilder keys = new StringBuilder();
        for (Map.Entry<String, String> key: getPrimaryKeyColumns().entrySet()) {
            if (keys.length() > 0) { keys.append(" and "); }
            keys.append(key.getKey() + " = '" + getValue(key.getKey()) + "'");
        }

        // 処理日時を取得
        String upsertDateTime = CKUtil.createLastUpdateDateTime();

        final String _ERR_SOURCE = ".upsertDataでエラー\n";
        try {

            // 既存データの存在判定
            // select
            if (numberOfRows(keys.toString()) == 0) {

                // 存在しなければinsert
                for (Map.Entry<String, String> val: getAllValue().entrySet()) {
                    contentValues.put(val.getKey(), val.getValue());
                }

                //CreDt,UpdDtに日時設定
                if (isUpdateDtCurrentDateTime) {
                    contentValues.put(col_CreDt, upsertDateTime);
                    contentValues.put(col_UpdDt, upsertDateTime);
                }

                try {
                    getWritableDatabase().insert(tableName, null, contentValues);

                }catch (Exception exception){
                    CKUtil.showLongToast(getClass().getName() + _ERR_SOURCE + exception.getMessage());
                    throw exception;
                }

            } else {

                // 存在すればupdate
                StringBuilder whereColName = new StringBuilder();
                List whereValue = new ArrayList();

                for (Map.Entry<String, String> val: getAllValue().entrySet()) {

                    // キー項目判定
                    if (getPrimaryKeyColumns().containsKey(val.getKey())) {
                        if (whereColName.length() > 0) { whereColName.append(" and "); }
                        String setVal = val.getKey() + " = ? ";
                        whereColName.append(setVal);
                        whereValue.add(val.getValue());
                    } else {
                        contentValues.put(val.getKey(), val.getValue());
                    }
                }

                //UpdDtに日時設定
                if (isUpdateDtCurrentDateTime) {
                    contentValues.put(col_UpdDt, upsertDateTime);
                }

                try {
                    getWritableDatabase().update(tableName, contentValues, whereColName.toString(), (String[])whereValue.toArray(new String[0]));

                }catch (Exception exception){
                    CKUtil.showLongToast(getClass().getName() + _ERR_SOURCE + exception.getMessage());
                    throw exception;
                }

            }
        }catch (Exception exception){
            CKUtil.showLongToast(getClass().getName() + _ERR_SOURCE + exception.getMessage());

        }

        return true;
    }
    // delete(物理削除)
    boolean deleteData()
    {
        StringBuilder whereColName = new StringBuilder();
        List whereValue = new ArrayList();

        final String _ERR_SOURCE = ".deleteDataでエラー\n";
        // キー項目判定
        for (Map.Entry<String, String> val: getAllValue().entrySet()) {
            if (getPrimaryKeyColumns().containsKey(val.getKey())) {
                if (whereColName.length() > 0) { whereColName.append(" and "); }
                String setVal = val.getKey() + " = ? ";
                whereColName.append(setVal);
                whereValue.add(val.getValue());
            }
        }

        try {
            getWritableDatabase().delete(tableName, whereColName.toString(), (String[])whereValue.toArray(new String[0]));
        }catch (Exception exception){
            CKUtil.showLongToast(getClass().getName() + _ERR_SOURCE + exception.getMessage());
            return false;
        }

        return true;
    }

    // delete(論理削除)
    boolean deleteDataLogical() {

        // 隠し項目に設定
        ContentValues contentValues = new ContentValues();
        contentValues.put(col_IsHidden, val_HiddenOn);
        upsertData();

        /*
        ContentValues contentValues = new ContentValues();

        StringBuilder whereColName = new StringBuilder();
        List whereValue = new ArrayList();

        final String _ERR_SOURCE = ".deleteDataでエラー\n";
        // キー項目判定
        for (Map.Entry<String, String> val: getAllValue().entrySet()) {
            if (getPrimaryKeyColumns().containsKey(val.getKey())) {
                if (whereColName.length() > 0) { whereColName.append(" and "); }
                String setVal = val.getKey() + " = ? ";
                whereColName.append(setVal);
                whereValue.add(val.getValue());
            }
        }

        // 隠し項目に設定
        contentValues.put(col_IsHidden, val_HiddenOn);

        try {
            getWritableDatabase().update(tableName, contentValues, whereColName.toString(), (String[])whereValue.toArray(new String[0]));

        }catch (Exception exception){
            CKUtil.showLongToast(getClass().getName() + _ERR_SOURCE + exception.getMessage());
            return false;
        }
        */

        return true;
    }

    // SQL文の直接実行
    boolean execSQL(String sql) {
        final String _ERR_SOURCE = ".execSQLでエラー\n" + sql + "\n";
        try {
            getWritableDatabase().execSQL(sql);
        }catch (Exception exception){
            CKUtil.showLongToast(getClass().getName() + _ERR_SOURCE + exception.getMessage());
            return false;
        }
        return true;
    }
}

package com.cloudkura.light;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DBHelper extends SQLiteOpenHelper {

    // DB名定義
    private static final String DATABASE_NAME = "ckcore.db";
    // DBバージョン情報: onUpgradeを呼び出す場合はこのVersionを修正
    private static int DATABASE_VERSION = 1;

    // コンストラクタ
    DBHelper() {
        super(CKUtil.getMyContext(), DATABASE_NAME , null, DATABASE_VERSION);
    }

    // 初回利用時のテーブル定義、初期値登録
    @Override
    public void onCreate(SQLiteDatabase db) {

        // テーブル定義

        // 商品情報
        db.execSQL( new DAItem().getCreatTableSQL() );
        // ストック情報
        db.execSQL( new DAItemStock().getCreatTableSQL() );
        // 設定情報
        db.execSQL( new DASettings().getCreatTableSQL() );
    }

    // バージョンアップ時にテーブル定義変更がある場合に記述
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (newVersion >= 2) {

            // Sample
            //String sql = "alter table " + DAItem.tableName + " add " + DAItem.col_ItemType + " text ;";
            //db.execSQL( sql );

            //String sql = "alter table " + DAItem.tableName + " add " + DAItem.col_AffiriateUrl + " text;";
            //db.execSQL( sql );
        }
    }

}

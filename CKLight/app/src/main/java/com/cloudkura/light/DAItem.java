package com.cloudkura.light;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class DAItem extends DABase
    implements Serializable
{

    // テーブル名定義
    static final String tableName = "Item";
    // 列名定義
    static final String col_IId = "ItemId";
    static final String col_Explain = "Explain";
    static final String col_ImagePath = "ImagePath";
    static final String col_ItemType = "ItemType";
    static final String col_Calorie = "Calorie";
    static final String col_Barcode = "Barcode";
    static final String col_IsHandFlag = "IsHandFlag";

    // 商品タイプ(0: 通常 / 1: 補充)
    static final String val_ItemType_Normal = "";
    static final String val_ItemType_Fill = "fill";

    // コンストラクタ
    DAItem() {
        super();

        // 列定義
        Map<String, String> colDefs = new HashMap <String, String>() {
            {
                put(col_IId, "text");
                put(col_Explain, "text");
                put(col_ImagePath, "text");
                put(col_ItemType, "text");
                put(col_Calorie, "integer not null default 0");
                put(col_Barcode, "text");
                put(col_IsHandFlag, "integer not null default 0");
                put(col_IsHidden, "integer not null default 0");
                put(col_CreDt, "text not null default (DATETIME( 'now', 'localtime' ))");
                put(col_UpdDt, "text not null default (DATETIME( 'now', 'localtime' ))");
            }
        };

        // キー列定義(Key名のみ指定)
        Map <String, String> primaryKeyCols = new HashMap <String, String>() {
            {
                put(col_IId, "");
            }
        };

        // 親クラスへ必要情報を渡す。
        setTableName(tableName);
        setColDefs(colDefs);
        setPrimaryKeyDef(primaryKeyCols);

        // 履歴を取得するか
        setIsTakeHistory(false);
    }

    public long getRowCount() {
        ArrayList<String> whereList = new ArrayList<>();
        return super.getRowCount("", whereList.toArray(new String[0]));

    }

    // 画像パスを取得
    ArrayList<Map<String, String>> getAllItemImagePath() {
        String sql = "select " + col_ImagePath + " from " + tableName + " where ifnull(" + col_ImagePath + ",'') != ''";
        return super.getSqlData(sql);
    }

    // 更新用
    Map<String, String> getUpsertByIId(String iid) {
        String sql = "select * from " + tableName + " where " + col_IId + " = '" + iid + "'";
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        return data.size() == 0 ? null : data.get(0);
    }

    boolean isExistByImagePath(String path) {
        String sql = "select * from " + tableName + " where " + col_ImagePath + " = '" + path + "'";
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        return data.size() != 0;
    }

    String getIIdByBarcode(String barcode) {
        String sql = "select * from " + tableName + " where " + col_Barcode + " = '" + super.escapeString(barcode) + "'";
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        if (data.size() == 0) {
            return "";
        } else {
            return data.get(0).get(col_IId);
        }
    }

    // ウィッシュ設定商品の存在判定
    boolean isExistFillItem() {
        String sql = "select count(*) cnt from " + tableName + " where " + col_ItemType + " = '" + val_ItemType_Fill + "'";
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        return (0 < Integer.valueOf(data.get(0).get("cnt")));
    }

    ArrayList<String> getSynonimItemName(String searchWord) {
        String sql = "select distinct " + col_Explain + " from " + tableName + " where " + col_Explain + " like '%" + super.escapeString(searchWord) + "%' order by " + col_Explain;
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        ArrayList<String> wordList = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            wordList.add(data.get(i).get(col_Explain));
        }
        return wordList;
    }

    // Item/Stockをまとめて削除
    void deleteItemAndStock(String iid) {

        // 紐づくStockを消す
        DAItemStock daItemStock = CKDBUtil.getDAItemStock();
        ArrayList<Map<String, String>> items = daItemStock.getUpsertByIId(iid);

        // 更新されたデータが取得したリスト内のどの位置かを取得
        for (Map<String, String> item : items) {
            daItemStock.setValue(DAItemStock.col_IId, item.get(DAItemStock.col_IId));
            daItemStock.setLimitDate(item.get(DAItemStock.col_LimitDt));
            daItemStock.deleteData();
        }

        // Itemを消す
        super.deleteData();
    }

    // CK画像非表示化対応
    boolean updateToNewImagePath() {

        String saveImageDir = CKUtil.getImageSaveDirNew() + "/";
        ArrayList<Map<String, String>> data = super.getSqlData("select " + col_IId + ", " + col_ImagePath + " from " + tableName);
        for (int i = 0; i < data.size(); i++) {
            String[] imageFileName = data.get(i).get(col_ImagePath).split("/", 0);
            String sql = " UPDATE " + tableName
                    + "   SET " + col_ImagePath + " = '" + saveImageDir + imageFileName[imageFileName.length - 1] + "' "
                    + " WHERE " + col_IId + " = '" + data.get(i).get(col_IId) + "' ";
            if (! execSQL(sql)) {
                return false;
            }
        }
        return true;
    }
    // 画像の旧保存先が登録されていれば更新処理を流す
    boolean isExistOldImagePathData(String oldImageDir, String newImageDir) {
        String sql = "select count(*) cnt from " + tableName
                   + " where " + col_ImagePath + " like '%" + oldImageDir + "%'"
                   + "   and " + col_ImagePath + " not like '%" + newImageDir + "%'";
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        return (0 < Integer.valueOf(data.get(0).get("cnt")));
    }
}

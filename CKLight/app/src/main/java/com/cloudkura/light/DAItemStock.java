package com.cloudkura.light;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DAItemStock extends DABase
    implements Serializable
{

    // テーブル名定義
    static final String tableName = "ItemStock";
    // 列名定義
    static final String col_IId = "ItemId";
    static final String col_LimitDt = "LimitDt";
    static final String col_ItemCount = "ItemCount";
    static final String col_Memo = "Memo";

    // 買い物リスト識別用
    static final String val_ShoppingList = "9999/12/31";

    // 詳細情報取得列
    static final String col_Detail_IId = "detailIId";
    static final String col_Detail_ImagePath = "detailImagePath";
    static final String col_Detail_Barcode = "detailBarcode";
    static final String col_Detail_ItemType = "detailItemType";
    static final String col_Detail_Explain = "detailExplain";
    static final String col_Detail_Calorie = "detailCalorie";
    static final String col_Detail_Count = "detailCount";
    static final String col_Detail_CalorieTotal = "detailCalorieTotal";
    static final String col_Detail_MinLimitDt = "detailMinLimitDt";
    static final String col_Detail_ItemHiddenFlag = "detailItemHiddenFlg";
    static final String col_Detail_ItemHandFlag = "detailItemHandFlg";
    static final String col_Detail_StockHiddenFlag = "detailIStockHiddenFlg";
    static final String col_Detail_IsExistStock = "isExistStock";

    // PieChart用列名
    static final String col_PieChart_SummaryTitle = "pieTitle";
    static final String col_PieChart_SummaryData = "pieData";

    // テーブル結合用定数
    private final static String _ALIAS_ITEM = "i.";
    private final static String _ALIAS_STOCK = "s.";
    private final static String _ALIAS_STOCK2 = "s2.";

    // コンストラクタ
    public DAItemStock() {
        super();

        // 列定義
        Map <String, String> colDefs = new HashMap <String, String>() {
            {
                put(col_IId, "text");
                put(col_LimitDt, "text");
                put(col_ItemCount, "integer not null default 0");
                put(col_Memo, "text");
                put(col_IsHidden, "integer not null default 0");
                put(col_CreDt, "text not null default (DATETIME( 'now', 'localtime' ))");
                put(col_UpdDt, "text not null default (DATETIME( 'now', 'localtime' ))");
            }
        };

        // キー列定義(Key名のみ指定)
        Map <String, String> primaryKeyCols = new HashMap <String, String>() {
            {
                put(col_IId, "");
                put(col_LimitDt, "");
            }
        };

        // 親クラスへ必要情報を渡す。
        setTableName(tableName);
        setColDefs(colDefs);
        setPrimaryKeyDef(primaryKeyCols);

        // 履歴を取得するか
        setIsTakeHistory(false);
    }

    // 日付はローカライズがあるのでgetter/setterでラッピング
    String getLimitDate() {
        return CKUtil.getFormatDate(CKUtil.isSystemFormatDate(getValue(col_LimitDt)));
    }
    String getSystemFormatLimitDate() {
        return CKUtil.getSystemFormatDate(CKUtil.isSystemFormatDate(getValue(col_LimitDt)));
    }
    void setLimitDate(String date) {
        // 保存時はyyyy/mm/dd形式に変換
        if (CKUtil.isSystemFormatDate(date) == null) {
            date = CKUtil.getSystemFormatDate(CKUtil.isDate(date));
        }
        setValue(DAItemStock.col_LimitDt, date);
    }

    // テーブルキーを指定
    void setKeyValue(String iid, String date) {

        // いずれかがない場合は設定しない
        if (iid == null || iid.equals("")) { return; }
        if (date == null || date.equals("")) { return; }

        // 保存時はyyyy/mm/dd形式に変換
        if (CKUtil.isSystemFormatDate(date) == null) {
            date = CKUtil.getSystemFormatDate(CKUtil.isDate(date));
        }
        setValue(DAItemStock.col_IId, iid);
        setValue(DAItemStock.col_LimitDt, date);
    }
    // テーブル全件の件数
    long getAllRowCount() {

        return super.getRowCount(null, null);

    }

    // 指定期間の件数
    public long getRowCount(String fromDate, String toDate) {

        String whereSql = "";
        ArrayList<String> whereList = new ArrayList<>();

        if (! fromDate.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + DAItemStock.col_LimitDt + " >= ? ";
            whereList.add(fromDate);
        }
        if (! toDate.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + DAItemStock.col_LimitDt + " <= ? ";
            whereList.add(toDate);
        }

        return super.getRowCount(whereSql, whereList.toArray(new String[0]));

    }

    // Item/Stock 結合テーブルからのデータ取得の基SQL(商品単位)
    private String getBaseSqlEachItem() {

        return
            "select " +
            "   " + _ALIAS_ITEM + DAItem.col_IId + " " + DAItem.col_IId +
            "  ," + _ALIAS_ITEM + DAItem.col_Explain + " " + DAItem.col_Explain +
            "  ," + _ALIAS_ITEM + DAItem.col_ImagePath + " " + DAItem.col_ImagePath +
            "  ," + _ALIAS_ITEM + DAItem.col_ItemType + " " + DAItem.col_ItemType +
            "  ," + _ALIAS_ITEM + DAItem.col_Calorie + " " + DAItem.col_Calorie +
            "  ," + _ALIAS_ITEM + DAItem.col_Barcode + " " + DAItem.col_Barcode +
            "  ," + _ALIAS_ITEM + col_IsHidden + " " + col_IsHidden +
            "  ,ifnull(sum(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + "), 0) " + DAItemStock.col_ItemCount +
            "  ,ifnull(min(" + _ALIAS_STOCK + DAItemStock.col_LimitDt + "), '') " + DAItemStock.col_LimitDt +
            getBaseTable()
        ;
    }

    // Item/Stock 結合テーブルからのデータ取得の基SQL(商品、期限、単位)
    private String getBaseSqlEachItemDate() {

        return
                "select " +
                "   " + _ALIAS_ITEM + DAItem.col_IId + " " + DAItem.col_IId +
                "  ," + _ALIAS_ITEM + DAItem.col_Explain + " " + DAItem.col_Explain +
                "  ," + _ALIAS_ITEM + DAItem.col_ImagePath + " " + DAItem.col_ImagePath +
                "  ," + _ALIAS_ITEM + DAItem.col_ItemType + " " + DAItem.col_ItemType +
                "  ," + _ALIAS_ITEM + DAItem.col_Calorie + " " + DAItem.col_Calorie +
                "  ," + _ALIAS_ITEM + DAItem.col_Barcode + " " + DAItem.col_Barcode +
                "  ,ifnull(" + _ALIAS_STOCK + DAItemStock.col_LimitDt + ", '') " + DAItemStock.col_LimitDt +
                "  ," + _ALIAS_ITEM + col_IsHidden + " " + col_IsHidden +
                "  ,ifnull(sum(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + "), 0) " + DAItemStock.col_ItemCount +
                getBaseTable()
                ;
    }

    // Item/Stock 結合テーブルからのデータ取得の基SQL(商品、期限、1個ずつ)
    private String getBaseSqlEachItemDateCount() {

        return
                "select " +
                        "   " + _ALIAS_ITEM + DAItem.col_IId + " " + DAItem.col_IId +
                        "  ," + _ALIAS_ITEM + DAItem.col_Explain + " " + DAItem.col_Explain +
                        "  ," + _ALIAS_ITEM + DAItem.col_ImagePath + " " + DAItem.col_ImagePath +
                        "  ," + _ALIAS_ITEM + DAItem.col_ItemType + " " + DAItem.col_ItemType +
                        "  ," + _ALIAS_ITEM + DAItem.col_Calorie + " " + DAItem.col_Calorie +
                        "  ," + _ALIAS_ITEM + col_IsHidden + " " + col_IsHidden +
                        "  ," + _ALIAS_STOCK + DAItemStock.col_ItemCount + " " + DAItemStock.col_ItemCount +
                        "  ," + _ALIAS_STOCK + DAItemStock.col_LimitDt + " " + DAItemStock.col_LimitDt +
                        getBaseTable()
                ;
    }

    private String getGroupBySqlEachItem() {

        return
            " group by " +
                "   " + _ALIAS_ITEM + DAItem.col_IId +
                "  ," + _ALIAS_ITEM + DAItem.col_Explain +
                "  ," + _ALIAS_ITEM + DAItem.col_ImagePath +
                "  ," + _ALIAS_ITEM + DAItem.col_ItemType +
                "  ," + _ALIAS_ITEM + DAItem.col_Calorie +
                "  ," + _ALIAS_ITEM + col_IsHidden
            ;
    }

    private String getGroupBySqlEachItemDate() {

        return
                " group by " +
                        "   " + _ALIAS_ITEM + DAItem.col_IId +
                        "  ," + _ALIAS_ITEM + DAItem.col_Explain +
                        "  ," + _ALIAS_ITEM + DAItem.col_ImagePath +
                        "  ," + _ALIAS_ITEM + DAItem.col_ItemType +
                        "  ," + _ALIAS_ITEM + DAItem.col_Calorie +
                        "  ," + _ALIAS_STOCK + DAItemStock.col_LimitDt +
                        "  ," + _ALIAS_ITEM + col_IsHidden
                ;
    }

    // 基本テーブル作成
    private String getBaseTable() {

        return
            "  from " + DAItem.tableName + " " + _ALIAS_ITEM.replace(".", "") +
            "  inner join " + DAItemStock.tableName  + " " + _ALIAS_STOCK.replace(".", "") +
            "    on " + _ALIAS_STOCK + DAItemStock.col_IId  + " = " + _ALIAS_ITEM + DAItem.col_IId
        ;
    }

    // 検索条件を作成(在庫あり)
    private String getWhereStatement(ObjectSearchCondition searchCondition) {

        String whereSql = "";

        // IId
        String searchIId = searchCondition.getSearchIId();
        if (! searchIId.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_ITEM + DAItem.col_IId + " = '" + searchIId + "'";
        }

        // 消費期限
        String searchLimitDate = searchCondition.getmSearchLimitDate();
        if (! searchIId.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " = '" + searchLimitDate + "'";
        }

        // 商品名
        String searchWord = searchCondition.getSearchWord();
        searchWord = super.escapeString(searchWord);
        if (! searchWord.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_ITEM + DAItem.col_Explain + " like '%" + searchWord + "%'";
        }

        // バーコード
        String searchBarcode = searchCondition.getBarcode();
        searchBarcode = super.escapeString(searchBarcode);
        if (! searchBarcode.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_ITEM + DAItem.col_IId + " = '" + searchBarcode + "'";
        }

        // 補充リスト用日付のストックを除く
        if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
        whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList + "' ";

        // 非表示ストックを含めない
        if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
        whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_IsHidden + " <> '" + DAItemStock.val_HiddenOn + "' ";

        /*
        // 非表示商品を含めない
        if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
        whereSql = whereSql + _ALIAS_ITEM + DAItem.col_IsHidden + " <> '" + DAItem.val_HiddenOn  + "' ";
        */

        // 消費期限
        String overDate = searchCondition.getSpanOverDate();
        String fromDate = searchCondition.getSpanFromDate();
        String toDate = searchCondition.getSpanToDate();

        if (!overDate.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " < '" + overDate + "'";
        } else {
            if (!fromDate.equals("")) {
                if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
                whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " >= '" + fromDate + "'";
            }
            if (!toDate.equals("")) {
                if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
                whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " <= '" + toDate + "'";
            }
        }

        return whereSql;
    }

    // 検索条件に合致するデータの抽出(商品単位)
    ArrayList<Map<String, String>> getEachItemBySearchCondition(ObjectSearchCondition searchCondition, String sortOrder) {

        // 検索条件
        String whereSql = getWhereStatement(searchCondition);

        // SQL分作成
        String sql = getBaseSqlEachItem() +
            (whereSql.equals("") ? "" : " where " + whereSql ) + getGroupBySqlEachItem() +
            " having ifnull(sum(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + "), 0) > 0 " +
            (sortOrder.equals("") ? "" : " order by " + sortOrder);

        return super.getSqlData(sql);

    }

    // 検索条件に合致するデータの抽出(商品、期限単位)
    ArrayList<Map<String, String>> getEachItemDateBySearchCondition(ObjectSearchCondition searchCondition, String sortOrder) {

        // 検索条件
        String whereSql = getWhereStatement(searchCondition);

        // SQL分作成
        String sql = getBaseSqlEachItemDate() +
                (whereSql.equals("") ? "" : " where " + whereSql ) + getGroupBySqlEachItemDate() +
                " having ifnull(sum(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + "), 0) > 0 " +
                (sortOrder.equals("") ? "" : " order by " + sortOrder);

        return super.getSqlData(sql);
    }

    // 検索条件に合致するデータの抽出(一個ずつ)
    ArrayList<Map<String, String>> getEachItemDateCountBySearchCondition(ObjectSearchCondition searchCondition, String sortOrder) {

        // 検索条件
        String whereSql = getWhereStatement(searchCondition);

        // SQL分作成
        String sql = getBaseSqlEachItemDateCount() +
                (whereSql.equals("") ? "" : " where " + whereSql ) +
                (sortOrder.equals("") ? "" : " order by " + sortOrder);

        return super.getSqlData(sql);

    }

    // 更新用
    ArrayList<Map<String, String>> getUpsertByIId(String iid) {
        return super.getSqlData("select * from " + tableName + " where " + col_IId + " = '" + iid + "' order by " + col_LimitDt);
    }

    // IId、期限でのデータ抽出
    Map<String, String> getItemByKey(String iid, String limitDate) {

        if (iid.equals("") || limitDate.equals("")) { return null; }

        // SQL分作成
        String sql = "select * from " + tableName + " where " + col_IId + " = '" + iid + "' and " + col_LimitDt + " = '" + limitDate + "' ";
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        return data.size() == 0 ? null : data.get(0);
    }

    // IID、期間指定でのデータ抽出(商品個数)
    int getItemCount(String iid, String fromDate, String toDate) {

        String whereSql = DAItem.col_IsHidden + " <> '" + DAItem.val_HiddenOn + "' ";

        if (! iid.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + DAItemStock.col_IId + " = '" + iid + "'";
        }
        if (! fromDate.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + DAItemStock.col_LimitDt + " >= '" + fromDate + "'";
        }
        if (! toDate.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + DAItemStock.col_LimitDt + " <= '" + toDate + "'";
        }
        // 買い物リスト用日付のストックを除く
        if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
        whereSql = whereSql + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList + "' ";

        // SQL分作成
        String sql = "select ifnull(sum(" + col_ItemCount + "), 0) " + col_ItemCount +
                " from " + tableName +
                (whereSql.equals("") ? "" : " where " + whereSql );

        int getCount = 0;
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        if (data != null) {
            Map<String, String> count = data.get(0);
            if (count != null) {
                getCount = Integer.valueOf(count.get(DAItemStock.col_ItemCount));
            }
        }
        return getCount;
    }

    // 日付指定でのデータ抽出(商品個数)
    int getItemCountByLimitDate(String fromDate, String toDate) {

        String whereSql = "";

        whereSql = _ALIAS_STOCK + DAItem.col_IsHidden + " <> '" + DAItem.val_HiddenOn + "'";

        if (! fromDate.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " >= '" + fromDate + "'";
        }
        if (! toDate.equals("")) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " <= '" + toDate + "'";
        }
        // 買い物リスト用日付のストックを除く
        if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
        whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList + "' ";


        // SQL分作成
        String sql = "select ifnull(sum(" + _ALIAS_STOCK + col_ItemCount + "), 0) " + col_ItemCount +
                getBaseTable() +
                (whereSql.equals("") ? "" : " where " + whereSql );

        int getCount = 0;
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        if (data != null) {
            Map<String, String> count = data.get(0);
            if (count != null) {
                getCount = Integer.valueOf(count.get(DAItemStock.col_ItemCount));
            }
        }
        return getCount;
    }

    // 期限未設定の不正データを削除する
    void deleteInvalidData() {

        // 商品がないストック
        String sql = "select " + _ALIAS_STOCK + DAItemStock.col_IId +
                     "      ," + _ALIAS_STOCK + DAItemStock.col_LimitDt +
                     "  from " + DAItemStock.tableName + " " + _ALIAS_STOCK.replace(".", "") +
                     "  left outer join " + DAItem.tableName  + " " + _ALIAS_ITEM.replace(".", "") +
                     "    on " + _ALIAS_ITEM + DAItem.col_IId + " = " + _ALIAS_STOCK + DAItemStock.col_IId +
                     " where " + _ALIAS_ITEM + DAItem.col_IId + " is null ";
        ArrayList<Map<String, String>> delData = getSqlData(sql);
        if (delData != null) {
            DAItemStock daItemStock = CKDBUtil.getDAItemStock();
            for (Map<String, String> data : delData) {
                daItemStock.setValue(col_IId, data.get(col_IId));
                daItemStock.setValue(col_LimitDt, data.get(col_LimitDt));
                daItemStock.deleteData();
            }
        }

        // 消費期限が未登録のストック
        sql = "select " + col_IId + "," + col_LimitDt +" from " + tableName + " where ifnull(" + col_LimitDt + ", '') = ''";
        delData = getSqlData(sql);
        if (delData != null) {
            DAItemStock daItemStock = CKDBUtil.getDAItemStock();
            for (Map<String, String> data : delData) {
                daItemStock.setValue(col_IId, data.get(col_IId));
                daItemStock.setValue(col_LimitDt, data.get(col_LimitDt));
                daItemStock.deleteData();
            }
        }
    }

    // Pieグラフ用データを取得(期限別: 全体)
    ArrayList<Map<String, String>> getPieGraphDataByLimit() {
        return getGraphDataByLimit("");
    }

    // グラフ用データを取得(期限別: IId単位)
    private ArrayList<Map<String, String>> getGraphDataByLimit(String iid) {

        String baseDate = CKUtil.getSystemFormatCurrentDate();

        String sql =
            " select " +
            "        '0' " + col_PieChart_SummaryTitle +
            "       ,sum(ifnull(" + col_ItemCount + ",0)) " + col_PieChart_SummaryData +
            "   from " + DAItemStock.tableName +
            "  where " + DAItemStock.col_LimitDt + " < '" + baseDate  + "'" +
            "    and " + DAItemStock.col_IsHidden + " <> '" + DAItemStock.val_HiddenOn+ "'" +
            "    and " + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList+ "'" +
            (iid.equals("") ? "" : "  and " + DAItemStock.col_IId + " = '" + iid + "'") +
            " union all " +
            " select " +
            "       '1' " + col_PieChart_SummaryTitle +
            "       ,sum(ifnull(" + col_ItemCount + ",0)) " + col_PieChart_SummaryData +
            "   from " + DAItemStock.tableName +
            "  where " + DAItemStock.col_LimitDt + " <= '" + CKUtil.calcDate(baseDate, CKUtil.IN_1_MONTH) + "'" +
            "    and " + DAItemStock.col_LimitDt + " >= '" + baseDate + "'"  +
            "    and " + DAItemStock.col_IsHidden + " <> '" + DAItemStock.val_HiddenOn+ "'" +
            "    and " + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList+ "'" +
            (iid.equals("") ? "" : "  and " + DAItemStock.col_IId + " = '" + iid + "'") +
            " union all " +
            " select " +
            "        '2' " + col_PieChart_SummaryTitle +
            "       ,sum(ifnull(" + col_ItemCount + ",0)) " + col_PieChart_SummaryData +
            "   from " + DAItemStock.tableName +
            "  where " + DAItemStock.col_LimitDt + " <= '" + CKUtil.calcDate(baseDate, CKUtil.IN_1_YEAR) + "'"  +
            "    and " + DAItemStock.col_LimitDt + " >= '" + CKUtil.calcDate(baseDate, CKUtil.IN_1_MONTH + 1) + "'"  +
            "    and " + DAItemStock.col_IsHidden + " <> '" + DAItemStock.val_HiddenOn+ "'" +
            "    and " + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList+ "'" +
            (iid.equals("") ? "" : "  and " + DAItemStock.col_IId + " = '" + iid + "'") +
            " union all " +
            " select " +
            "        '3' " + col_PieChart_SummaryTitle +
            "       ,sum(ifnull(" + col_ItemCount + ",0)) " + col_PieChart_SummaryData +
            "   from " + DAItemStock.tableName +
            "  where " + DAItemStock.col_LimitDt + " >= '" + CKUtil.calcDate(baseDate, CKUtil.IN_1_YEAR + 1) + "'"  +
            "    and " + DAItemStock.col_IsHidden + " <> '" + DAItemStock.val_HiddenOn+ "'" +
            "    and " + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList+ "'" +
            (iid.equals("") ? "" : "  and " + DAItemStock.col_IId + " = '" + iid + "'") +
            "  order by " +
            "        " + col_PieChart_SummaryTitle;

        return super.getSqlData(sql);
    }

    // 商品詳細フッター表示用データ
    Map<String, String> getItemDetail(String iid, String limitDate) {

        String sql =
            "select " +
            "       " + _ALIAS_ITEM + DAItem.col_IId + " " + col_Detail_IId +
            "      ," + _ALIAS_ITEM + DAItem.col_ItemType + " " + col_Detail_ItemType +
            "      ," + _ALIAS_ITEM + DAItem.col_ImagePath + " " + col_Detail_ImagePath +
            "      ," + _ALIAS_ITEM + DAItem.col_Barcode + " " + col_Detail_Barcode +
            "      ," + _ALIAS_ITEM + DAItem.col_Explain + " " + col_Detail_Explain +
            "      ," + _ALIAS_ITEM + DAItem.col_Calorie + " " + col_Detail_Calorie +
            "      ," + _ALIAS_ITEM + DAItem.col_IsHidden + " " + col_Detail_ItemHiddenFlag +
            "      ," + _ALIAS_ITEM + DAItem.col_IsHandFlag + " " + col_Detail_ItemHandFlag +
            "      ,sum(ifnull(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + ",0)) " + col_Detail_Count +
            "      ,sum(ifnull(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + ",0) * ifnull(" + _ALIAS_ITEM + DAItem.col_Calorie + ",0)) " + col_Detail_CalorieTotal +
            "      ," + _ALIAS_STOCK + DAItemStock.col_LimitDt + " " + col_Detail_MinLimitDt +
            getBaseTable() +
            "  where " + _ALIAS_ITEM + DAItem.col_IId + " = '" + iid + "'"  +
            "    and " + _ALIAS_STOCK + DAItemStock.col_LimitDt + " = '" + limitDate + "'"  +
            "    and " + _ALIAS_STOCK + DAItemStock.col_IsHidden + " <> '" + DAItemStock.val_HiddenOn + "'" +
            " group by " +
            "       " + _ALIAS_ITEM + DAItem.col_IId +
            "      ," + _ALIAS_ITEM + DAItem.col_ItemType +
            "      ," + _ALIAS_ITEM + DAItem.col_ImagePath +
            "      ," + _ALIAS_ITEM + DAItem.col_Barcode +
            "      ," + _ALIAS_ITEM + DAItem.col_Explain +
            "      ," + _ALIAS_ITEM + DAItem.col_Calorie +
            "      ," + _ALIAS_ITEM + DAItem.col_IsHidden +
            "      ," + _ALIAS_ITEM + DAItem.col_IsHandFlag +
            "      ," + _ALIAS_STOCK + DAItemStock.col_LimitDt;

        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        if (data != null && data.size() > 0) {
            return data.get(0);
        } else {
            return null;
        }
    }

    // 在庫ありでバーコードを持つデータの存在判定
    boolean isExistHasBarcodeItemWithStock() {

        // 補充リスト用日付のストックを除いてバーコードを持つデータ
        String sql = "select " +
                "   " + _ALIAS_ITEM + DAItem.col_IId + " " + DAItem.col_IId +
                "  ,sum(ifnull(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + ", 0)) " + DAItemStock.col_ItemCount +
                getBaseTable() +
                " where " + _ALIAS_STOCK + DAItemStock.col_LimitDt + " <> '" + DAItemStock.val_ShoppingList + "' " +
                "   and ifnull(" + _ALIAS_ITEM + DAItem.col_Barcode + ",'') <> '' " +
                " group by " + _ALIAS_ITEM + DAItem.col_IId +
                " having sum(ifnull(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + ", 0)) > 0 " +
                " limit 1 ";

        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        return (data.size() > 0);
    }

    String getAddWishCount(String iid) {

        String sql = "select " + col_ItemCount + " from " + DAItemStock.tableName  +
                     " where " + col_LimitDt + " = '" + val_ShoppingList + "'" +
                     "   and " + col_IId + " = '" + iid + "' ";
        ArrayList<Map<String, String>> data = super.getSqlData(sql);
        if (data.size() == 0) {
            return "0";
        } else {
            if (data.get(0).get(col_ItemCount) == null || data.get(0).get(col_ItemCount).equals("")) {
                return "0";
            } else{
                return data.get(0).get(col_ItemCount);
            }
        }
    }


    // 在庫なし商品リスト
    ArrayList<Map<String, String>> getNoStockItemList(boolean isDisplayCheckedItem) {

        String whereSql = "";

        // 補充リスト対象商品
        if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
        whereSql = whereSql + _ALIAS_ITEM + DAItem.col_ItemType + " = '" + DAItem.val_ItemType_Fill + "' ";

        // 補充リスト用日付のストックのみ
        if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
        whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_LimitDt + " = '" + DAItemStock.val_ShoppingList + "' ";

        // チェック済み商品の表示制御
        if (! isDisplayCheckedItem) {
            if (!whereSql.equals("")) {whereSql = whereSql + " and "; }
            whereSql = whereSql + _ALIAS_STOCK + DAItemStock.col_IsHidden + " = '" + DAItemStock.val_HiddenOff + "' ";
        }

        String sql = "select " +
                "   " + _ALIAS_ITEM + DAItem.col_IId + " " + DAItem.col_IId +
                "  ," + _ALIAS_ITEM + DAItem.col_Explain + " " + DAItem.col_Explain +
                "  ," + _ALIAS_ITEM + DAItem.col_ImagePath + " " + DAItem.col_ImagePath +
                "  ," + _ALIAS_ITEM + DAItem.col_ItemType + " " + DAItem.col_ItemType +
                "  ," + _ALIAS_ITEM + DAItem.col_Calorie + " " + DAItem.col_Calorie +
                "  ," + _ALIAS_ITEM + DAItem.col_Barcode + " " + DAItem.col_Barcode +
                "  ,'0' " + DAItem.col_IsHandFlag +
                //"  ," + _ALIAS_ITEM + DAItem.col_IsHandFlag + " " + DAItem.col_IsHandFlag +
                "  ," + _ALIAS_STOCK + DAItemStock.col_LimitDt + " " + DAItemStock.col_LimitDt +
                "  ," + _ALIAS_STOCK + DAItemStock.col_IsHidden + " " + DAItemStock.col_IsHidden +
                "  ,ifnull(" + _ALIAS_STOCK + DAItemStock.col_ItemCount + ", 1) " + DAItemStock.col_ItemCount +
                getBaseTable() +
                (whereSql.equals("") ? "" : " where " + whereSql ) +
                " order by " + _ALIAS_STOCK + DAItemStock.col_IsHidden +
                "         ," + _ALIAS_ITEM + DAItem.col_IId;

        return super.getSqlData(sql);
    }
}

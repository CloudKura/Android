package com.cloudkura.light;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// DB関連クラスのインスタンスを管理する。
public class CKDBUtil {

    // 入力制限
    final static int MAX_LENGTH_ITEM_NAME = 100;
    final static int MAX_ITEM_COUNT = 99;

    static private String whereDiff() {
        return DABase.col_UpdDt + " >= '" + CKUtil.getLastSyncDateTime() + "'";
    }

    // DAItemクラス
    static DAItem getDAItem() {
        return new DAItem();
    }

    // Itemの差分データをJSON形式で取得
    static JSONArray getJsonArrayItem() {
        ArrayList<Map<String, String>> list = getDAItem().getItemListView(whereDiff(),null, DABase.col_UpdDt);
        return CKUtil.getJsonArray(list);
    }

    // 商品画像の差分データをJSON形式で取得
    static JSONArray getJsonArrayItemImage() {
        ArrayList<Map<String, String>> imageList = new ArrayList<>();
        ArrayList<Map<String, String>> list = getDAItem().getItemListView(whereDiff(),null, DABase.col_UpdDt);
        if (list != null) {
            for (Map<String, String> row: list) {
                String imagePath = row.get(DAItem.col_ImagePath);
                String[] imageFileName = imagePath.split("/", 0);
                Map<String, String> imagedData = new HashMap<>();
                String base64Image = CKUtil.encodeTobase64(CKUtil.getBitmap(imagePath));
                if (! base64Image.equals("")) {
                    imagedData.put(DAItem.col_IId, row.get(DAItem.col_IId));
                    imagedData.put("ImageName", imageFileName[imageFileName.length - 1]);
                    imagedData.put("ImageBase64", base64Image);
                    imagedData.put(DAItem.col_CreDt, row.get(DAItem.col_CreDt));
                    imagedData.put(DAItem.col_UpdDt, row.get(DAItem.col_UpdDt));
                    imageList.add(imagedData);
                }
            }
        }

        return CKUtil.getJsonArray(imageList);
    }


    // DAItemStockクラス
    static DAItemStock getDAItemStock() {
        return new DAItemStock();
    }

    // Stockの差分データをJSON形式で取得
    static JSONArray getJsonArrayStock() {
        ArrayList<Map<String, String>> list = getDAItemStock().getItemListView(whereDiff(),null, DABase.col_UpdDt);
        return CKUtil.getJsonArray(list);
    }

    // DASettingsクラス
    static DASettings getDASettings() {
        return new DASettings();
    }

    // Settingsの差分データをJSON形式で取得
    static JSONArray getJsonArraySettings() {
        // 設定値は最終ログイン日時以外の更新分
        String where = whereDiff() + " and " + DASettings.col_Id + " <> '" + DASettings.key_LastLaunchedDateTime + "'";
        ArrayList<Map<String, String>> list = getDASettings().getItemListView(where,null, DABase.col_UpdDt);
        return CKUtil.getJsonArray(list);
    }

    // CK画像非表示化対応
    static boolean setImagesHidden() {

        // 新旧の画像保存先を取得
        String oldImageFileDir = CKUtil.getImageSaveDirOld();
        String newImageFileDir = CKUtil.replaceSaveDirHiddenDir();

        // 旧画像保存先を持つデータがあれば更新し、保存先フォルダの更新を実施する。
        DAItem daUpdateImagePath = new DAItem();
        if ( daUpdateImagePath.isExistOldImagePathData(oldImageFileDir, newImageFileDir)) {
            File imageSaveDir = new File(newImageFileDir);
            if (imageSaveDir.exists()) {
                daUpdateImagePath.updateToNewImagePath();
            }
        }

        return true;
    }

}

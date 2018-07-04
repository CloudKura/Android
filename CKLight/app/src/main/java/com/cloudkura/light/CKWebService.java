package com.cloudkura.light;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class CKWebService {

    static final String _KEY_BARCODE = DAItem.col_Barcode;
    static final String _KEY_ITEM_CODE = DAItem.col_IId;
    static final String _KEY_ITEM_NAME = DAItem.col_Explain;
    static final String _KEY_IMAGE_PATH = DAItem.col_ImagePath;
    static final String _KEY_LIMIT_DAYS = DAItem.col_Calorie;
    static final String _KEY_SOURCE_FLG = DAItem.col_IsHandFlag;
    static final String _KEY_ITEM_URL = "item_url";
    static final String _KEY_AFFI_LINK = "url_affiliate";

    // DB情報同期の結果
    static final String _WEBSYNC_HTTP_OK = "HTTP_OK";
    static final String _WEBSYNC_HTTP_NG = "HTTP_NG";
    static final String _WEBSYNC_UPDATED = "UPDATED:";
    static final String _WEBSYNC_NO_NETWORK = "NO_NETWORK:";
    static final String _WEBSYNC_NO_SYNC_DATA = "NO_SYNC_DATA:";

    public CKWebService() {}

    // Cloud Kura Webマスタから商品情報を取得
    static Map<String, String> getItemInfoFromCloudKuraWebDB(String barcode) {

        // JSON情報を取得
        String ckUrl = CKUtil.getMyString(R.string.webapi_root_url);
        String postUrl = ckUrl + CKUtil.getMyString(R.string.webapi_get_item_by_barcode) + "?barcode=" + barcode + "&uid=" + CKUtil.getUserID();

        String httpText = CKUtil.getHttpText(postUrl);
        JSONObject itemObj = CKUtil.getJSONObject(httpText);
        if (itemObj == null) {
            return null;
        }

        Map<String, String> decideValue = new HashMap<String, String>();
        try {
            // 商品情報
            decideValue.put(_KEY_ITEM_CODE, formatItemName( itemObj.getString("barcode") ));
            decideValue.put(_KEY_ITEM_NAME, formatItemName( itemObj.getString("item_name") ));
            decideValue.put(_KEY_BARCODE, barcode );
            decideValue.put(_KEY_ITEM_URL, formatItemName( itemObj.getString("image_file_path") ));
            decideValue.put(_KEY_LIMIT_DAYS, formatItemName( itemObj.getString("limit_days") ));
            decideValue.put(_KEY_AFFI_LINK, formatItemName( itemObj.getString("url_affiliate") ));
            decideValue.put(_KEY_SOURCE_FLG, "1"); // CKDBからの取得

        } catch (JSONException e) {
            //
        }

        // 画像を取得
        Bitmap image = CKUtil.downloadImage(ckUrl + CKUtil.getMyString(R.string.webapi_image_dir) + decideValue.get(_KEY_ITEM_URL));
        decideValue.put(_KEY_IMAGE_PATH, "");
        if (image != null) {
            // CKDBの画像はサイズ最適化されている前提なのでサイズ調整せずに保存
            String savePath = CKUtil.savePictureSD(image, barcode + ".png", false);
            decideValue.put(_KEY_IMAGE_PATH, savePath);
        }

        return decideValue;
    }

    // 楽天APIを利用して商品情報を取得
    static Map<String, String> getItemInfoFromRakuten(String barcode) {

        // JSON情報を取得
        String postUrl = CKUtil.getMyString(R.string.web_api_rakuten_url)
                + "&keyword=" + barcode + "&applicationId=" + CKUtil.getMyString(R.string.web_api_rakuten_application_id)
                + "&affiliateId=" + CKUtil.getMyString(R.string.web_api_rakuten_affiliate_id);

        String httpText = CKUtil.getHttpText(postUrl);
        JSONObject root = CKUtil.getJSONObject(httpText);
        if (root == null) {
            return null;
        }

        Map<String, String> itemValue = new HashMap<>();
        Map<String, String> decideValue = new HashMap<>();

        // 取得した商品情報
        try {
            int itemNameLength;
            int decideItemNameLength;

            // 商品情報の登録
            JSONArray arrayItems = root.getJSONArray("Items");
            for (int i = 0; i < arrayItems.length(); i++) {
                JSONObject itemObj = arrayItems.getJSONObject(i);
                // 商品名
                itemValue.put(_KEY_ITEM_NAME, formatItemName( itemObj.getJSONObject("Item").get("itemName").toString() ));
                // バーコード
                itemValue.put(_KEY_BARCODE, barcode);
                // アフィリエイトリンク
                //itemValue.put(_KEY_AFFI_LINK, formatItemName( itemObj.getJSONObject("Item").get("affiliateUrl").toString() ));
                // 取得元フラグ
                itemValue.put(_KEY_SOURCE_FLG, "0");
                // 画像URL
                JSONArray arrayUrls = itemObj.getJSONObject("Item").getJSONArray("mediumImageUrls");
                if (arrayUrls.length() > 0) {
                    // 最初の1枚だけ取得
                    String decideUrl = decideValue.get(_KEY_ITEM_URL);
                    if (decideUrl == null || decideUrl.equals("")) {
                        itemValue.put(_KEY_ITEM_URL, arrayUrls.getJSONObject(0).get("imageUrl").toString());
                    }
                }
                if (decideValue.size() > 0) {
                    // 商品名(短いものを優先)
                    itemNameLength = itemValue.get(_KEY_ITEM_NAME).length();
                    decideItemNameLength = decideValue.get(_KEY_ITEM_NAME).length();
                    if (itemNameLength > 0 && decideItemNameLength > 0) {
                        if (itemNameLength > decideItemNameLength) {
                            itemValue.put(_KEY_ITEM_NAME, decideValue.get(_KEY_ITEM_NAME));
                        }
                    }
                }
                decideValue = itemValue;
            }

        } catch (JSONException e) {
            //
        }

        if (decideValue.size() == 0) {
            return null;
        } else {
            // 画像を取得
            String imageUrl = decideValue.get(_KEY_ITEM_URL);
            if (imageUrl != null && ! imageUrl.equals("")) {
                Bitmap image = CKUtil.downloadImage(imageUrl);
                String savePath = CKUtil.savePictureSD(image, barcode + ".png");
                decideValue.put(_KEY_IMAGE_PATH, savePath);
            }
            return decideValue;
        }
    }

    // 商品名から不要な文字を削除
    static private String formatItemName(String nameValue) {

        if (nameValue == null || nameValue.equals("null")) {
            return "";
        }

        // 不要文字を削除
        try {
            nameValue = cutStr(nameValue, "【", "】");
            nameValue = cutStr(nameValue, "(", ")");
            nameValue = cutStr(nameValue, "（", "）");
            nameValue = cutStr(nameValue, "「", "」");
            nameValue = cutStr(nameValue, "[", "]");
            nameValue = cutStr(nameValue, "［", "］");
            nameValue = cutStr(nameValue, "〔", "〕");
            nameValue = cutStr(nameValue, "<", ">");
            nameValue = cutStr(nameValue, "＜", "＞");
            nameValue = cutStr(nameValue, "★", "★");
        } catch (Exception ex) {
            //
        }
        // 文字数制限
        if (nameValue.length() > CKDBUtil.MAX_LENGTH_ITEM_NAME) {
            nameValue = nameValue.substring(0, CKDBUtil.MAX_LENGTH_ITEM_NAME - 1);
        }

        return nameValue;
    }

    static private String cutStr(String strValue , String cutStartStr, String cutEndStr) {

        String rtnValue = strValue;

        if (cutStartStr.trim().equals("") || cutEndStr.trim().equals("")) {
            return  rtnValue;
        }

        if (strValue.contains(cutStartStr) && strValue.indexOf(cutEndStr) > 0 ) {
            boolean isStart = false;
            String buff = "";
            for ( int i = 0; i < strValue.length(); i++) {
                String val = strValue.substring(i,i + 1);
                if (val.equals(cutStartStr)) {
                    isStart = true;
                } else if (val.equals(cutEndStr)) {
                    val = "";
                    isStart = false;
                }
                if (! isStart) {
                    buff += val;
                }
            }
            rtnValue = buff.trim();
        }

        return rtnValue;
    }

    // 英語版で商品情報を取得
    static Map<String, String> getItemInfoFromSearchUPCSite(String barcode) {

        // JSON情報を取得
        String postUrl = CKUtil.getMyString(R.string.web_api_search_upc_url) + barcode;
        String httpText = CKUtil.getHttpText(postUrl);
        if (httpText.equals("")) {
            return null;
        }

        // 返却用の固定値
        Map<String, String> decideValue = new HashMap<>();
        // バーコード
        decideValue.put(_KEY_BARCODE, barcode);
        // アフィリエイトリンク
        decideValue.put(_KEY_AFFI_LINK, "");
        // 取得元フラグ
        decideValue.put(_KEY_SOURCE_FLG, "0");

        // 商品名を取得
        String regexLeftItemName = " availability\">";
        String regexRightItemName = "</a>";
        String regexStringItemName = regexLeftItemName + ".*?" + regexRightItemName;
        String itemName = CKUtil.getMatchString(regexStringItemName, httpText);
        if (! itemName.equals("")) {
            itemName = itemName.replace(regexLeftItemName,"").replace(regexRightItemName,"");
            decideValue.put(_KEY_ITEM_NAME, itemName);
        }

        // 画像を取得
        String regexLeftImageUrl = "<img src=\"";
        String regexRightImageUrl = "\" id";
        String regexStringImageUrl = regexLeftImageUrl + "https://.*?\\.jpg";
        String imageUrl = CKUtil.getMatchString(regexStringImageUrl, httpText);
        if (! imageUrl.equals("")) {
            imageUrl = imageUrl.replace(regexLeftImageUrl,"").replace(regexRightImageUrl,"");
            Bitmap image = CKUtil.downloadImage(imageUrl);
            String savePath = CKUtil.savePictureSD(image, barcode + ".png");
            decideValue.put(_KEY_IMAGE_PATH, savePath);
        }

        return decideValue;
    }

    // 商品情報を取得
    static Map<String, String> getItemInfoFromGoogleShoppingSearch(String barcode) {

        String httpTextImage = CKUtil.getHttpTextGooglePcMode("https://www.google.com/search?tbm=shop&tbs=vw:g&q=" + barcode );
        if (httpTextImage.equals("")) {
            return null;
        }

        // 返却用の固定値
        Map<String, String> decideValue = new HashMap<>();
        // バーコード
        decideValue.put(_KEY_BARCODE, barcode);
        // アフィリエイトリンク
        decideValue.put(_KEY_AFFI_LINK, "");
        // 取得元フラグ
        decideValue.put(_KEY_SOURCE_FLG, "0");

        String itemName = "";
        String imageData = "";

        // 商品情報を保持する部分を切り出し
        String itemInfoContentsStart = "<div data-async-context=\"query:" + barcode + "\" id=\"ires\">";
        String itemInfoContentsEnd = "</body></html>";
        String regexStringItemInfoContents = itemInfoContentsStart + ".*?" + itemInfoContentsEnd;
        String ItemInfoContents = CKUtil.getMatchString(regexStringItemInfoContents, httpTextImage);
        if (ItemInfoContents.equals("")) {
            String regexLeftImageUrl = "https://encrypted-tbn";
            String regexRightImageUrl = "=CAE";
            String regexStringImageUrl = regexLeftImageUrl + ".*?" + regexRightImageUrl;
            imageData = CKUtil.getMatchString(regexStringImageUrl, httpTextImage);
            if (! imageData.equals("")) {
                // 画像を取得
                Bitmap image = CKUtil.downloadImage(imageData);
                decideValue.put(_KEY_IMAGE_PATH, CKUtil.savePictureSD(image, barcode + ".png"));
                // 商品名を取得
                int nameIndex = httpTextImage.indexOf(regexLeftImageUrl);
                itemName = httpTextImage.substring(nameIndex - 100, nameIndex);
                String regexLeftItemName = "alt=\"";
                String regexRightItemName = "\" ";
                String regexStringItemName = regexLeftItemName + ".*?" + regexRightItemName;
                itemName = CKUtil.getMatchString(regexStringItemName, itemName);
                if (! itemName.equals("")) {
                    itemName = itemName.replace(regexLeftItemName,"").replace(regexRightItemName,"");
                    itemName = CKUtil.replaceHtmlFormat(itemName);
                    decideValue.put(_KEY_ITEM_NAME, formatItemName(itemName));
                }
            }
        } else {
            // 商品名と画像を切り出し
            String itemNameStart = "\"><img alt=\"";
            String itemNameEnd = "\" id=\"";
            String regexStringItemName = itemNameStart + ".*?" + itemNameEnd;
            itemName = CKUtil.getMatchString(regexStringItemName, ItemInfoContents);
            if (! itemName.equals("")) {
                itemName = formatItemName(itemName.replace(itemNameStart,"").replace(itemNameEnd,""));
                itemName = CKUtil.replaceHtmlFormat(itemName);
                // 商品名を取得
                decideValue.put(_KEY_ITEM_NAME, formatItemName(itemName));
            }
            String imageDataStart = "var _image_src='";
            String imageDataEnd = "';";
            String regexStringImageData = imageDataStart + ".*?" + imageDataEnd;
            imageData = CKUtil.getMatchString(regexStringImageData, ItemInfoContents);
            if (! imageData.equals("")) {
                imageData = imageData.replace(imageDataStart,"").replace(imageDataEnd,"");
                String header = "base64,";
                imageData = imageData.substring(imageData.indexOf(header));
                imageData = imageData.replace(header,"").replace("\\x3d", "=");
                // 画像を取得
                Bitmap image = CKUtil.decodeBase64(imageData);
                decideValue.put(_KEY_IMAGE_PATH,CKUtil.saveWebImageToSD(image, barcode + ".png"));
            }
        }

        if (itemName.equals("") && imageData.equals("")) {
            return null;
        } else {
            return decideValue;
        }
    }

    // デバイスID等のユーザ情報をPOSTする
    static void postUserInfo() {

        if (! CKUtil.isConnectNetwork()) { return; }

        String postUrl = CKUtil.getMyString(R.string.webapi_root_url) +
                         CKUtil.getMyString(R.string.webapi_reg_user_session) +
                         "?did=" + CKUtil.getDeviceID() +
                         "&uid=" + CKUtil.getUserID();
                ;
        CKUtil.getHttpText(postUrl);
    }

    // ユーザの操作ログをPOSTする
    static void postUserTraceLog(String log) {

        if (! CKUtil.isConnectNetwork()) { return; }

        String postUrl = CKUtil.getMyString(R.string.webapi_root_url) +
                CKUtil.getMyString(R.string.webapi_reg_user_session) +
                "?did=" + CKUtil.getDeviceID() +
                "&uid=" + CKUtil.getUserID() +
                "&log=" + log;
        ;
        CKUtil.getHttpText(postUrl);
    }

    // デバイスID等のユーザ情報をPOSTする
    static String postUserDBData(boolean isForceSync) {

        String returnValue = _WEBSYNC_NO_SYNC_DATA;

        String[] httpResult = new String[2];
        if (! CKUtil.isConnectNetwork()) {
            return _WEBSYNC_NO_NETWORK;
        }

        try {
            if (isForceSync) {
                // 強制同期の場合は同期日時を初期化
                CKUtil.clearLastSyncDateTime();
            }

            // サーバへ送信するデータの生成
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("mode", "[POST]");
            jsonRequest.put("auth_info", CKUtil.getJsonArrayAuthInfo());
            jsonRequest.put("user_info", CKUtil.getJsonArrayUserInfo());
            jsonRequest.put("settings", CKDBUtil.getJsonArraySettings());
            jsonRequest.put("item", CKDBUtil.getJsonArrayItem());
            jsonRequest.put("stock", CKDBUtil.getJsonArrayStock());
            jsonRequest.put("image", CKDBUtil.getJsonArrayItemImage());

            if (! isForceSync) {
                if (jsonRequest.getJSONArray("settings").toString().equals("[]")
                        && jsonRequest.getJSONArray("item").toString().equals("[]")
                        && jsonRequest.getJSONArray("stock").toString().equals("[]")
                        && jsonRequest.getJSONArray("image").toString().equals("[]")) {

                    // 最終同期日時を更新
                    CKUtil.setLastSyncDateTime();
                    return returnValue;
                }
            }

            // 同期処理
            String url = CKUtil.getMyString(R.string.webapi_root_url) + CKUtil.getMyString(R.string.webapi_reg_user_dbdata);
            httpResult = CKUtil.putHttpText(url, jsonRequest.toString());
            // 通信成功時
            String httpReturn = httpResult[CKUtil._POST_RETURN_CODE];
            if (httpReturn == null) {
                httpReturn = "";
            }
            if (! httpReturn.equals(_WEBSYNC_HTTP_OK)) {
                returnValue = _WEBSYNC_HTTP_NG;

            } else {
                //WEBからの返却値を取得
                String returnData = httpResult[CKUtil._POST_RETURN_VALUE];
                JSONObject root = CKUtil.getJSONObject(returnData);
                if (root == null) {
                    returnValue = httpResult[CKUtil._POST_RETURN_CODE];
                } else {
                    // 取得値をローカルへ反映
                    if (saveWebDataToLocal(root)) {
                        returnValue = _WEBSYNC_UPDATED;
                        // 最終同期日時を更新
                        CKUtil.setLastSyncDateTime();
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    // WebDB情報をGETする
    static String getWebDBData(boolean isForceSync) {

        String returnValue = _WEBSYNC_NO_SYNC_DATA;

        String[] httpResult = new String[2];
        if (! CKUtil.isConnectNetwork()) {
            return _WEBSYNC_NO_NETWORK;
        }

        try {
            if (isForceSync) {
                // 強制同期の場合は同期日時を初期化
                CKUtil.clearLastSyncDateTime();
            }

            // サーバへ送信するデータの生成
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("mode", "[GET]");
            jsonRequest.put("auth_info", CKUtil.getJsonArrayAuthInfo());
            jsonRequest.put("user_info", CKUtil.getJsonArrayUserInfo());

            // 同期処理
            String url = CKUtil.getMyString(R.string.webapi_root_url) + CKUtil.getMyString(R.string.webapi_reg_user_dbdata);
            httpResult = CKUtil.putHttpText(url, jsonRequest.toString());
            if (httpResult == null || httpResult[CKUtil._POST_RETURN_CODE] == null) {
                returnValue = _WEBSYNC_HTTP_NG;
            } else {
                // 通信成功時
                if (! httpResult[CKUtil._POST_RETURN_CODE].equals(_WEBSYNC_HTTP_OK)) {
                    returnValue = _WEBSYNC_HTTP_NG;

                } else {
                    //WEBからの返却値を取得
                    String returnData = httpResult[CKUtil._POST_RETURN_VALUE];
                    JSONObject root = CKUtil.getJSONObject(returnData);
                    if (root == null) {
                        returnValue = httpResult[CKUtil._POST_RETURN_CODE];
                    } else {
                        // 取得値をローカルへ反映
                        if (saveWebDataToLocal(root)) {
                            returnValue = _WEBSYNC_UPDATED;
                        }
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    // WEBデータをローカルへ反映
    static private boolean saveWebDataToLocal(JSONObject root) {

        boolean returnValue = false;

        // 結果を取得
        try {

            String result = root.get("result").toString();
            if (result.equals("success")) {
                // WEB処理成功時、他端末での更新データを取得して反映
                // Settings
                JSONArray arraySettings = root.getJSONArray("settings");
                for (int i = 0; i < arraySettings.length(); i++) {
                    DASettings daSettings = null;
                    daSettings = CKDBUtil.getDASettings();
                    JSONObject itemObj = arraySettings.getJSONObject(i);

                    daSettings.setValue(DASettings.col_Id, itemObj.get("param_id").toString());
                    daSettings.setValue(DASettings.col_AlertOnOff, itemObj.get("alert_onoff").toString());
                    daSettings.setValue(DASettings.col_AlertSpan, itemObj.get("alert_span").toString());
                    daSettings.setValue(DASettings.col_IsHidden, itemObj.get("is_hidden").toString());
                    daSettings.setValue(DASettings.col_CreDt, itemObj.get("user_create_dt").toString());
                    daSettings.setValue(DASettings.col_UpdDt, itemObj.get("user_last_upd_dt").toString());
                    // DB反映
                    daSettings.upsertDataWebSync();
                }
                // ItemStock
                JSONArray arrayStock = root.getJSONArray("stock");
                for (int i = 0; i < arrayStock.length(); i++) {
                    DAItemStock daItemStock = null;
                    daItemStock = CKDBUtil.getDAItemStock();
                    JSONObject itemObj = arrayStock.getJSONObject(i);
                    daItemStock.setValue(DAItemStock.col_IId, itemObj.get("item_id").toString());
                    daItemStock.setValue(DAItemStock.col_LimitDt, itemObj.get("limit_dt").toString());
                    daItemStock.setValue(DAItemStock.col_ItemCount, itemObj.get("item_count").toString());
                    daItemStock.setValue(DAItemStock.col_IsHidden, itemObj.get("is_hidden").toString());
                    daItemStock.setValue(DAItemStock.col_CreDt, itemObj.get("user_create_dt").toString());
                    daItemStock.setValue(DAItemStock.col_UpdDt, itemObj.get("user_last_upd_dt").toString());
                    // DB更新
                    daItemStock.upsertDataWebSync();
                }
                // Item
                JSONArray arrayItem = root.getJSONArray("item");
                JSONArray arrayImage = root.getJSONArray("image");
                for (int i = 0; i < arrayItem.length(); i++) {

                    DAItem daItem = null;
                    daItem = CKDBUtil.getDAItem();
                    JSONObject itemObj = arrayItem.getJSONObject(i);

                    daItem.setValue(DAItem.col_IId, itemObj.get("item_id").toString());
                    daItem.setValue(DAItem.col_Explain, itemObj.get("name").toString());
                    daItem.setValue(DAItem.col_ItemType, itemObj.get("item_type").toString());
                    daItem.setValue(DAItem.col_Calorie, itemObj.get("calorie").toString());
                    daItem.setValue(DAItem.col_Barcode, itemObj.get("barcode").toString());
                    daItem.setValue(DAItem.col_IsHandFlag, itemObj.get("is_hand").toString());
                    daItem.setValue(DAItem.col_IsHidden, itemObj.get("is_hidden").toString());
                    daItem.setValue(DAItem.col_CreDt, itemObj.get("user_create_dt").toString());
                    daItem.setValue(DAItem.col_UpdDt, itemObj.get("user_last_upd_dt").toString());
                    // 画像を保存
                    for (int j = 0; j < arrayImage.length(); j++) {
                        JSONObject imageObj = arrayImage.getJSONObject(j);
                        if (imageObj.get("item_id").toString().equals(daItem.getValue(DAItem.col_IId))) {
                            // " "->"+"変換
                            String base64 = imageObj.get("image_base64").toString();
                            //String base64 = imageObj.get("image_base64").toString().replace(" ", "+");
                            if (! base64.equals("")) {
                                Bitmap image = CKUtil.decodeBase64(base64);
                                String imageName = imageObj.get("image_name").toString();
                                daItem.setValue(DAItem.col_ImagePath, CKUtil.saveWebImageToSD(image, imageName));
                            }
                        }
                    }
                    // DB反映
                    daItem.upsertDataWebSync();
                }

                // 承認コードを保存
                CKUtil.saveApprovalCode(root.get("approval_code").toString());

                // 同期成功したら最終同期日時を上書き
                CKUtil.setLastSyncDateTime();

                returnValue = true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return returnValue;
    }

}

package com.cloudkura.light;

import java.io.Serializable;

class ObjectSearchCondition implements Serializable {

    private String mSearchIId = "";
    private String mSearchLimitDate = "";
    private String mSearchWord = "";
    private String mSearchBarcode = "";
    private CKUtil.SelectLimitDay mSelectedLimitDay = CKUtil.SelectLimitDay.ALL;
    private int mAlertSpan = 0;

    public String getSearchIId(){ return mSearchIId; }
    public void setSearchIId(String searchIid){ mSearchIId = searchIid; }

    public String getmSearchLimitDate(){ return mSearchLimitDate; }
    public void setmSearchLimitDate(String searchLimitDate){ mSearchLimitDate = searchLimitDate; }

    public String getSearchWord(){ return mSearchWord; }
    public void setSearchWord(String searchWord){ mSearchWord = searchWord; }

    public String getBarcode(){ return mSearchBarcode; }
    public void setBarcode(String barcode){ mSearchBarcode = barcode; }

    CKUtil.SelectLimitDay getSelectedLimitDay(){ return mSelectedLimitDay; }
    void setSelectedLimitDay(CKUtil.SelectLimitDay selectedLimitDay){
        if (selectedLimitDay == null) {
            selectedLimitDay = CKUtil.SelectLimitDay.ALL;
        }
        mSelectedLimitDay = selectedLimitDay;
        mAlertSpan = 0;
    }
    void setSelectedLimitDay(CKUtil.SelectLimitDay selectedLimitDay, int alertSpan){
        if (selectedLimitDay == null) {
            selectedLimitDay = CKUtil.SelectLimitDay.ALL;
        }
        mSelectedLimitDay = selectedLimitDay;
        mAlertSpan = alertSpan;
    }

    // 日付範囲を取得
    String getSpanOverDate(){
        String date = "";
        if (getSelectedLimitDay().equals(CKUtil.SelectLimitDay.OVER)) {
            date = CKUtil.getSystemFormatCurrentDate();
        }
        return date;
    }
    String getSpanFromDate(){
        String date = "";
        switch (getSelectedLimitDay()) {
            case ALERT_SPAN:
                break;
            case IN_1MONTH:
                date = CKUtil.getSystemFormatCurrentDate();
                break;
            case IN_1YEAR:
                date = CKUtil.calcDate(CKUtil.getSystemFormatCurrentDate(), CKUtil.IN_1_MONTH + 1);
                break;
            case OTHER:
                date = CKUtil.calcDate(CKUtil.getSystemFormatCurrentDate(), CKUtil.IN_1_YEAR + 1);
                break;
        }
        return date;
    }
    String getSpanToDate(){
        String fromDate = getSpanFromDate();
        String toDate = "";
        switch (getSelectedLimitDay()) {
            case IN_1MONTH:
                toDate = CKUtil.calcDate(fromDate, CKUtil.IN_1_MONTH);
                break;
            case IN_1YEAR:
                toDate = CKUtil.calcDate(fromDate, CKUtil.IN_1_YEAR);
                break;
            case ALERT_SPAN:
                toDate = CKUtil.calcDate(fromDate, mAlertSpan);
                break;
        }
        return toDate;
    }

    // 条件指定有無を返す
    boolean hasNoSearchCondition() {
        return (mSearchWord.equals("") && mSelectedLimitDay.equals(CKUtil.SelectLimitDay.ALL));
    }

}

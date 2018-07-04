package com.cloudkura.light;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

// Google AdMod用


public class FragmentTab extends Fragment
{

    /* ToDo: 2018.03.10 Del
    // AdMod用
    AdView mAdView;
    */

    public static final String ARG_TAB_NO = "arg_tab_no";

    // 親Activity
    ActivityMain mParentActivity;

    // フラグメント
    FragmentItemList mFragmentItemList;
    FragmentGraphList mFragmentGraphList;
    FragmentNoStockItemList mFragmentNoStockItemList;

    // 画面の部品
    View mView;

    // 画面部品
    TabLayout mHeaderTab;
    ViewPager mViewPager;

    // 検索条件を保存
    ObjectSearchCondition mSearchCondition = new ObjectSearchCondition();

    // 選択されたタブを保存
    CKUtil.TabPage mSelectedTab = CKUtil.TabPage.TAB_ITEMLIST;


    public FragmentTab() { }

    public static FragmentTab newInstance() {
        return new FragmentTab();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // xmlファイルとの紐付け
        mView = inflater.inflate(R.layout.fragment_tab, null, false);

        /* ToDo: 2018.03.10 Del
        // Google AdMod用
        mAdView = (AdView) mView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        */

        // タブを準備
        mHeaderTab = (TabLayout) mView.findViewById(R.id.fragment_page_frame_tab);
        mViewPager = (ViewPager) mView.findViewById(R.id.fragment_page_frame_pager);

        // 初期タブ選択
        mSelectedTab = CKUtil.TabPage.TAB_ITEMLIST;
        if (getArguments() != null) {
            mSelectedTab = (CKUtil.TabPage) getArguments().getSerializable(ARG_TAB_NO);
        }

        // 表示ページ項目の設定
        PagerAdapter pageAdapter = new PagerAdapter(getChildFragmentManager());

        // ViewPagerにページを設定
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setAdapter(pageAdapter);
        mViewPager.addOnPageChangeListener(pageAdapter);

        // ViewPagerをTabLayoutに設定
        mHeaderTab.setupWithViewPager(mViewPager);
        mHeaderTab.getTabAt(mSelectedTab.ordinal()).select();

        int textSize = 10;

        // タブ：サマリー
        TextView tabGraph = (TextView) inflater.inflate(R.layout.custom_tab, null);
        tabGraph.setText(R.string.tab_title_home);
        tabGraph.setGravity(Gravity.CENTER);
        tabGraph.setTextSize(textSize + 4);
        tabGraph.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tab_home, 0, 0, 0);
        mHeaderTab.getTabAt(0).setCustomView(tabGraph);

        // タブ：商品
        TextView tabItem = (TextView) inflater.inflate(R.layout.custom_tab, null);
        tabItem.setText(R.string.tab_title_itemlist);
        tabItem.setGravity(Gravity.CENTER);
        tabItem.setTextSize(textSize + 4);
        tabItem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tab_item, 0, 0, 0);
        mHeaderTab.getTabAt(1).setCustomView(tabItem);

        // タブ：ストックなし
        TextView tabNoStock = (TextView) inflater.inflate(R.layout.custom_tab, null);
        tabNoStock.setText(R.string.tab_title_no_stock);
        tabNoStock.setGravity(Gravity.CENTER);
        tabNoStock.setTextSize(textSize + 3);
        tabNoStock.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_tab_no_stock, 0, 0,0);
        //tabNoStock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tab_no_stock, 0, 0, 0);
        mHeaderTab.getTabAt(2).setCustomView(tabNoStock);

        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mParentActivity = (ActivityMain) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        /* ToDo: 2018.03.10 Del
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
        */
        super.onDestroy();
    }

    // ViewPagerAdapter
    private class PagerAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {

        // タブのページタイトル定義
        private String[] tabPageName = null;

        PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);

            // ページタイトル名を取得
            tabPageName = new String[CKUtil.TabPage.values().length];
            tabPageName[CKUtil.TabPage.TAB_ITEMLIST.ordinal()] = getString(R.string.tab_title_itemlist);
            tabPageName[CKUtil.TabPage.TAB_HOME.ordinal()] = getString(R.string.tab_title_home);
            tabPageName[CKUtil.TabPage.TAB_NOSTOCK.ordinal()] = getString(R.string.tab_title_no_stock);
        }

        // ページを取得
        @Override
        public Fragment getItem(int position) {

            // 選択されたタブを保存
            CKUtil.TabPage tabPage[]  = CKUtil.TabPage.values();
            mSelectedTab = tabPage[position];

            mParentActivity.setFabVisibility(true);

            Fragment fragment = null;
            switch (mSelectedTab) {
                case TAB_ITEMLIST:
                    fragment = (mFragmentItemList == null ? mFragmentItemList = FragmentItemList.newInstance() : mFragmentItemList);
                    break;

                case TAB_HOME:
                    fragment = (mFragmentGraphList == null ? mFragmentGraphList = FragmentGraphList.newInstance() : mFragmentGraphList);
                    break;

                case TAB_NOSTOCK:
                    fragment = (mFragmentNoStockItemList == null ? mFragmentNoStockItemList = FragmentNoStockItemList.newInstance() : mFragmentNoStockItemList);
                    break;

                default:
            }

            return fragment;
        }

        // ページタイトルの設定
        @Override
        public CharSequence getPageTitle(int position) {
            return tabPageName[position];
        }

        // ページ数を取得
        @Override
        public int getCount() {
            return tabPageName.length;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mParentActivity.setFabVisibility( ! mFragmentItemList.isVisibleItemDetailFooter());
        }

        @Override
        public void onPageSelected(int position) {

            // 選択されたタブを保存
            CKUtil.TabPage tabPage[] = CKUtil.TabPage.values();
            mSelectedTab = tabPage[position];

            if (! mParentActivity.isExistsDisplayItem() ) {
                if (tabPage[position] == CKUtil.TabPage.TAB_HOME) {
                    mParentActivity.fabOpenNoBackground();
                } else {
                    mParentActivity.fabCloseForce();
                }
            }

            if (tabPage[position] == CKUtil.TabPage.TAB_HOME && mFragmentItemList != null) {
                // 画面切り替え時には詳細画面を閉じる
                mFragmentItemList.setItemDetailFooterVisibility(false);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) { }
    }

    // 外部からのタブ指定用
    public void setTabSelect(CKUtil.TabPage selectTab) {
        mHeaderTab.getTabAt(selectTab.ordinal()).select();
    }

    // ヘッダータブの表示制御
    public void setHeaderTabVisibity(boolean isShow) {

        // アニメーション定義
        Animation anim = null;
        anim = AnimationUtils.loadAnimation(CKUtil.getMyContext(), isShow ? R.anim.slide_down : R.anim.slide_up);
        anim.setDuration(isShow ? 200 : 100);
        anim.setFillBefore(true);
        mHeaderTab.startAnimation(anim);
        mHeaderTab.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    public void setSearchCondition() {

        // 検索条件欄の制御
        showSearchCondition();

        // 表示中Fragmentのリフレッシュ
        switch (mSelectedTab) {
            case TAB_ITEMLIST:
                mFragmentItemList.updateDataNotify();
                break;

            case TAB_HOME:
                mFragmentGraphList.refreshGraph();
                break;

            case TAB_NOSTOCK:
                mFragmentNoStockItemList.refreshNoStockItemList();
                break;
        }
    }

    // 検索条件表示欄の制御
    public void showSearchCondition() {

        // 検索条件指定がある場合は条件説明欄を表示
        LinearLayout llSearchCondition = (LinearLayout) mView.findViewById(R.id.fragment_page_frame_search_condition);
        TextView tvSearchCondition = (TextView) mView.findViewById(R.id.fragment_item_search_condition_string);
        if (mSearchCondition.hasNoSearchCondition()) {
            llSearchCondition.setVisibility(View.GONE);
            tvSearchCondition.setText("");
            return;
        }

        // 条件指定
        String condition = "";
        // 消費期限
        String limitDayValue = "";
        switch (mSearchCondition.getSelectedLimitDay()) {
            case ALL:
                break;
            case OVER:
                limitDayValue = getString(R.string.limit_day_over).replace("\n","");
                break;
            case IN_1MONTH:
                limitDayValue = getString(R.string.limit_day_in1month).replace("\n","");
                break;
            case IN_1YEAR:
                limitDayValue = getString(R.string.limit_day_in1year).replace("\n","");
                break;
            case OTHER:
                limitDayValue = getString(R.string.limit_day_other).replace("\n","");
                break;
        }
        if (! limitDayValue.equals("")) {
            if (! condition.equals("")) {
                condition = condition + " + ";
            }
            condition = condition + "'" + limitDayValue + "'";
        }

        // 商品名
        if (! mSearchCondition.getSearchWord().equals("")) {
            if (!condition.equals("")) {
                condition = condition + " + ";
            }
            condition = condition + "'" + mSearchCondition.getSearchWord() + "'";
        }

        llSearchCondition.setVisibility(View.VISIBLE);
        tvSearchCondition.setText(condition);
    }

}

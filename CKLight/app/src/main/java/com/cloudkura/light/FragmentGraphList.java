package com.cloudkura.light;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

public class FragmentGraphList extends Fragment {

    // 親Activity
    ActivityMain mParentActivity;

    // グラフ種類
    private enum GraphType{
        LIMIT_DAY,
    }

    // 画面の部品
    Dialog dialog;
    View mView;
    GraphType mSelectGraphType = GraphType.LIMIT_DAY;

    // 欄タイトル
    TextView mTvGraphTitle;
    TextView mTvListTitle;

    AbsListView mAbsListView;
    GraphLegendListAdapter mAdapter;

    public FragmentGraphList() { }

    public static FragmentGraphList newInstance() {
        return new FragmentGraphList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // xmlファイルとの紐付け
        mView = inflater.inflate(R.layout.fragment_graph_list, null, false);

        //データ無し時に表示するView
        final TextView tvEmptyView = (TextView) mView.findViewById(R.id.fragment_graph_list_empty_view);
        //リストの準備
        mAbsListView = (ListView) mView.findViewById(R.id.fragment_graph_list_list_view);
        mAbsListView.setEmptyView(tvEmptyView);
        //欄タイトル
        mTvGraphTitle = (TextView) mView.findViewById(R.id.fragment_graph_graph_title);
        mTvListTitle = (TextView) mView.findViewById(R.id.fragment_graph_list_title);

        // 初期表示
        setupPieChartView(GraphType.LIMIT_DAY);

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

    // タブナビゲーション切り替えイベント
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            // グラフ切り替え
            switch (item.getItemId()) {
                // 消費期限タブ
                case R.id.menu_graph_tab_limit_day:
                    setupPieChartView(GraphType.LIMIT_DAY);
                    break;
            }

            return true;
        }

    };

    // グラフリフレッシュ
    public void refreshGraph() {
        setupPieChartView(mSelectGraphType);
    }

    // 円グラフ
    private void setupPieChartView(GraphType graphType) {

        // グラフタイプを保存
        mSelectGraphType = graphType;

        // グラフ
        PieChart pieChart = (PieChart) mView.findViewById(R.id.fragment_graph_list_graph);
        pieChart.setMotionEventSplittingEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setNoDataText("");
        pieChart.setHoleRadius(50f);
        pieChart.setTouchEnabled(false);

        // 凡例
        Legend legend = pieChart.getLegend();
        legend.setEnabled(false);
        legend.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
        legend.setFormSize(15f);
        legend.setForm(Legend.LegendForm.CIRCLE);

        // データ取得
        DAItemStock daItemStock = CKDBUtil.getDAItemStock();

        // 種類の数
        String[] itemTypeList = {};
        Float itemTypeValue[];

        // 種類別の個数を取得
        int allItemCount = 0;
        ArrayList<Map<String, String>> graphData = null;
        switch (graphType) {
            case LIMIT_DAY:
                graphData = daItemStock.getPieGraphDataByLimit();
                itemTypeList = new String [] {
                        getString(R.string.limit_day_over_detail),
                        getString(R.string.limit_day_in1month_detail),
                        getString(R.string.limit_day_in1year_detail),
                        getString(R.string.limit_day_other_detail)
                };
                // 総数を取得
                for (Map<String, String> data : graphData) {
                    allItemCount = allItemCount + Integer.valueOf((String) CKUtil.nullTo(data.get(DAItemStock.col_PieChart_SummaryData), "0") );
                }
                // データ有無によるデザイン変更
                if(allItemCount==0) {
                    // データない場合の表示
                    pieChart.setVisibility(View.GONE);
                    mTvGraphTitle.setVisibility(View.GONE);
                    mTvListTitle.setVisibility(View.GONE);

                } else {
                    // データある場合の表示
                    pieChart.setCenterText(getString(R.string.message_sum_count) + String.valueOf(allItemCount) + " " + CKUtil.getMyString(R.string.notify_item_unit));
                    pieChart.setCenterTextSize(15f);
                    pieChart.setVisibility(View.VISIBLE);
                    mTvGraphTitle.setVisibility(View.VISIBLE);
                    mTvListTitle.setVisibility(View.VISIBLE);
                }
                break;
        }

        itemTypeValue = new Float[itemTypeList.length];
        String[] itemCount = new String[itemTypeList.length];
        pieChart.setDescription("");

        List<Integer> graphColors = new ArrayList<>();
        List<Integer> legendColors = new ArrayList<>();

        // 種類別の比率を取得
        for (Map<String, String> data : graphData) {
            for (int i = 0; i < itemTypeList.length; i++) {
                String summaryTitle = data.get(DAItemStock.col_PieChart_SummaryTitle);
                if (String.valueOf(i).equals(summaryTitle)) {
                    String summaryData = (String) CKUtil.nullTo(data.get(DAItemStock.col_PieChart_SummaryData), "0");
                    itemCount[i] = String.format(Locale.ENGLISH, "%,d", Integer.valueOf(summaryData));
                    itemTypeValue[i] = (Float.valueOf(summaryData) / allItemCount ) * 100;

                    if (Integer.valueOf(summaryData) > 0) {
                        // 色指定(グラフ)
                        switch (summaryTitle) {
                            case "0":
                                graphColors.add(CKUtil.getMyColor(R.color.colorExclamation));
                                break;
                            case "1":
                                graphColors.add(CKUtil.getMyColor(R.color.colorAlert));
                                break;
                            case "2":
                                graphColors.add(CKUtil.getMyColor(R.color.colorInfo));
                                break;
                            case "3":
                                graphColors.add(CKUtil.getMyColor(R.color.colorNoInformation));
                                break;
                        }
                    }

                    // 色指定(凡例)
                    switch (summaryTitle) {
                        case "0":
                            legendColors.add(CKUtil.getMyColor(R.color.colorExclamation));
                            break;
                        case "1":
                            legendColors.add(CKUtil.getMyColor(R.color.colorAlert));
                            break;
                        case "2":
                            legendColors.add(CKUtil.getMyColor(R.color.colorInfo));
                            break;
                        case "3":
                            legendColors.add(CKUtil.getMyColor(R.color.colorNoInformation));
                            break;
                    }
                }
            }
        }

        // 円グラフに表示するデータ
        List<String> labels = new ArrayList<>();
        List<Float> values = Arrays.asList(itemTypeValue);
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null && values.get(i) > 0) {
                labels.add(itemTypeList[i]);
                entries.add(new Entry(values.get(i), i));
            }
        }

        // グラフ表示
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(graphColors);
        dataSet.setDrawValues(false);

        PieData pieData = new PieData(labels, dataSet);
        pieData.setHighlightEnabled(false);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(0f);
        pieData.setValueTextColor(Color.BLACK);

        pieChart.setData(pieData);

        // 描画更新を通知してリフレッシュ
        pieChart.notifyDataSetChanged();
        pieChart.invalidate();

        // 凡例情報をリスト表示
        ArrayList<Map<String, String>> legendListData = new ArrayList<>();
        DecimalFormat dcf = new DecimalFormat("#,##0.0");
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null && values.get(i) > 0) {
                Map<String, String> legendVal = new HashMap<>();
                legendVal.put("color",legendColors.get(i).toString());
                legendVal.put("label", itemTypeList[i]);
                legendVal.put("count", itemCount[i]);
                legendVal.put("rate", dcf.format(values.get(i)) + " %");
                legendListData.add(legendVal);
            }
        }

        //凡例一覧を表示
        mAdapter = new GraphLegendListAdapter(CKUtil.getMyContext(), R.layout.row_graph_legend, legendListData);
        mAbsListView.setAdapter(mAdapter);

        // タップ時のイベント
        mAbsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // 詳細画面を表示
                String dialogTag = "use_list";
                if (getFragmentManager().findFragmentByTag(dialogTag) == null) {
                    DialogUseList useList = DialogUseList.newInstance();
                    useList.setTargetFragment(FragmentGraphList.this, 0);

                    Map<String, String> selectItem = (Map<String, String>) parent.getItemAtPosition(position);

                    //引数設定
                    Bundle args = new Bundle();
                    args.putString(DialogUseList.ARG_SEARCH_CONDITION, selectItem.get("label"));
                    useList.setArguments(args);
                    useList.setCancelable(false);
                    useList.show(getFragmentManager(), dialogTag);
                }
            }
        });

        //スクロール時のイベント
        mAbsListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                // 上下タブの表示制御
                switch (scrollState) {
                    // スクロールが停止したら表示
                    case SCROLL_STATE_IDLE:
                        mParentActivity.setFabVisibility(true);
                        break;
                    // スクロール中は非表示
                    case SCROLL_STATE_TOUCH_SCROLL:
                    case SCROLL_STATE_FLING:
                        mParentActivity.setFabVisibility(false);
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    // 凡例情報を表示するためのAdapterを定義
    private class GraphLegendListAdapter extends ArrayAdapter<Map<String, String>> {

        private LayoutInflater inflater;
        private int inflate_item_id;

        GraphLegendListAdapter(Context context, int id, ArrayList<Map<String, String>> items) {
            super(context, id, items);

            inflate_item_id = id;
            inflater = LayoutInflater.from(context);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(inflate_item_id, parent, false);
            }
            // タップ時のアニメーション
            convertView.setOnTouchListener(CKUtil.doOnTouchAnimation());

            // 汎用欄
            TextView tvRowId = (TextView) convertView.findViewById((R.id.row_graph_legend_row_id)) ;
            View vwLegendColor = convertView.findViewById((R.id.row_graph_legend_color)) ;
            TextView tvLegendLabel = (TextView) convertView.findViewById((R.id.row_graph_legend_label)) ;
            TextView tvLegendCount = (TextView) convertView.findViewById((R.id.row_graph_legend_count)) ;
            TextView tvLegendRate = (TextView) convertView.findViewById((R.id.row_graph_legend_rate)) ;

            Map<String, String> val = getItem(position);
            tvRowId.setText(val.get("row_id"));
            vwLegendColor.setBackgroundColor(Integer.valueOf(val.get("color")));
            tvLegendLabel.setText(val.get("label"));
            tvLegendCount.setText(val.get("count") + " " + CKUtil.getMyString(R.string.notify_item_unit));
            tvLegendRate.setText(val.get("rate"));

            return convertView;
        }
    }

}

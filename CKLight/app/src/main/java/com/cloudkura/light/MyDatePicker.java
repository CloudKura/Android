package com.cloudkura.light;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;

public class MyDatePicker extends DatePicker
{

    NumberPicker mNpYear;
    NumberPicker mNpMonth;
    NumberPicker mNpDay;

    public MyDatePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (mNpYear == null) {
            mNpYear = (NumberPicker) this.findViewById(Resources.getSystem().getIdentifier("year", "id", "android"));
            mNpMonth = (NumberPicker) this.findViewById(Resources.getSystem().getIdentifier("month", "id", "android"));
            mNpDay = (NumberPicker) this.findViewById(Resources.getSystem().getIdentifier("day", "id", "android"));
        }

        // 初期表示時のレイアウト変更
        customiseNumberPicker(mNpYear);
        customiseNumberPicker(mNpMonth);
        customiseNumberPicker(mNpDay);

        // スクロール完了時のレイアウト変更
        mNpYear.setOnScrollListener(new NumberPicker.OnScrollListener() {
            @Override
            public void onScrollStateChange(NumberPicker view, int scrollState) {
                if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                    customiseNumberPicker(view);
                }
            }
        });
        mNpMonth.setOnScrollListener(new NumberPicker.OnScrollListener() {
            @Override
            public void onScrollStateChange(NumberPicker view, int scrollState) {
                if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                    customiseNumberPicker(view);
                }
            }
        });
        mNpDay.setOnScrollListener(new NumberPicker.OnScrollListener() {
            @Override
            public void onScrollStateChange(NumberPicker view, int scrollState) {
                if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                    customiseNumberPicker(view);
                }
            }
        });

    }

    private void customiseNumberPicker(NumberPicker picker) {

        CKUtil.setNumberPickerDividerNone(picker);

        EditText editText = (EditText) picker.getChildAt(0);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24.0f);
        editText.setTextColor(Color.BLACK);
        editText.setMinHeight(20);
        editText.setFocusable(false);

        editText.setVisibility(View.GONE);
        editText.setVisibility(View.VISIBLE);
    }

}
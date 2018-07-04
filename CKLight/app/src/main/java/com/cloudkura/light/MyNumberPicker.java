package com.cloudkura.light;

import android.content.Context;
import android.widget.NumberPicker;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.util.TypedValue;
import android.graphics.Color;

public class MyNumberPicker extends NumberPicker
{

    public MyNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        customiseNumberPicker();

        // スクロール完了時のレイアウト変更
        this.setOnScrollListener(new NumberPicker.OnScrollListener(){
            @Override
            public void onScrollStateChange(NumberPicker view, int scrollState) {
                if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                    customiseNumberPicker();
                }
            }
        });
    }

    private void customiseNumberPicker() {
        EditText etInput = (EditText) this.getChildAt(0);
        etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22.0f);
        etInput.setTextColor(Color.BLACK);
        etInput.setMinHeight(30);
        etInput.setFocusable(false);

        //etInput.setVisibility(View.GONE);
        //etInput.setVisibility(View.VISIBLE);
    }
}
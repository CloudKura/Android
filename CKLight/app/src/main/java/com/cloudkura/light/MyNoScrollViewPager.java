package com.cloudkura.light;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MyNoScrollViewPager extends ViewPager {

    public MyNoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // スクロールをキャンセル
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // スクロールをキャンセル
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // ViewPagerの heigh: wrap_content を有効にする。
        try {
            int height = 0;
            for(int i = 0; i < getChildCount(); i++){
                View child = getChildAt(i);
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int h = child.getMeasuredHeight();
                if(h > height) height = h;
            }
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height + 10, MeasureSpec.EXACTLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
package com.cloudkura.light;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class MyLayerViewOnTap extends View {

    View mView;
    ImageView mIvTapPoint;
    WindowManager mWindowManager;

    public MyLayerViewOnTap(Context context, AttributeSet attrs) {
        super(context, attrs);
        customizeViewGroup(context);
    }

    private void customizeViewGroup(Context context) {

       // Viewからインフレータを作成する
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        // レイアウトファイルから重ね合わせするViewを作成する
        mView = layoutInflater.inflate(R.layout.layer_service, null);
        mIvTapPoint = (ImageView)mView.findViewById(R.id.layer_service_on_touch);
        mIvTapPoint.setAlpha(0f);
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                final MotionEvent motionEvent = event;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                        // タップされた場所に表示
                        final float eventX = motionEvent.getX();
                        final float eventY = motionEvent.getY();
                        final int viewWidth = 200;
                        final int viewHeight = 200;
                        int left = (int) (eventX - (float) (viewWidth / 2));
                        int top = (int) (eventY - (float) (viewHeight / 2));
                        mIvTapPoint.layout(left, top, left + viewWidth, top + viewHeight);

                        // アニメーションの時間を設定
                        ValueAnimator anim = ValueAnimator.ofFloat(50f, 0f);
                        anim.setDuration(300);
                        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float animValue = (float) animation.getAnimatedValue();
                                mIvTapPoint.setAlpha(animValue / 100f);

                                int width = viewWidth - (int) animValue * 2;
                                int height = viewHeight - (int) animValue * 2;
                                int left = (int) (eventX - (float) (width / 2));
                                int top = (int) (eventY - (float) (height / 2));
                                mIvTapPoint.layout(left, top, left + width, top + height);
                            }
                        });
                        anim.start();
                    }
                });
                return false;
            }
        });

        // WindowManagerを取得する
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        // Viewを画面上に重ね合わせする
        mWindowManager.addView(mView,
                new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_BASE_APPLICATION,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE                       // これを忘れるとタッチイベントが伝播されず何も操作できなくなる。
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE             // NOT_FOCUSABLE + WATCH_OUTSIDE_TOUCHでViewの外のタッチイベントをフックする
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSPARENT)
        );
    }
}
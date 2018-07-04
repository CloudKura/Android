/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudkura.light;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.cloudkura.light.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Graphic instance for rendering barcode position, size, and ID within an associated graphic
 * overlay view.
 */
public class BarcodeGraphic extends GraphicOverlay.Graphic {

    private int mId;

    private Paint mRectPaint;
    private volatile Barcode mBarcode;

    private RectF mPreRect;

    BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        final int selectedColor = Color.RED;

        mRectPaint = new Paint();
        mRectPaint.setColor(selectedColor);
        mRectPaint.setAntiAlias(true);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(4.0f);

    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public Barcode getBarcode() {
        return mBarcode;
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(Barcode barcode) {
        mBarcode = barcode;
        postInvalidate();
    }

    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Barcode barcode = mBarcode;
        if (barcode == null) {
            return;
        }

        // Draws the bounding box around the barcode.
        RectF rect = new RectF(barcode.getBoundingBox());
        // padding設定
        rect.left = translateX(rect.left) - 50;
        rect.top = translateY(rect.top) - 50;
        rect.right = translateX(rect.right) + 50;
        rect.bottom = translateY(rect.bottom) + 50;
        /*
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);
         */

        // フレームのブレを低減させる
        if (mPreRect == null) {
            canvas.drawRect(rect, mRectPaint);

        } else {
            if (Math.abs(rect.left - mPreRect.left) > 50
                || Math.abs(rect.right - mPreRect.right) > 50
                || Math.abs(rect.top - mPreRect.top) > 50
                || Math.abs(rect.bottom - mPreRect.bottom) > 50) {

                canvas.drawRect(rect, mRectPaint);
            } else {

                canvas.drawRect(mPreRect, mRectPaint);
            }
        }

        mPreRect = rect;

        // ToDo 認識した値を描画する場合はここを復活
        // Draws a label at the bottom of the barcode indicate the barcode value that was detected.
        //canvas.drawText(barcode.rawValue, rect.left, rect.bottom, mTextPaint);
    }
}

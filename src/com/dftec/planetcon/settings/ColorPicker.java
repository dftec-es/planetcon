/*
 * Copyright 2013 Piotr Adamus
 * Copyright 2014 David Fernandez <dftec.es@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dftec.planetcon.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPicker extends View {

    private final static float MIN_VALUE = 0.5f;    // minimum saturation and value
    private final static int HUE_OFFSET = 0;        // angle offset (in degrees) for red color
    private final static int SHAPE_DIAMOND = 0;
    private final static int SHAPE_DISC = 1;
    private final static int SHAPE_SQUARE = 2;

    //Customizable display parameters (in percents)
    private final static int OUTER_PADDING = 4;     // outer padding of the whole color picker view
    private final static int INNER_PADDING = 5;     // distance between hue ring and inner color disc
    private final static int HUE_RING_WIDTH = 10;   // width of the hue ring
    private final static int HUE_ARROW_SIZE = 4;    // size of the arrow pointer; set to 0 to hide the pointer

    private int mShape;
    private boolean mHueTouch;

    private Paint mInitialViewPaint;
    private Paint mColorPaint;
    private Paint mPointerPaint;
    private Paint mHueRingPaint;
    private Paint mHueArrowPaint;

    private Path mHueArrowPath;
    private Path mTrianglePath;
    private Bitmap mColorDiscBitmap;
    private RectF mSquareRect;

    private int mHueRingWidth;
    private int mHueArrowSize;

    private float mOuterRingRadius;
    private float mInnerRingRadius;
    private float mColorDiscRadius;
    private float mSquareRadius;
    private float mInitialViewRadius;

    private int mColorInitial;
    private float[] mColorHSV;   //Currently selected color
    private float[] mHSV;        //Temp HSV color
    private double[] mXY;        //Temp x and y coordinates
    

    public ColorPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPicker(Context context) {
        super(context);
        init();
    }

    private void init() {
        mShape = SHAPE_DIAMOND;
        mHueTouch = false;

        mColorInitial = 0;
        mColorHSV = new float[] { 0f, 1f, 1f };
        mHSV = new float[] { 0f, 1f, 1f };
        mXY = new double[] {0, 0};

        mInitialViewPaint = new Paint();
        mInitialViewPaint.setAntiAlias(true);

        mColorPaint = new Paint();
        mColorPaint.setAntiAlias(true);
        mColorPaint.setDither(true);

        mPointerPaint = new Paint();
        mPointerPaint.setStyle(Style.STROKE);
        mPointerPaint.setStrokeWidth(2f);
        mPointerPaint.setAntiAlias(true);

        mHueRingPaint = new Paint();
        mHueRingPaint.setStyle(Style.STROKE);
        mHueRingPaint.setAntiAlias(true);
        mHueRingPaint.setDither(true);

        mHueArrowPaint = new Paint();
        mHueArrowPaint.setAntiAlias(true);

        mHueArrowPath = new Path();
        mTrianglePath = new Path();

        mSquareRect = new RectF();

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Needed by ComposeShader
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(widthSize, heightSize);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);

        int centerX = width / 2;
        int centerY = height / 2;
        int innerPadding;
        int outerPadding;

        mHueRingWidth = (HUE_RING_WIDTH * width / 100);
        mHueArrowSize = (HUE_ARROW_SIZE * width / 100);
        innerPadding = (INNER_PADDING * width / 100);
        outerPadding = (OUTER_PADDING * width / 100);

        mOuterRingRadius = width / 2 - mHueArrowSize - outerPadding;
        mInnerRingRadius = mOuterRingRadius - mHueRingWidth;
        mColorDiscRadius = mInnerRingRadius - innerPadding;
        mSquareRadius = (float)(mColorDiscRadius / Math.sqrt(2));

        mInitialViewRadius = mHueRingWidth;

        if (mShape == SHAPE_DISC) {
            mColorDiscBitmap = createColorDiscBitmap((int)mColorDiscRadius);
        }
        else if (mShape == SHAPE_SQUARE) {
            mSquareRect.set(centerX - mSquareRadius, centerY - mSquareRadius, centerX + mSquareRadius, centerY + mSquareRadius);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float pointerRadius;
        double colorPointX, colorPointY;
        double hueAngle = Math.toRadians(getHue() + HUE_OFFSET);
        double hueAngleX = Math.cos(hueAngle);
        double hueAngleY = Math.sin(hueAngle);

        // drawing color view: current and previous color

        mInitialViewPaint.setColor(mColorInitial);
        canvas.drawCircle(getWidth() - mInitialViewRadius, mInitialViewRadius, mInitialViewRadius, mInitialViewPaint);
        mInitialViewPaint.setColor(Color.HSVToColor(mColorHSV));
        canvas.drawCircle(getWidth() - mInitialViewRadius, getHeight() - mInitialViewRadius, mInitialViewRadius, mInitialViewPaint);

        // drawing hue ring

        SweepGradient sweepGradient = new SweepGradient(xToDraw(0), yToDraw(0), getColors(mColorHSV), null);
        mHueRingPaint.setShader(sweepGradient);
        mHueRingPaint.setStrokeWidth(mHueRingWidth);
        canvas.drawCircle(xToDraw(0), yToDraw(0), mInnerRingRadius + mHueRingWidth/2, mHueRingPaint);

        // drawing hue pointer

        mPointerPaint.setColor(Color.BLACK);
        canvas.drawLine(xToDraw(hueAngleX * mInnerRingRadius), yToDraw(hueAngleY * mInnerRingRadius),
                xToDraw(hueAngleX * mOuterRingRadius), yToDraw(hueAngleY * mOuterRingRadius), mPointerPaint);

        // drawing hue arrow

        if (mHueArrowSize > 0) {
            drawPointerArrow(canvas, hueAngle, mOuterRingRadius);
        }

        if (mShape == SHAPE_DISC) {
            // drawing color disc
            canvas.drawBitmap(mColorDiscBitmap, xToDraw(-mColorDiscRadius), yToDraw(mColorDiscRadius), null);

            colorPointX = getValue() * hueAngleX * mColorDiscRadius;
            colorPointY = getValue() * hueAngleY* mColorDiscRadius;
        }
        else if (mShape == SHAPE_SQUARE) {
            // drawing color square
            drawColorSquare(canvas);

            colorPointX = (getValue() - 0.5f) * 2 * mSquareRadius;
            colorPointY = (getSaturation() - 0.5f) * 2 * mSquareRadius;
        }
        else { //mShape == SHAPE_DIAMOND
            // drawing color diamond
            drawColorDiamond(canvas, hueAngle, mColorDiscRadius);
            drawSelectableDiamond(canvas, hueAngle, mColorDiscRadius);

            diamondHSVtoXY(getSaturation(), getValue(), mColorDiscRadius, mXY);
            colorPointX = mXY[0];
            colorPointY = mXY[1];
        }

        // drawing color pointer

        pointerRadius = 1f/25 * mColorDiscRadius;
        mPointerPaint.setARGB(128, 0, 0, 0);
        canvas.drawCircle(xToDraw(colorPointX), yToDraw(colorPointY), pointerRadius, mPointerPaint);

    }

    @Override
//    @SuppressWarnings("fallthrough")
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        float cx = xToCart(x);
        float cy = yToCart(y);
        double touchAngle = Math.atan2(cy, cx);
        double r = Math.sqrt(cx * cx + cy * cy);
        float hue, sat, value;

        switch (action) {
        case MotionEvent.ACTION_DOWN:

            // Detect click on initial color view
            if (onInitialView(x, y)) {
                setColor(mColorInitial);
                return false;
            }
            // Use down event to decide the effect of following move events
            mHueTouch = (r > mInnerRingRadius);

        case MotionEvent.ACTION_MOVE:

            // Handle both down and move events
            if (mHueTouch) {
                hue = (float) (Math.toDegrees(touchAngle));
                setHue((hue - HUE_OFFSET + 360) % 360);
                invalidate();
            }
            else {
                if (mShape == SHAPE_DISC) {
                    // color disc
                    hue = (float) (Math.toDegrees(touchAngle));
                    value = (float) (r / mColorDiscRadius);
                    setHue((hue - HUE_OFFSET + 360) % 360);
                    setValue(Math.max(0, Math.min(1f, value)));
                }
                else if (mShape == SHAPE_SQUARE) {
                    // color square
                    sat = cy / (2 * mSquareRadius) + 0.5f;
                    value = cx / (2 * mSquareRadius) + 0.5f;
                    setSaturation(Math.max(0, Math.min(1f, sat)));
                    setValue(Math.max(0, Math.min(1f, value)));
                }
                else { //mShape == SHAPE_DIAMOND
                    // color diamond
                    diamondXYtoHSV(cx, cy, mColorDiscRadius, mHSV);
                    sat = mHSV[1];
                    value = mHSV[2];
                    setSaturation(Math.max(0, Math.min(1f, sat)));
                    setValue(Math.max(0, Math.min(1f, value)));
                }

                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private float xToDraw(double x) {
        int centerX = getWidth() / 2;
        return (float)x + centerX;
    }

    private float yToDraw(double y) {
        int centerY = getHeight() / 2;
        return (float)(-y + centerY);
    }

    private float xToCart(double x) {
        int centerX = getWidth() / 2;
        return (float)x - centerX;
    }

    private float yToCart(double y) {
        int centerY = getHeight() / 2;
        return -(float)(y - centerY);
    }

    private Bitmap createColorDiscBitmap(int radius) {
        int centerColor, edgeColor;
        Bitmap bitmap = Bitmap.createBitmap(2 * radius, 2 * radius, Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        mHSV[0] = 0; mHSV[1] = 1; mHSV[2] = 1;   //red
        SweepGradient sweepGradient = new SweepGradient(radius, radius, getColors(mHSV), null);
        mColorPaint.setShader(sweepGradient);
        canvas.drawCircle(radius, radius, radius, mColorPaint);

        mHSV[0] = 0; mHSV[1] = 0; mHSV[2] = 1;   //white
        centerColor = Color.HSVToColor(255, mHSV);
        edgeColor = Color.HSVToColor(0, mHSV);
        RadialGradient radialGradient = new RadialGradient(radius, radius, radius, centerColor, edgeColor, TileMode.CLAMP);
        mColorPaint.setShader(radialGradient);
        canvas.drawCircle(radius, radius, radius, mColorPaint);

        return bitmap;
    }

    private void drawColorSquare(Canvas canvas) {
        int pureColor, brightColor, darkColor, transparentColor;

        // pureColor
        mHSV[0] = mColorHSV[0];
        mHSV[1] = 1; mHSV[2] = 1;
        pureColor = Color.HSVToColor(mHSV);
        // brightColor
        mHSV[1] = 0; mHSV[2] = 1;
        brightColor = Color.HSVToColor(mHSV);
        // darkColor
        mHSV[1] = 1; mHSV[2] = 0;
        darkColor = Color.HSVToColor(255, mHSV);
        // alphaColor
        mHSV[1] = 0; mHSV[2] = 0;
        transparentColor = Color.HSVToColor(0, mHSV);

        // drawn without compose shader, but looks worse
        Shader gradient1 = new LinearGradient(mSquareRect.right, mSquareRect.bottom, mSquareRect.right, mSquareRect.top, brightColor, pureColor, TileMode.CLAMP);
        Shader gradient2 = new LinearGradient(mSquareRect.right, mSquareRect.bottom, mSquareRect.left, mSquareRect.bottom, transparentColor, darkColor, TileMode.CLAMP);

        mColorPaint.setShader(gradient1);
        canvas.drawRect(mSquareRect, mColorPaint);
        mColorPaint.setShader(gradient2);
        canvas.drawRect(mSquareRect, mColorPaint);
    }

    private void drawPointerArrow(Canvas canvas, double tipAngle, double radius) {
        double leftAngle = tipAngle + Math.PI / 96;
        double rightAngle = tipAngle - Math.PI / 96;

        float tipAngleX = xToDraw(Math.cos(tipAngle) * radius);
        float tipAngleY = yToDraw(Math.sin(tipAngle) * radius);
        float leftAngleX = xToDraw(Math.cos(leftAngle) * (radius + mHueArrowSize));
        float leftAngleY = yToDraw(Math.sin(leftAngle) * (radius + mHueArrowSize));
        float rightAngleX = xToDraw(Math.cos(rightAngle) * (radius + mHueArrowSize));
        float rightAngleY = yToDraw(Math.sin(rightAngle) * (radius + mHueArrowSize));

        mHueArrowPath.reset();
        mHueArrowPath.moveTo(tipAngleX, tipAngleY);
        mHueArrowPath.lineTo(rightAngleX, rightAngleY);
        mHueArrowPath.lineTo(leftAngleX, leftAngleY);
        mHueArrowPath.lineTo(tipAngleX, tipAngleY);

        mHueArrowPaint.setColor(Color.HSVToColor(mColorHSV));
        mHueArrowPaint.setStyle(Style.FILL);
        canvas.drawPath(mHueArrowPath, mHueArrowPaint);

        mHueArrowPaint.setStyle(Style.STROKE);
        mHueArrowPaint.setColor(Color.BLACK);
        canvas.drawPath(mHueArrowPath, mHueArrowPaint);

    }

    private void drawColorDiamond(Canvas canvas, double hueAngle, double radius) {
        int pureColor, brightColor, darkColor;
        double leftAngle = hueAngle + Math.toRadians(120);
        double rightAngle = hueAngle - Math.toRadians(120);

        float tipAngleX = xToDraw(Math.cos(hueAngle) * radius);
        float tipAngleY = yToDraw(Math.sin(hueAngle) * radius);
        float leftAngleX = xToDraw(Math.cos(leftAngle) * radius);
        float leftAngleY = yToDraw(Math.sin(leftAngle) * radius);
        float rightAngleX = xToDraw(Math.cos(rightAngle) * radius);
        float rightAngleY = yToDraw(Math.sin(rightAngle) * radius);

        mTrianglePath.reset();
        mTrianglePath.moveTo(tipAngleX, tipAngleY);
        mTrianglePath.lineTo(rightAngleX, rightAngleY);
        mTrianglePath.lineTo(leftAngleX, leftAngleY);
        mTrianglePath.lineTo(tipAngleX, tipAngleY);

        // pureColor
        mHSV[0] = mColorHSV[0];
        mHSV[1] = 1; mHSV[2] = 1;
        pureColor = Color.HSVToColor(mHSV);
        // brightColor
        mHSV[1] = 0; mHSV[2] = 1;
        brightColor = Color.HSVToColor(mHSV);
        // darkColor
        mHSV[1] = 1; mHSV[2] = 0;
        darkColor = Color.HSVToColor(mHSV);

        Shader gradient1 = new LinearGradient(leftAngleX, leftAngleY, tipAngleX, tipAngleY, brightColor, pureColor, TileMode.CLAMP);
        Shader gradient2 = new LinearGradient(leftAngleX, leftAngleY, rightAngleX, rightAngleY, brightColor, darkColor, TileMode.CLAMP);
        ComposeShader composeShader = new ComposeShader(gradient1, gradient2, PorterDuff.Mode.MULTIPLY);

        mColorPaint.setShader(composeShader);
        canvas.drawPath(mTrianglePath, mColorPaint);
    }

    private void drawSelectableDiamond(Canvas canvas, double hueAngle, double radius) {
        double backAngle = hueAngle + Math.toRadians(180);
        double leftAngle = hueAngle + Math.toRadians(60);
        double rightAngle = hueAngle - Math.toRadians(60);
        double innerRadius = Math.sin(Math.toRadians(30)) * radius;

        float backAngleX = xToDraw(Math.cos(backAngle) * innerRadius);
        float backAngleY = yToDraw(Math.sin(backAngle) * innerRadius);
        float leftAngleX = xToDraw(Math.cos(leftAngle) * innerRadius);
        float leftAngleY = yToDraw(Math.sin(leftAngle) * innerRadius);
        float rightAngleX = xToDraw(Math.cos(rightAngle) * innerRadius);
        float rightAngleY = yToDraw(Math.sin(rightAngle) * innerRadius);

        mTrianglePath.reset();
        mTrianglePath.moveTo(rightAngleX, rightAngleY);
        mTrianglePath.lineTo(backAngleX, backAngleY);
        mTrianglePath.lineTo(leftAngleX, leftAngleY);

        // diamond lines to delimit selectable area
        mPointerPaint.setARGB(128, 0, 0, 0);
        canvas.drawPath(mTrianglePath, mPointerPaint);
    }

    private void diamondHSVtoXY(float sat, float value, float radius, double[] xy) {
        double hueAngle = Math.toRadians(getHue() + HUE_OFFSET);
        double hueAngleX = Math.cos(hueAngle);
        double hueAngleY = Math.sin(hueAngle);
        double satAngle = hueAngle - Math.toRadians(30);
        double valueAngle = Math.toRadians(60) - hueAngle;
        double triangleSide = 2 * radius * Math.cos(Math.toRadians(30));

        double pointTriangleX = (1 - sat) * Math.cos(satAngle) + (1 - value) * Math.sin(valueAngle);
        double pointTriangleY = (1 - sat) * Math.sin(satAngle) + (1 - value) * Math.cos(valueAngle);
        xy[0] = (hueAngleX * radius - pointTriangleX * triangleSide);
        xy[1] = (hueAngleY * radius - pointTriangleY * triangleSide);
    }

    private void diamondXYtoHSV(float cx, float cy, float radius,float[] hsv) {
        double hueAngle = Math.toRadians(getHue() + HUE_OFFSET);
        double hueAngleX = Math.cos(hueAngle);
        double hueAngleY = Math.sin(hueAngle);
        double satAngle = hueAngle - Math.toRadians(30);
        double valueAngle = Math.toRadians(60) - hueAngle;
        double triangleSide = 2 * radius * Math.cos(Math.toRadians(30));

        double hx = hueAngleX * radius;
        double hy = hueAngleY * radius;
        double valueDiv;
        double satDiv;
        double value = 0;
        double sat = 0;

        // Inverse function of diamondHSVtoXY (TODO: Too complex!, use rotation matrix?)
        if (Math.cos(satAngle) != 0) {
            valueDiv = Math.cos(valueAngle) - Math.sin(valueAngle) * Math.tan(satAngle);
            if (valueDiv != 0) {
                value = ((hy - cy) - (hx - cx) * Math.tan(satAngle)) / valueDiv;
                sat = (hx - cx) / Math.cos(satAngle) - value * Math.sin(valueAngle) / Math.cos(satAngle);
            }
        }
        if (Math.sin(valueAngle) != 0) {
            satDiv = Math.sin(satAngle) - Math.cos(satAngle) / Math.tan(valueAngle);
            if (satDiv != 0) {
                sat = ((hy - cy) - (hx - cx) / Math.tan(valueAngle)) / satDiv;
                if (value == 0) {
                    // Previous value calculation is better. This one causes glitches
                    value = (hx - cx) / Math.sin(valueAngle) - sat * Math.cos(satAngle) / Math.sin(valueAngle);
                }
            }
        }
        hsv[1] = (float)(1 - sat / triangleSide);
        hsv[2] = (float)(1 - value / triangleSide);
    }

    private boolean onInitialView(float eventX, float eventY) {
        float centerX = getWidth() - mInitialViewRadius;
        float centerY = mInitialViewRadius;
        double distance = Math.sqrt( (eventX - centerX) * (eventX - centerX) + (eventY - centerY) * (eventY - centerY) );
        return (distance <= mInitialViewRadius);
    }

    public void setColor(int color) {
        Color.colorToHSV(color, mColorHSV);
        invalidate();
    }

    public void setInitialColor(int color) {
        mColorInitial = color;
        setColor(color);
    }

    public void setHue(float hue) {
        mColorHSV[0] = hue;
    }

    public void setSaturation(float sat) {
        // Adjustable Saturation: from MIN_VALUE to 1
        mColorHSV[1] = Math.max(MIN_VALUE, sat);
    }

    public void setValue(float value) {
        // Adjustable Value: from MIN_VALUE to 1
        mColorHSV[2] = Math.max(MIN_VALUE, value);
    }

    public float getHue() {
        return mColorHSV[0];
    }

    public float getSaturation() {
        return mColorHSV[1];
    }

    public float getValue() {
        return mColorHSV[2];
    }

    public int getColor() {
        return Color.HSVToColor(mColorHSV);
    }

    private int[] getColors(float[] hsvReference) {
        int colorCount = 12;
        int colorAngleStep = 360 / colorCount;
        int colors[] = new int[colorCount + 1];
        for (int i = 0; i < colors.length; i++) {
            // (360 - hue) because sweepGradient goes anti-clockwise.
            mHSV[0] = ((360 - HUE_OFFSET) + (360 - i * colorAngleStep)) % 360;
            mHSV[1] = hsvReference[1];
            mHSV[2] = hsvReference[2];
            colors[i] = Color.HSVToColor(mHSV);
        }
        colors[colorCount] = colors[0];

        return colors;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putFloatArray("color", mColorHSV);
        state.putInt("previous", mColorInitial);
        state.putParcelable("super", super.onSaveInstanceState());
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mColorHSV = bundle.getFloatArray("color");
            mColorInitial = bundle.getInt("previous");
            super.onRestoreInstanceState(bundle.getParcelable("super"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

}

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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.dftec.planetcon.R;

public class ColorPickerDialog extends AlertDialog {

    private final static int J_MAX = 2;   // columns
    private final static int I_MAX = 6;   // rows
    private final OnColorSelectedListener mOnColorSelectedListener;
    private final ColorPicker mColorPickerView;
    private int mInitialColor;

    public ColorPickerDialog(Context context, int defaultColor, int initialColor, OnColorSelectedListener onColorSelectedListener) {
        super(context);
        final Resources res = context.getResources();
        final int boxMax = res.getDimensionPixelOffset(R.dimen.color_box);
        final int margin = res.getDimensionPixelOffset(R.dimen.view_margin_small);

        mOnColorSelectedListener = onColorSelectedListener;
        mInitialColor = initialColor;

        setTitle(context.getString(R.string.dialog_color));
        setIcon(0);
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), onDialogClickListener);
        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), onDialogClickListener);

        // Main layout with fixed size
        LinearLayout mainLayout = new LinearLayout(context) {

            @Override
            public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                int w = View.MeasureSpec.getSize(widthMeasureSpec);
                int h = View.MeasureSpec.getSize(heightMeasureSpec);

                // Force squared remaining space for color picker
                int boxSize = (int)Math.ceil(Math.min((double) w / (I_MAX + J_MAX), (double) h / I_MAX));
                boxSize = Math.min(boxSize, boxMax);
                w = Math.min(w, boxSize * (I_MAX + J_MAX));
                h = Math.min(h, boxSize * I_MAX);
                setMeasuredDimension(w, h);
            }

        };
        LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mainLayout.setLayoutParams(linearParams);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setBaselineAligned(false);

        // Preset colors
        addPresetColorViews(context, mainLayout);

        // Color picker
        // For some reason, MATCH_PARENT here does not work properly in some devices
        linearParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        linearParams.setMargins(margin, margin, margin, margin);
        mColorPickerView = new ColorPicker(context);
        mColorPickerView.setInitialColor(mInitialColor);
        mColorPickerView.setColor(defaultColor);
        mainLayout.addView(mColorPickerView, linearParams);

        // Layout needed to center the main layout inside the dialog
        linearParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(linearParams);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(mainLayout);
        setView(linearLayout, margin, margin, margin, margin);
    }

    private final OnClickListener onDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case BUTTON_POSITIVE:
                int selectedColor = mColorPickerView.getColor();
                mOnColorSelectedListener.onColorSelected(selectedColor);
                break;
            case BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
            }
        }
    };

    private final Button.OnClickListener onButtonClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            int color;
            try {
                // use the content description to store/retrieve the color of the box
                color = Color.parseColor(v.getContentDescription().toString());
                mColorPickerView.setColor(color);
            }
            catch (IllegalArgumentException e) {
                mColorPickerView.setColor(mInitialColor);
            }
        }
    };

    public interface OnColorSelectedListener {
        public void onColorSelected(int color);
    }

    private void addPresetColorViews( Context context, LinearLayout mainLayout) {
        final Resources res = context.getResources();
        final int margin = res.getDimensionPixelOffset(R.dimen.view_margin_small);
        final int boxMax = res.getDimensionPixelOffset(R.dimen.color_box);
        int i, j;
        LinearLayout colorLayout;
        Button colorView;
        TypedArray presetColors = res.obtainTypedArray(R.array.pref_players_color_values);

        // set params for colorView boxes
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        layoutParams.setMargins(margin, margin, margin, margin);

        for (j = 0; j < J_MAX; j++) {
            colorLayout = new LinearLayout(context);
            // set params for colorLayout columns
            colorLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
            colorLayout.setOrientation(LinearLayout.VERTICAL);
            colorLayout.setGravity(Gravity.CENTER);
            for (i = 0; (i < I_MAX) && ((I_MAX * j) + i < presetColors.length()); i++) {
                colorView = new Button(context) {

                    @Override
                    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                        int w = View.MeasureSpec.getSize(widthMeasureSpec);
                        int h = View.MeasureSpec.getSize(heightMeasureSpec);
                        if (Math.abs(w - h) > margin) {
                            int boxSize = Math.min(w, h);
                            // Squared color box
                            w = Math.min(boxSize, boxMax);
                            h = Math.min(boxSize, boxMax);
                        } else {
                            // Almost squared color box
                            w = Math.min(w, boxMax);
                            h = Math.min(h, boxMax);
                        }
                        setMeasuredDimension(w, h);
                    }

                };
                try {
                    String sColor = presetColors.getString((I_MAX * j) + i);
                    colorView.setBackgroundColor(Color.parseColor(sColor));
                    // use the content description to store/retrieve the color of the box
                    colorView.setContentDescription(sColor);
                }
                catch (IllegalArgumentException e) {
                    colorView.setBackgroundColor(mInitialColor);
                }
                colorView.setOnClickListener(onButtonClickListener);
                colorView.setWidth(boxMax);
                colorView.setHeight(boxMax);
                colorLayout.addView(colorView, layoutParams);
            }
            mainLayout.addView(colorLayout);
        }
        presetColors.recycle();
    }
}

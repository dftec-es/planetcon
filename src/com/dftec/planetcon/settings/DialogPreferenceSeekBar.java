/*
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
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class DialogPreferenceSeekBar extends DialogPreference implements OnSeekBarChangeListener {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final int DEFAULT_VALUE = 0;
    private static final int DEFAULT_MAX = 100;
    private final Context mContext;
    private SeekBar mSeekBar;
    private TextView mViewValue;
    private int mDefault, mMax, mValue;

    public DialogPreferenceSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        // Get attributes from preferences xml
        mDefault = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", DEFAULT_VALUE);
        mMax = attrs.getAttributeIntValue(ANDROID_NS, "max", DEFAULT_MAX);
        mValue = DEFAULT_VALUE;
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // Layout returned as dialog view
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Add text view to show the current value
        mViewValue = new TextView(mContext);
        mViewValue.setGravity(Gravity.CENTER_HORIZONTAL);
        mViewValue.setText(String.valueOf(mValue));
        layout.addView(mViewValue, params);

        // Add seek bar
        mSeekBar = new SeekBar(mContext);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
        // Set progress before change listener
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, params);

        return layout;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // Store preference value
        if (positiveResult && shouldPersist()) {
            persistInt(mValue);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            // Restore state
            mValue = getPersistedInt(mDefault);
        } else {
            // Set state
            mValue = (Integer) defaultValue;
            if (shouldPersist()) persistInt(mValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, mDefault);
    }

    @Override
    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
        // Update and show current value
        mValue = progress;
        mViewValue.setText(String.valueOf(mValue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
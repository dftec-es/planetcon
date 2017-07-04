/*
 * Copyright 2008 The Android Open Source Project
 * Copyright 2011 Viktor Reiser <viktorreiser@gmx.de>
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
package com.dftec.planetcon.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

/**
 * Integer number preference (<b>Beta +</b>).<br>
 * <br>
 * XML attributes will be delegated to {@link NumberPicker}.<br>
 * <b>Note</b>: This preference persists an {@code Integer} not a {@code String}.<br>
 * <br>
 * Dependent preferences will be disabled when value is {@code 0}.<br>
 * <br>
 * <i>Depends on</i>: {@link NumberPicker}
 *
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class NumberPickerPreference extends DialogPreference {

    // PRIVATE ====================================================================================

    /** Number picker view which contains input. */
    private NumberPicker mPicker;

    /** Layout which contains number picker view and placed as margin layout into dialog. */
    private LinearLayout mPickerLayout;

    /** Value of preference. */
    private int mNumber;

    // PUBLIC =====================================================================================

    /**
     * Get {@link NumberPicker} containing input of preference.
     *
     * @return {@link NumberPicker} containing input of preference
     */
    public NumberPicker getNumberPicker() {
        return mPicker;
    }

    /**
     * Saves number to {@link SharedPreferences}.
     *
     * @param number
     *            number to save
     */
    public void setNumber(int number) {
        final boolean wasBlocking = shouldDisableDependents();

        mNumber = number;

        persistInt(number);

        final boolean isBlocking = shouldDisableDependents();

        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    /**
     * Gets current set number of preference.
     *
     * @return current preference value
     */
    public int getNumber() {
        return mNumber;
    }

    // OVERRIDDEN =================================================================================

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(attrs);
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Add number picker view to dialog.
     */
    @Override
    protected View onCreateDialogView() {
        ViewParent parent = mPickerLayout.getParent();

        if (parent != null) {
            ((ViewGroup) parent).removeView(mPickerLayout);
        }

        return mPickerLayout;
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Set entered value by keyboard to number picker.
     */
    @Override
    public void onClick(DialogInterface di, int which) {
        mPicker.clearFocus();

        super.onClick(di, which);
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Set edit text value on bind.
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mPicker.setCurrent(getNumber());
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Dependent values should be disabled when value is {@code 0}.
     */
    @Override
    public boolean shouldDisableDependents() {
        return mNumber == 0 || super.shouldDisableDependents();
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Get default value.
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Set initial value.
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            setNumber(getPersistedInt(0));
        }

        if (defaultValue != null) {
            setNumber((Integer) defaultValue);
        }
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Save state.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        if (isPersistent()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.number = getNumber();
        return myState;
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Restore state.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setNumber(myState.number);
    }

    /**
     * <i>Overridden for internal use!</i><br>
     * <br>
     * Persist value (if desired).
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int result = mPicker.getCurrent();

            if (callChangeListener(result)) {
                setNumber(result);
            }
        }
    }

    // PRIVATE ====================================================================================

    /**
     * Initialize preference view and it's behavior.
     */
    private void initialize(AttributeSet attrs) {
        // setup edit text
        mPicker = new NumberPicker(getContext(), attrs);
        mPicker.setEnabled(true);
        mPicker.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // setup layout for edit text
        int dip = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                getContext().getResources().getDisplayMetrics()) + 0.5f);

        mPickerLayout = new LinearLayout(getContext());
        mPickerLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mPickerLayout.setPadding(dip, dip, dip, dip);
        mPickerLayout.addView(mPicker);
    }

    /**
     * Used to re/store state of preference.
     *
     * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
     */
    private static class SavedState extends BaseSavedState {

        Integer number;

        public SavedState(Parcel source) {
            super(source);
            number = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(number);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}

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
package com.dftec.planetcon.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.dftec.planetcon.R;

public class TextAdapter extends BaseAdapter {
    public static final int INFO_NO = -2;
    public static final int INFO_PREV = -1;
    // Valid values from 0 to 3 (linked to index of tabs)
    public static final int INFO_ARRIVALS = 0;
    public static final int INFO_FLEETS = 1;
    public static final int INFO_STATS = 2;
    public static final int INFO_THREATS = 3;
    public static final int INFO_NEXT = 4;
    private final Context mContext;
    private final Resources mRes;
    private final TextBuilder mInfoBuilder;
    private final SpannableStringBuilder mBuilderColumn;
    private int mStateInfoGame = INFO_NO;
    private int mTextSize = 0;
    private int mNumCols = 1;
    private int mNumRows = 1;

    public TextAdapter(Context context, TextBuilder builder) {
        super();
        mContext = context;
        mRes = context.getResources();
        mInfoBuilder = builder;
        mBuilderColumn = new SpannableStringBuilder();
    }

    public void setState(int state) {
        mStateInfoGame = state;
    }

    public void setTextSize(int textSize) {
        mTextSize = textSize;
    }

    public CharSequence getTitle() {
        switch (mStateInfoGame) {
        case INFO_THREATS:
            return mRes.getString(R.string.info_threats).toUpperCase();
        case INFO_STATS:
            return mRes.getString(R.string.info_stats).toUpperCase();
        case INFO_FLEETS:
            return mRes.getString(R.string.info_fleets).toUpperCase();
        case INFO_ARRIVALS:
            return mRes.getString(R.string.info_arrivals).toUpperCase();
        }
        return "";
    }

    public int getSize() {
        switch (mStateInfoGame) {
        case INFO_THREATS:
            return mInfoBuilder.getInfoThreats().size();
        case INFO_STATS:
            return mInfoBuilder.getInfoStats().size();
        case INFO_FLEETS:
            return mInfoBuilder.getInfoFleets().size();
        case INFO_ARRIVALS:
            return mInfoBuilder.getInfoArrivals().size();
        }
        return 0;
    }

    @Override
    public int getCount() {
        if (mStateInfoGame == INFO_STATS) {
            return 0;
        } else {
            return getSize();
        }
    }

    @Override
    public CharSequence getItem(int position) {
        if (position < getSize()) {
            switch (mStateInfoGame) {
            case INFO_THREATS:
                return mInfoBuilder.getInfoThreats().get(position);
            case INFO_STATS:
                return mInfoBuilder.getInfoStats().get(position);
            case INFO_FLEETS:
                return mInfoBuilder.getInfoFleets().get(position);
            case INFO_ARRIVALS:
                return mInfoBuilder.getInfoArrivals().get(position);
            }
        }
        return "";
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        //no dividers between items
        return false;
    }

    @Override
    public boolean isEnabled(int position)
    {
        //non-selectable non-clickable item
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // create a new view for each item
        TextView colTextView;
        if (convertView == null) {
            // if it's not recycled, initialize the view
            colTextView = createTextView();
            colTextView.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.WRAP_CONTENT, GridView.LayoutParams.WRAP_CONTENT));
        } else {
            colTextView = (TextView) convertView;
            if (mTextSize > 0) colTextView.setTextSize(mTextSize);
        }
        colTextView.setGravity(Gravity.LEFT);
        colTextView.setText(getItem(position));
        return colTextView;
    }

    public void showInfoGame(GridView layout) {
        int colWidth = 0;

        if (getCount() > 0) {
            // Use the first item to measure the column width
            TextView tempView = createTextView(getItem(0));
            tempView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tempView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            colWidth = tempView.getMeasuredWidth();
        }
        layout.setColumnWidth(colWidth);
        layout.requestLayout();
    }

    private TextView createTextView(CharSequence info) {
        TextView textView = createTextView();
        textView.setText(info);
        return textView;
    }

    private TextView createTextView() {
        TextView textView = new TextView(mContext);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextColor(Color.WHITE);
        textView.setHorizontallyScrolling(true);
        if (mTextSize > 0) textView.setTextSize(mTextSize);
        //textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        //textView.setPadding(0, 0, 2, 0);
        return textView;
    }


////// Table Adapter //////

    public void showInfoGame(TableLayout table) {
        table.removeAllViews();
        if (mStateInfoGame == INFO_STATS) {
            TextView textView;
            mNumCols = measureNumColumns(table, getItem(0), mInfoBuilder.getItemStatsHeader());
            mNumCols = Math.max(2, mNumCols);
            mNumCols = Math.min(mNumCols, getSize() + 1);
            mNumRows = (int)Math.ceil( (double)getSize() / (mNumCols - 1) );
            TableRow row = new TableRow(mContext);
            table.addView(row, TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
            for (int c = 0; c < mNumCols; c++) {
                textView = createTextView(getColumn(c-1));
                if (c > 0) textView.setGravity(Gravity.CENTER);
                row.addView(textView, TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private int measureNumColumns(ViewGroup layout, CharSequence item, CharSequence title) {
        int colWidth = 0;
        int viewWidth = 0;

        if (getSize() > 0) {
            // Use the first item to measure the column width
            TextView tempView = createTextView(item);
            tempView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tempView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            colWidth = tempView.getMeasuredWidth() + 4;
            // Stack overflow when called just after resized layout !?
            viewWidth = layout.getWidth();
            // If table layout not inflated yet, use device width
            if (viewWidth <= 0) {
                viewWidth = mRes.getDisplayMetrics().widthPixels - 2 * mRes.getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
            }
            if (title != null) {
                tempView = createTextView(title);
                tempView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tempView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
                viewWidth -= tempView.getMeasuredWidth();
            }
        }
        if (colWidth != 0) {
            return Math.max(1, viewWidth/colWidth);
        } else {
            return 1;
        }
    }

    private CharSequence getColumn(int c) {
        int index;

        mBuilderColumn.delete(0, mBuilderColumn.length());
        for (int r = 0; r < mNumRows; r++) {
            if (c < 0) {
                mBuilderColumn.append(mInfoBuilder.getItemStatsHeader());
            } else {
                index = r + c * mNumRows;
                mBuilderColumn.append(getItem(index));
            }
        }
        return mBuilderColumn.subSequence(0, mBuilderColumn.length());
    }
//
//    private int transposed(int position) {
//        int numCols = Math.max(1, mNumCols);
//        int numRows = Math.max(1, mNumRows);
//        int col = position % numCols;
//        int row = position / numCols;
//        return row + col * numRows;
//    }

}

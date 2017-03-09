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
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class InfoView extends FrameLayout {
    private IEventListenerInfoView mEventListener;
    private final GestureDetector mGestureDetector;
    
    public InfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, mGestureListener);
    }

    public interface IEventListenerInfoView {
        // Events sent to the Activity that implements this interface
        public void onSingleTap();
        public void onLongPress();
        public void onFling();
    }

    public void setEventListener(IEventListenerInfoView eventListener) {
        mEventListener = eventListener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    // Capture touch events before children views
    public boolean dispatchTouchEvent(MotionEvent event){
        super.dispatchTouchEvent(event);    
        boolean gesture = mGestureDetector.onTouchEvent(event);

        return gesture;  
    }

//    @Override
//    public boolean onInterceptTouchEvent (MotionEvent event){
//        super.onInterceptTouchEvent(event);
//        boolean gesture = mGestureDetector.onTouchEvent(event);
//
//        return gesture;
//    }
//
//    @Override 
//    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
//        boolean gesture = mGestureDetector.onTouchEvent(event);
//
//        return gesture;
//    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener
    = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (mEventListener != null) mEventListener.onSingleTap();
            //Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
            return false;
        }

//        @Override
//        public boolean onSingleTapConfirmed(MotionEvent event) {
//            //Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
//            return false;
//        }
  
//        @Override
//        public boolean onDoubleTap(MotionEvent event) {
//            if (mEventListener != null) mEventListener.onDoubleTap();
//            //Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
//            return false;
//        }

        @Override
        public void onLongPress(MotionEvent event) {
            if (mEventListener != null) mEventListener.onLongPress();
        }
  
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //if (mLayoutExpanded) {
                // fling to left 
                if ( (e1.getX() - e2.getX()) > Math.abs(e2.getY() - e1.getY()) ) {
                    if (mEventListener != null) mEventListener.onFling();
                }
            //} else {
                // fling to right 
                if ( (e2.getX() - e1.getX()) > Math.abs(e2.getY() - e1.getY()) ) {
                    if (mEventListener != null) mEventListener.onFling();
                }
            //}
            //Log.d(DEBUG_TAG, "onFling: " + velocityX + ", " + velocityY);
            return true;
        }

//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            //Log.d(DEBUG_TAG, "onScroll: " + distanceX + ", " + distanceY);
//            return false;
//        }
    };
}

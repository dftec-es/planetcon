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
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.dftec.planetcon.R;
import com.dftec.planetcon.activities.SettingsActivity;
import com.dftec.planetcon.data.FleetData;
import com.dftec.planetcon.data.GalaxyData;
import com.dftec.planetcon.data.PlanetData;
import com.dftec.planetcon.data.PlayerData;

public class GalaxyView extends View {

    private static final String DEBUG_TAG = "Gestures";
    private static final float SCALE_MIN = 0.5f;
    private static final float SCALE_NO = 1.0f;
    private static final float SCALE_TAP = 1.1f;
    private static final int SCALE_DIV = 4;         // Tiles shown at max scale
    private static final float RADIUS_MAX = 0.5f;   // Planet radius with Max production
    private static final int GESTURE_DRAG = 1;
    private static final int DRAG_MARGIN = 32;      // Distance to border in pixels
    private static final int LINE_WIDTH = 0;        // Always 1 px wide (no matter canvas scale)
    private static final float WING_LENGTH = 1.25f; // Ship shape: ratio wing to ship length
    private static final float WING_ARC = 0.4f;     // Ship shape: wing arc in radians (22 degrees)
    private static final int ANIMATION_FPS = 25;
    private static final int ANIMATION_DURATION = 4000;

    private final Context mContext;
    private OnEventListenerGalaxyView mEventListener;

    // Gesture variables
    private final ScaleGestureDetector mScaleGestureDetector;
    private final GestureDetector mGestureDetector;

    private boolean mMultitouch = false;    // Multiple touch pointers
    private int mGesture = 0;               // Custom gesture
    private float mX = 0;                   // Touch X coordinate
    private float mY = 0;                   // Touch Y coordinate
    private float mDragX = 0;               // Drag gesture: touch X coordinate
    private float mDragY = 0;               // Drag gesture: touch Y coordinate
    private float mScaleX = 0;              // Scale gesture: touch X coordinate
    private float mScaleY = 0;              // Scale gesture: touch Y coordinate
    private float mScaleSpan = 0;           // Scale Gesture: Initial Span value (for zoom)
    private float mScaleOldX = 0;           // Scale gesture: Initial center X coordinate
    private float mScaleOldY = 0;           // Scale gesture: Initial center Y coordinate
    private float mDownX = 0;               // Gesture: Initial X coordinate
    private float mDownY = 0;               // Gesture: Initial Y coordinate
    private float mPointerX = 0;            // Pointer drawn: X coordinate
    private float mPointerY = 0;            // Pointer drawn: Y coordinate
    private float mCanvasScrollX = 0;       // Scroll the canvas
    private float mCanvasScrollY = 0;       // Scroll the canvas
    private float mCanvasPivotX = 0;        // Pivot to Scale the canvas
    private float mCanvasPivotY = 0;        // Pivot to Scale the canvas
    private float mCanvasScale = SCALE_NO;  // Scale the canvas
    private float mScaleMax = 0;            // Max scale (over one)
    private float mZoomScale = 0;           // Zoom for Double tap
    private int mTile = 0;                  // Size of one tile: width=height
    private int mSize = 0;                  // Size of the board: width=height
    private Rect mBoundsView;               // Canvas coordinates of the current viewport (scaled/translated) 
    private Rect mBoundsDraw;               // Canvas coordinates of the total drawable canvas 

    // Draw variables
    private boolean mDrawingDistances = false;
    private boolean mDrawingDefenses = false;
    private boolean mDrawingShips = false;
    private boolean mDrawingFleets = true;
    private boolean mDrawingArrivals = false;
    private boolean mDrawingThreats = false;
    
    private int mGridAlpha = 0;
    private int mTextAlpha = 0;
    private int mTextSize = 0;
    private boolean mScrollBoard = true;

//    private final GradientDrawable mRadialDrawable;
    private RadialGradient mRadialGradient;
    private LinearGradient mLinearGradient;
    private SweepGradient mSweepGradient;
    private final Path mArrowPath;

    private final Paint mPointerPaint;
    private final Paint mLinePaint;
    private final Paint mGradientLinePaint;
    private final Paint mGradientCirclePaint;
    private final Paint mTextPaint;
    private final float[] mPoint;
    private final float[] mVertices;
    private final int[] mColors;
    private String sShips;
    private String sBuilt;
    private String sDefense;
    private String sDistance;

    // Animate variables
    private boolean mAnimated = false;
    private double mAnimationTime = 0;
    private long mAnimationStartTime = 0;

    // Galaxy variables
    private GalaxyData mGalaxy;
    private PlanetData mFocusedPlanet;
    private PlanetData mSelectedPlanet;
    private PlanetData mTargetedPlanet;
    private FleetData mPreviousFleet;


////// Constructors //////

    public GalaxyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestFocus();
        mContext = context;

        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mGestureDetector = new GestureDetector(context, mGestureListener);

        mTextPaint = new Paint();

        mPointerPaint = new Paint();
        mPointerPaint.setStrokeWidth(LINE_WIDTH);
        mPointerPaint.setStyle(Style.FILL_AND_STROKE);
        mPointerPaint.setAntiAlias(true);

        mLinePaint = new Paint();
        mLinePaint.setStrokeWidth(LINE_WIDTH);
        mLinePaint.setStyle(Style.FILL_AND_STROKE);
        mLinePaint.setAntiAlias(true);

        mGradientLinePaint = new Paint();
        mGradientLinePaint.setStrokeWidth(LINE_WIDTH);
        mGradientLinePaint.setStyle(Style.FILL_AND_STROKE);
        mGradientLinePaint.setAntiAlias(true);

        mGradientCirclePaint = new Paint();
        mGradientCirclePaint.setStrokeWidth(LINE_WIDTH);
        mGradientCirclePaint.setAntiAlias(true);

        mPoint = new float[2];
        mVertices = new float[8];
        mColors = new int[9];
        
        mArrowPath = new Path();
        mBoundsView = new Rect();
        mBoundsDraw = new Rect();

        sShips = "";
        sBuilt = "";
        sDefense = "";

//        mRadialDrawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
//                new int[] { Color.WHITE, Color.TRANSPARENT });
//        mRadialDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
//        mRadialDrawable.setShape(GradientDrawable.OVAL);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    public interface OnEventListenerGalaxyView {
        // Events sent to the Activity that implements this interface
        public void onPlanetFocused();
        public void onPlanetSelected();
        public void onPlanetTargeted();
    }

    public void setEventListener(OnEventListenerGalaxyView eventListener) {
        mEventListener = eventListener;
    }

//////On Event methods //////

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Left, Top, Right, Bottom
        mBoundsDraw.set(0, 0, w, h);
        // Center galaxy board
        mCanvasPivotX = mBoundsDraw.centerX();
        mCanvasPivotY = mBoundsDraw.centerY();
        if (mGalaxy != null) {
            // Update size of one board tile
            mTile = Math.min (w, h) / mGalaxy.getMaxDiameter();
            // Update size of the board
            mSize = mTile * mGalaxy.getMaxDiameter();
        }
        centerScroll();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mGalaxy == null) {
            return;
        }

        if (mAnimated) {
            mAnimationTime = Math.abs(System.currentTimeMillis() - mAnimationStartTime) % ANIMATION_DURATION;
        }

        canvas.save();
        // Translate top-left point of current view (canvas coordinates) towards left and up (right and down if negative)
        canvas.translate(-mCanvasScrollX, -mCanvasScrollY);
        // Scale around a pivot center
        canvas.scale(mCanvasScale, mCanvasScale, mCanvasPivotX, mCanvasPivotY);
        // Get bounds of current showed view (canvas coordinates)
        canvas.getClipBounds(mBoundsView);

        // Clips the drawing operations to the window
        //canvas.clipRect(mBoundsView);
        
        // [...] stuff drawn with matrix transformations
        drawGrid(canvas);
        drawPlanets(canvas);
        drawFleets(canvas);
        drawDragShips(canvas);
        drawPointer(canvas);

        canvas.restore();
        // [...] stuff drawn without matrix transformations, as an overlay

        if (mAnimated) {
            postInvalidateDelayed(1000 / ANIMATION_FPS);
        }

    }

    @Override 
    public boolean onTouchEvent(MotionEvent event) {
        boolean scale = mScaleGestureDetector.onTouchEvent(event);
        boolean gesture = mGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        // Get the index of the pointer associated with the action.
        int index = event.getActionIndex();
        float touchX = event.getX();
        float touchY = event.getY();

        if (event.getPointerCount() > 1) {
            mMultitouch = true;
            // Multitouch event
            //touchX = MotionEventCompat.getX(event, index);
            //touchY = MotionEventCompat.getY(event, index);
            //Log.d(DEBUG_TAG, "Multitouch event");
        } else {
            mMultitouch = false;
            // Single touch event
            //touchX = event.getX());
            //touchY = event.getY();
            //Log.d(DEBUG_TAG, "Single touch event");
        }

        switch (action) {

        case MotionEvent.ACTION_DOWN:
            break;
        case MotionEvent.ACTION_MOVE:
            if ( (!mMultitouch) ) {
                mDragX = xViewToDraw(touchX);
                mDragY = yViewToDraw(touchY);
                focusPlanet(mDragX, mDragY);
                if ( (mGesture == GESTURE_DRAG)  ) {
                    dragScroll(touchX, touchY);
                }
                invalidate();
            }
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            break;
        case MotionEvent.ACTION_UP:
            if ( (!mMultitouch) && (mGesture == GESTURE_DRAG) ) {
                mDragX = xViewToDraw(touchX);
                mDragY = yViewToDraw(touchY);
                targetPlanet(mDragX, mDragY, true);
                invalidate();
            }
            mGesture = 0;
            break;
        case MotionEvent.ACTION_POINTER_UP:
            break;
        case MotionEvent.ACTION_OUTSIDE:
            break;
        case MotionEvent.ACTION_CANCEL:
            break;
        }
        return scale||gesture||super.onTouchEvent(event);
    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener
    = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent event) {
            mDownX = xViewToDraw(event.getX());
            mDownY = yViewToDraw(event.getY());
            mPointerX = mDownX;
            mPointerY = mDownY;
            focusPlanet(mDownX, mDownY);
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (!mMultitouch) {
                mX = xViewToDraw(event.getX());
                mY = yViewToDraw(event.getY());
                if (mSelectedPlanet == null) {
                    selectPlanet(mX, mY, false);
                } else {
                    targetPlanet(mX, mY, false);
                }
                invalidate();
                //Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            //Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
            return true;
        }
  
        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (!mMultitouch) {
                mX = xViewToDraw(event.getX());
                mY = yViewToDraw(event.getY());
                if (mCanvasScale == 1) {
                    // Zoom to selected point
                    updateZoom();
                    setScale(mZoomScale);
                    setScroll(mX, mY);
                } else {
                    // Unzoom and center
                    setScale(SCALE_NO);
                    centerScroll();
                }
                invalidate();
                //Log.d(DEBUG_TAG, "onDoubleTap: " + mX + ", " + mY);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            if (!mMultitouch) {
                mDragX = xViewToDraw(event.getX());
                mDragY = yViewToDraw(event.getY());
                // If pointer over existing planet, update distances
                updateDistances(mDragX, mDragY);
                selectPlanet(mDragX, mDragY, true);
                // Start drag gesture
                mGesture = GESTURE_DRAG;
                invalidate();
            }
        }
  
        @Override
        public void onShowPress(MotionEvent event) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //Log.d(DEBUG_TAG, "onFling: " + distanceX + ", " + distanceY);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if ( (!mMultitouch) && (mScrollBoard) ) {
                increaseScroll(distanceX, distanceY);
                invalidate();
                //Log.d(DEBUG_TAG, "onScroll: " + distanceX + ", " + distanceY);
            }
            return true;
        }
    };

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
    = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mScaleX = xViewToDraw(detector.getFocusX());
            mScaleY = yViewToDraw(detector.getFocusY());
            mScaleSpan = detector.getCurrentSpan();
            mScaleOldX = mBoundsView.centerX();
            mScaleOldY = mBoundsView.centerY();

            mPointerX = mScaleX;
            mPointerY = mScaleY;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float max = (float) mSize / 2;
            float span = Math.min( max, Math.abs(detector.getCurrentSpan() - mScaleSpan) );
            float x, y;

            setScale(mCanvasScale * detector.getScaleFactor());
            if (max != 0) {
                // While span is 0, keep scroll unchanged
                // When span grows, scroll towards focus (of the gesture)
                // When span reaches the max (width or height), scroll reaches the focus
                x = (mScaleOldX * (max - span) + mScaleX * span) / max;
                y = (mScaleOldY * (max - span) + mScaleY * span) / max;
                setScroll(x, y);
            }

            invalidate();
            //Log.d(DEBUG_TAG, "onScale: " + centerX + ", " + centerY);
            return true;
        }
    };

////// Draw methods //////

    private void drawPointer(Canvas canvas) {
        mPointerPaint.setColor(Color.WHITE);
        mPointerPaint.setAlpha(128 + mTextAlpha / 2);
        canvas.drawCircle(mPointerX, mPointerY, 1/mCanvasScale, mPointerPaint);
    }

    private void drawDragShips(Canvas canvas) {
        float sx, sy, tx, ty;
        if (mSelectedPlanet != null) {
            mLinePaint.setColor(mGalaxy.getCurrentPlayerData().colorText);
            // Always visible
            mLinePaint.setAlpha(128 + mTextAlpha / 2);
            sx = ij2xy(mSelectedPlanet.i);
            sy = ij2xy(mSelectedPlanet.j);

            if (mGesture == GESTURE_DRAG) {
                // Line from selected planet to touch point
                canvas.drawLine(sx, sy, mDragX, mDragY, mLinePaint);
            }
            else if (mTargetedPlanet != null) {
                // Line from selected planet to targeted planet
                tx = ij2xy(mTargetedPlanet.i);
                ty = ij2xy(mTargetedPlanet.j);
                canvas.drawLine(sx, sy, tx, ty, mLinePaint);
            }
        }
    }

    private void drawText(Canvas canvas, float textSize, Paint.Align align, int color, int alpha,
            String value, float x, float y, float dx, float dy) {
        // Draw text with shadow
        mTextPaint.setTextSize(textSize);
        mTextPaint.setTextAlign(align);
        mTextPaint.setColor(Color.BLACK);
        if (alpha < 255) mTextPaint.setAlpha(alpha);
        canvas.drawText(value, x + dx, y + dy, mTextPaint);
        mTextPaint.setColor(color);
        if (alpha < 255) mTextPaint.setAlpha(alpha);
        canvas.drawText(value, x, y, mTextPaint);
    }

    private void drawGrid(Canvas canvas) {
        int i,j;
        float strokeWidth = LINE_WIDTH;
        float startX, startY, stopX, stopY;

        mLinePaint.setColor(getColor(mContext, R.color.grid));
        mLinePaint.setAlpha(mGridAlpha);

        for (i = 0; i <= mGalaxy.getMaxDiameter(); i++) {
            startX = mTile * i;
            startY = mTile * 0 - strokeWidth/2;
            stopX = mTile * i;
            stopY = mTile * mGalaxy.getMaxDiameter() + strokeWidth/2;
            canvas.drawLine(startX, startY, stopX, stopY, mLinePaint);
        }
        for (j = 0; j <= mGalaxy.getMaxDiameter(); j++) {
            startX = mTile * 0 - strokeWidth/2;
            startY = mTile * j;
            stopX = mTile * mGalaxy.getMaxDiameter() + strokeWidth/2;
            stopY = mTile * j;
            canvas.drawLine(startX, startY, stopX, stopY, mLinePaint);
        }
    }

    private void drawSinglePlanet(Canvas canvas, PlanetData planet, PlayerData owner, float builtOffset, float radiusMult) {
        int i, j;
        int colorIn, colorOut;
        float x, y;
        // Padding of 1 pixel in all scales
        float padding = 1/mCanvasScale;
        float tile = mTile;
        // Reduce text size when larger than settings
        float textSize = scaledTextSize(tile/2);
        // Increase text padding when text size reduced due to settings
        float textPadding = (tile/2 - textSize) / 2;
        float radius = 2 * padding + tile * radiusMult;

        // Draw Planet as radial gradient
        i = planet.i;
        j = planet.j;
        x = tile/2 + tile*i;
        y = tile/2 + tile*j;
        colorIn = owner.colorText;
        colorOut = Color.TRANSPARENT;
        mRadialGradient = new RadialGradient(ij2xy(i), ij2xy(j), radius, colorIn, colorOut, Shader.TileMode.CLAMP);
        mGradientCirclePaint.setStyle(Style.FILL);
        mGradientCirclePaint.setShader(mRadialGradient);
        canvas.drawCircle(x, y, radius, mGradientCirclePaint);

        // Draw Planet as gradient drawable (without new allocations)
//        mRadialDrawable.setBounds((int)(x - radius), (int)(y - radius), (int)(x + radius), (int)(y + radius));
//        mRadialDrawable.setGradientRadius(radius);
//        mRadialDrawable.setGradientCenter(0.5f, 0.5f);
//        mRadialDrawable.setColorFilter(colorIn, PorterDuff.Mode.MULTIPLY);
//        mRadialDrawable.draw(canvas);

        // Draw planet Name
        x = tile*i + textPadding;
        y = tile*(j+1) - textPadding;
        drawText(canvas, textSize, Paint.Align.LEFT,
                Color.WHITE, 255, 
                planet.sName, x, y, +padding, -padding);

        // Draw planet Upkeep
        x = tile*i + tile/2;
        y = tile*j - builtOffset*tile/2;
        if (sBuilt != null) {
            drawText(canvas, textSize/2 + padding, Paint.Align.CENTER, 
                    owner.color, 255, 
                    sBuilt, x, y, +padding, +padding);
        }
        // Draw planet Ships
        x = tile*i + tile/2;
        y = tile*j + textSize - padding;
        if (mDrawingShips) {
            drawText(canvas, textSize + padding, Paint.Align.CENTER, 
                    owner.color, 255, 
                    sShips, x, y, +padding, +padding);
        }
        // Draw Distances to selected planet
        x = tile*(i+1) - textPadding;
        y = tile*(j+1) - textPadding;
        if (mDrawingDistances) {
            sDistance = String.valueOf(planet.distance);
            drawText(canvas, textSize*0.9f + padding, Paint.Align.RIGHT, 
                    getColor(mContext, R.color.distances), mTextAlpha, 
                    sDistance, x, y, -padding, -padding);
        }
        // Draw Defense (Ships arrived for neutral planets)
        else if (mDrawingDefenses) {
            if ( (mGalaxy.getRuleDefense() != SettingsActivity.DEFENSE_NONE) || (planet.player == 0) ){
                drawText(canvas, textSize/2 + padding, Paint.Align.RIGHT, 
                        getColor(mContext, R.color.infotext), 255, 
                        sDefense, x, y, -padding, -padding);
            }
        }
    }

    private void drawPlanets(Canvas canvas) {
        PlayerData owner;
        float builtOffset, radiusMult;
        float animationSplit = ANIMATION_DURATION/4;

        for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
            sBuilt = null;
            builtOffset = 0;
            // Static view of the galaxy this turn
            if (!mDrawingArrivals) {
                owner = mGalaxy.getPlayerData(planet.player);
                if (mGalaxy.getCurrentPlayer() == planet.player) {
                    sShips = String.valueOf(planet.shipsNow);
                } else {
                    sShips = String.valueOf(planet.shipsPublicNow);
                }
                sDefense = planet.sDefense;
            }
            // Static view of the galaxy in previous turn
            else if (mAnimationTime <= 2*animationSplit) {
                owner = mGalaxy.getPlayerData(planet.playerPrev);
                sShips = String.valueOf(planet.shipsPrev);
                sDefense = planet.sDefensePrev;
            }
            // Static view after the battles
            else if (mAnimationTime <= 3*animationSplit) {
                owner = mGalaxy.getPlayerData(planet.player);
                sShips = String.valueOf(planet.shipsPublicNow - (planet.production - planet.upkeep) );
                sDefense = planet.sDefense;
            }
            // Animated view of the ships built
            else  {
                owner = mGalaxy.getPlayerData(planet.player);
                sShips = String.valueOf(planet.shipsPublicNow);
                sDefense = planet.sDefense;
                if (planet.player != 0) {
                    sBuilt = "+" + String.valueOf(planet.production - planet.upkeep);
                    builtOffset = (float)(mAnimationTime - 3*animationSplit) / animationSplit;
                }
            }
            if (owner.index == 0) {
                // Neutral planets show always its production
                sShips = String.valueOf(planet.production);
            }

            // Planet Radius proportional to planet production
            radiusMult = RADIUS_MAX * (float)planet.production / PlanetData.MAX_PROD;
            if (mSelectedPlanet == planet) {
                // Highlight selected planet (no animation: too much new radial gradient)
                radiusMult += 1;
            }
            drawSinglePlanet(canvas, planet, owner, builtOffset, radiusMult);
        }
    }

    private void drawSingleFleet(Canvas canvas, FleetData fleet, double distanceOffset, boolean fow, boolean flight) {
        float x, y, fx, fy, tx, ty;
        float padding = 1/mCanvasScale;
        float tile = mTile;
        float textSize = scaledTextSize(tile/2) * 0.9f + padding;
        double distance, size;
        int colorFrom, colorTo;
        int ships;

        PlanetData planetFrom = mGalaxy.getPlanetData(fleet.from);
        PlanetData planetTo = mGalaxy.getPlanetData(fleet.to);
        PlayerData owner = mGalaxy.getPlayerData(fleet.player);
        if (fow) {
            // If Fog of War: do not draw source planet, use home planet instead
            planetFrom = mGalaxy.getPlanetData(fleet.player);
            ships = fleet.wave;
        } else {
            ships = fleet.ships;
        }
        fx = ij2xy(planetFrom.i);
        fy = ij2xy(planetFrom.j);
        tx = ij2xy(planetTo.i);
        ty = ij2xy(planetTo.j);

        // Distance to target at the end of this turn
        distance = tile * (2 * ( fleet.at - mGalaxy.getCurrentTurn() ) - distanceOffset);
        // Size of the ship gfx proportional to number of ships
        size = 2 * padding + tile/10 * Math.log10(1 + ships);
        setInflightVertices(fx, fy, tx, ty, distance, size, mVertices);
        setInflightTextPoint(fx, fy, tx, ty, distance, size, textSize, mPoint);
        // Coordinates at this distance from (fx, fy), while moving towards (tx, ty)
        x = mVertices[4];
        y = mVertices[5];

        // Line to target
        if (flight) {
            mLinePaint.setColor(getColor(mContext, R.color.fleets));
        } else {
            mLinePaint.setColor(owner.colorText);
        }
        // alpha setting hides the lines sooner than the labels
        mLinePaint.setAlpha(Math.max(0, 2 * mTextAlpha - 255));
        canvas.drawLine(x, y, tx, ty, mLinePaint);

        // Gradient line from origin
        colorFrom = Color.TRANSPARENT;
        colorTo = getColor(mContext, R.color.fleets);
        mLinearGradient = new LinearGradient(fx, fy, x, y, colorFrom, colorTo,  Shader.TileMode.CLAMP);
        mGradientLinePaint.setShader(mLinearGradient);
        canvas.drawLine(fx, fy, x, y, mGradientLinePaint);

        // Draw Ship arrow at flight position
        mPointerPaint.setColor(owner.color);
        setInflightPath(mVertices, mArrowPath);
        canvas.drawPath(mArrowPath, mPointerPaint);

        // Text with number of ships
        sShips = String.valueOf(ships);
        drawText(canvas, textSize, Paint.Align.CENTER, owner.color, mTextAlpha,
                sShips , mPoint[0], mPoint[1], padding, padding);
    }

    private void drawSingleThreat(Canvas canvas, PlanetData planetFrom, double distanceOffset, double alphaMult) {
        // Draw ships sent previous turn from this planet as a threat
        PlayerData playerOwner = mGalaxy.getPlayerData(planetFrom.playerPrev);
        int turns = 1;
        int ships = planetFrom.shipsPublicPrev - planetFrom.shipsPrev;
        drawSingleThreat(canvas, planetFrom, playerOwner, turns, ships, distanceOffset, alphaMult);
    }

    private void drawSingleThreat(Canvas canvas, FleetData fleet, double distanceOffset, double alphaMult) {
        // Draw this fleet as a threat
        PlanetData planetFrom = mGalaxy.getPlanetData(fleet.from);
        PlayerData playerOwner = mGalaxy.getPlayerData(fleet.player);
        int turns = mGalaxy.getCurrentTurn() - fleet.turn;
        int ships = fleet.threat;
        drawSingleThreat(canvas, planetFrom, playerOwner, turns, ships, distanceOffset, alphaMult);
    }

    private void drawSingleThreat(Canvas canvas, PlanetData planetFrom, PlayerData playerOwner, int turns, int ships, double distanceOffset, double alphaMult) {
        float padding = 1/mCanvasScale;
        float tile = mTile;
        float textSize = scaledTextSize(tile/2) + padding;
        float radius;
        float x, y;
        // alpha setting hides the labels at start of animation loop
        int alpha = mTextAlpha + (int)(alphaMult * (255 - mTextAlpha));
        int playerColor = playerOwner.color;

        // Circle around source planet
        // radius = range around source at the end of this turn
        radius = tile * (2 * turns + (float)distanceOffset);
        x = ij2xy(planetFrom.i);
        y = ij2xy(planetFrom.j);
        for (int n = 0; n < mColors.length; n++) {
            if (n%2 == 0) mColors[n] = Color.BLACK;
            else mColors[n] = playerColor;
        }
        mSweepGradient = new SweepGradient(x, y, mColors, null);
        mGradientCirclePaint.setStyle(Style.STROKE);
        mGradientCirclePaint.setShader(mSweepGradient);
        canvas.drawCircle (x, y, radius, mGradientCirclePaint);

        // Text with number of ships
        sShips = String.valueOf(ships);
        drawText(canvas, textSize, Paint.Align.RIGHT, playerColor, alpha,
                sShips, x + radius, y + textSize/2, padding, padding);
        drawText(canvas, textSize, Paint.Align.LEFT, playerColor, alpha,
                sShips, x - radius, y + textSize/2, padding, padding);
        drawText(canvas, textSize, Paint.Align.CENTER, playerColor, alpha,
                sShips, x, y + radius, padding, padding);
        drawText(canvas, textSize, Paint.Align.CENTER, playerColor, alpha,
                sShips, x, y + textSize - radius, padding, padding);
    }

    private void drawThreats(Canvas canvas) {
        double distanceOffset;
        double alphaMult;
        double threatAnimation = ANIMATION_DURATION/4;

        if (mAnimationTime <= threatAnimation) {
            distanceOffset = -1;
        } else if (mAnimationTime <= 2*threatAnimation) {
            // offset between -1 and 0 tiles  (animated between 1/4 and 2/4 of the time)
            distanceOffset = (mAnimationTime - threatAnimation) / threatAnimation - 1;
        } else if (mAnimationTime <= 3*threatAnimation) {
            distanceOffset = 0;
        } else {
            // offset between 0 and -1 tiles  (animated between 3/4 and 4/4 of the time)
            distanceOffset = - (mAnimationTime - 3*threatAnimation) / threatAnimation;
        }
        alphaMult = distanceOffset + 1;

        if (mGalaxy.getRuleFow() == SettingsActivity.FOW_UNKNOWN) {
            for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
                if ( (planet.playerPrev != mGalaxy.getCurrentPlayer()) && (planet.playerPrev != 0)
                        && (planet.shipsPrev < planet.shipsPublicPrev) ) {
                    // range of fleets sent the previous turn (Full Fog of War)
                    drawSingleThreat(canvas, planet, 2*distanceOffset + 2, alphaMult);
                }
            }
        } else {
            for (FleetData fleet : mGalaxy.getFleets()) {
                if (fleet.player != mGalaxy.getCurrentPlayer()) {
                    if (mGalaxy.getRuleFow() == SettingsActivity.FOW_THREATS) {
                        // range of each known threat at end of turn (Partial Fog of War)
                        if ( (fleet.turn < mGalaxy.getCurrentTurn()) && (fleet.threat > 0) ) {
                            drawSingleThreat(canvas, fleet, 2*distanceOffset + 2, alphaMult);
                        }
                    } else if (mGalaxy.getRuleFow() == SettingsActivity.FOW_KNOWN) {
                        // position of each known fleet at end of turn (No Fog of War)
                        if ( (fleet.turn < mGalaxy.getCurrentTurn()) ) {
                            drawSingleFleet(canvas, fleet, distanceOffset + 2, false, false);
                        }
                    }
                }
            }
        }
    }

    private void drawArrivals(Canvas canvas) {
        double distanceOffset;
        double arrivedAnimation = ANIMATION_DURATION/4;
        boolean fow;

        if (mAnimationTime <= arrivedAnimation) {
            distanceOffset = -1;
        } else if (mAnimationTime <= 2*arrivedAnimation) {
            // offset between -1 and 0 tiles (animated between 1/4 and 2/4 of the time)
            distanceOffset = (mAnimationTime - arrivedAnimation) / arrivedAnimation - 1;
        } else {
            distanceOffset = 0;
        }

        for (FleetData arrival : mGalaxy.getArrivals()) {
            // Enemy wave with Fog of War enabled
            fow = (mGalaxy.getRuleFow() == SettingsActivity.FOW_UNKNOWN) && (arrival.player != mGalaxy.getCurrentPlayer());

            // Draw all arrivals. Except if Fog of War, then draw one fleet per wave (ignore fleets with wave = 0)
            if ( (!fow) || (arrival.wave > 0) ) {
                if (mAnimationTime <= 2*arrivedAnimation) {
                    drawSingleFleet(canvas, arrival, distanceOffset, fow, false);
                }
            }
        }
    }

    private void drawFleets(Canvas canvas) {
        if (mDrawingFleets) {
            for (FleetData fleet : mGalaxy.getFleets()) {
                if (fleet.player == mGalaxy.getCurrentPlayer()) {
                    // offset 2 tiles away (position at end of turn)
                    drawSingleFleet(canvas, fleet, 2, false, true);
                }
            }
        }

        if (mDrawingThreats) {
            drawThreats(canvas);
        }

        if (mDrawingArrivals) {
            drawArrivals(canvas);
        }
    }

    private float scaledTextSize(float size) {
        float textSize;
        // Reduce text size when twice larger than settings
        if (size > 5f/4 * 2*mTextSize) {
            // 80% of size
            textSize = size * (4f/5);
        } else if (size > 2*mTextSize) {
             //99% to 80% of size
            textSize = 2*mTextSize;
        } else {
            //100% of size
            textSize = size;
        }
        return textSize;
    }

////// Conversion methods //////

    private float xViewToDraw(float coordinate) {
        return coordinate / mCanvasScale + mBoundsView.left;
    }
    private float yViewToDraw(float coordinate) {
        return coordinate / mCanvasScale + mBoundsView.top;
    }
    private float xDrawToView(float coordinate) {
        return (coordinate - mBoundsView.left) * mCanvasScale;
    }
    private float yDrawToView(float coordinate) {
        return (coordinate - mBoundsView.top) * mCanvasScale;
    }
    private float xToScroll(float coordinate) {
        return (coordinate - mBoundsDraw.centerX()) * mCanvasScale;
    }
    private float yToScroll(float coordinate) {
        return (coordinate - mBoundsDraw.centerY()) * mCanvasScale;
    }

    private int xy2ij(float xy) {
        if (mTile != 0) {
            return (int) (xy / mTile);
        } else {
            return 0;
        }
    }

    private float ij2xy(float ij) {
        float tile = mTile;
        return tile/2 + tile * ij;
    }

////// Gesture methods //////

    private void dragScroll(float touchX, float touchY) {
        float minLeft = mBoundsDraw.left + DRAG_MARGIN;
        float minTop = mBoundsDraw.top + DRAG_MARGIN;
        float maxRight = mBoundsDraw.right - DRAG_MARGIN;
        float maxBottom = mBoundsDraw.bottom - DRAG_MARGIN;
        float distanceX = 0;
        float distanceY = 0;

        // if touch point near the edge of the view (% of the size)
        if (touchX > maxRight) {
            // scroll faster the closer to the edge
            distanceX = touchX - maxRight;
        } else if (touchX < minLeft) {
            distanceX = touchX - minLeft;
        }
        if (touchY > maxBottom) {
            distanceY = touchY - maxBottom;
        } else if (touchY < minTop) {
            distanceY = touchY - minTop;
        }
        // scroll faster when zoomed
        distanceX *= mCanvasScale/SCALE_DIV;
        distanceY *= mCanvasScale/SCALE_DIV;
        increaseScroll(distanceX, distanceY);
    }

    private void centerScroll() {
        setScroll( (float)mSize / 2, (float)mSize / 2 );
    }

    private void increaseScroll(float distanceX, float distanceY) {
        float canvasScrollX = mCanvasScrollX + distanceX;
        float canvasScrollY = mCanvasScrollY + distanceY;
        
        setCanvasScroll(canvasScrollX ,  canvasScrollY);
    }

    private void setScroll(float coordX, float coordY) {
        float canvasScrollX = xToScroll(coordX);
        float canvasScrollY = yToScroll(coordY);
        
        setCanvasScroll(canvasScrollX ,  canvasScrollY);
    }

    private void setCanvasScroll(float scrollX, float scrollY) {
        float minRight = xToScroll(0);
        float minBottom = yToScroll(0);
        float maxLeft = xToScroll(mSize);
        float maxTop = yToScroll(mSize);

        // Don't let the center of the window get out of the borders of the board.
        mCanvasScrollX = Math.max(minRight, Math.min(scrollX, maxLeft));
        mCanvasScrollY = Math.max(minBottom, Math.min(scrollY, maxTop));
    }

    private void setScale(float scale) {
        // Don't let the board get too small or too large.
        mCanvasScale = Math.max(SCALE_MIN, Math.min(scale, 1 + mScaleMax));
    }

    private void updateZoom() {
        // Update scale of double tap zoom to fit to screen when preference is 0 (mZoomScale is 1)
        if (mZoomScale == 1 && mSize > 0) {
            mZoomScale = (float)Math.max(mBoundsDraw.width(), mBoundsDraw.height()) / mSize;
            mZoomScale = Math.max(SCALE_TAP, mZoomScale);
        }
    }

    private void updateDistances(float x, float y) {
        int i = xy2ij(x);
        int j = xy2ij(y);

        if (mGalaxy != null) {
            mDrawingDistances = mGalaxy.updateDistances(i, j);
        }
    }

    private void updateDistances(PlanetData planet) {
        if (mGalaxy != null) {
            mDrawingDistances = mGalaxy.updateDistances(planet.i, planet.j);
        }
    }

    private void focusPlanet(float x, float y) {
        int i = xy2ij(x);
        int j = xy2ij(y);

        if (mGalaxy != null) {
            PlanetData touchedPlanet = mGalaxy.findPlanetData(i, j);
            setFocusedPlanet(touchedPlanet);
        }
    }

    private void selectPlanet(float x, float y, boolean closest) {
        int i = xy2ij(x);
        int j = xy2ij(y);
        int currentPlayer;
        PlanetData touchedPlanet;

        if (mGalaxy != null) {
            currentPlayer = mGalaxy.getCurrentPlayer();
            if (closest) {
                // Automatically finds the closest planet to the pointer
                touchedPlanet = findClosestPlanetData(x, y, currentPlayer);
                setFocusedPlanet(touchedPlanet);
            } else {
                // If pointer over existing planet
                touchedPlanet = mGalaxy.findPlanetData(i, j);
            }
            if (touchedPlanet != null) {
                // If current player is the planet owner, select planet
                if (currentPlayer == touchedPlanet.player) {
                    setSelectedPlanet(touchedPlanet);
                }
            }
            // If pointer over void space, clean selection
             else {
                mDrawingDistances = false;
                clearSelectedPlanet();
            }
        }
    }

    private void targetPlanet(float x, float y, boolean closest) {
        int i = xy2ij(x);
        int j = xy2ij(y);
        PlanetData touchedPlanet;

        if (mGalaxy != null) {
            if (closest) {
                // Automatically finds the closest planet to the pointer
                touchedPlanet = findClosestPlanetData(x, y, -1);
                setFocusedPlanet(touchedPlanet);
            } else {
                // If pointer over existing planet
                touchedPlanet = mGalaxy.findPlanetData(i, j);
            }
            if (touchedPlanet != null) {
                updateDistances(touchedPlanet);
                // If pointed a planet other than current selected, set as target
                if (mSelectedPlanet != null && mSelectedPlanet != touchedPlanet) {
                    setTargetedPlanet(touchedPlanet);
                }
            }
            // If pointer over void space, clean selection
             else {
                mDrawingDistances = false;
                clearSelectedPlanet();
            }
        }
    }

////// Public methods: Get //////
    
     public PlanetData getFocusedPlanet() {
         return mFocusedPlanet;
     }
    
     public PlanetData getSelectedPlanet() {
         return mSelectedPlanet;
     }
     
     public PlanetData getTargetedPlanet() {
         return mTargetedPlanet;
     }

     public FleetData getPreviousFleet() {
         return mPreviousFleet;
     }

////// Public methods: Set //////

    public void clearSelectedPlanet() {
        setSelectedPlanet(null);
    }

    public void clearTargetedPlanet() {
        setTargetedPlanet(null);
    }

     public void setFocusedPlanet(PlanetData planet) {
        if (mFocusedPlanet != planet) {
            mFocusedPlanet = planet;
            if (mEventListener != null) mEventListener.onPlanetFocused();
        }
     }

     public void setSelectedPlanet(PlanetData planet) {
        mSelectedPlanet = planet;
        setTargetedPlanet(null);
        if (mEventListener != null) mEventListener.onPlanetSelected();
     }

     public void setTargetedPlanet(PlanetData planet) {
        mTargetedPlanet = planet;
        mPreviousFleet = findPreviousFleet(mSelectedPlanet, mTargetedPlanet);
        if (mEventListener != null) mEventListener.onPlanetTargeted();
     }

    public void setOptions(boolean scroll, int zoom, int gridAlpha, int textAlpha, int textSize) {
        mScrollBoard = scroll;
        // Zoom from 1 to (1 + mScaleMax)
        mZoomScale = 1 + (mScaleMax * zoom) / SettingsActivity.ZOOM_MAX;
        mGridAlpha = Math.max(0, Math.min(gridAlpha, 255));
        mTextAlpha = Math.max(0, Math.min(textAlpha, 255));
        mTextSize = textSize;
    }

    public void setDrawing(boolean defenses, boolean ships, boolean fleets, boolean arrivals, boolean threats) {
        if ( ((arrivals) && (!mDrawingArrivals)) || ((threats) && (!mDrawingThreats)) ) {
            // restart the animations:
            mAnimationStartTime = System.currentTimeMillis();
            postInvalidate();
        }
        mDrawingDefenses = defenses;
        mDrawingShips = ships;
        mDrawingFleets = fleets;
        mDrawingArrivals = arrivals;
        mDrawingThreats = threats;
        mAnimated = (mDrawingArrivals || mDrawingThreats);
    }

    public void setGalaxy(GalaxyData galaxyGame) {
        mGalaxy = galaxyGame;
        mFocusedPlanet = null;
        mSelectedPlanet = null;
        mTargetedPlanet = null;
        mPreviousFleet = null;
        // Set size of one board tile
        mTile = Math.min(mBoundsDraw.width(), mBoundsDraw.height()) / mGalaxy.getMaxDiameter();
        // Set size of the board
        mSize = mTile * mGalaxy.getMaxDiameter();
        // Set max scale (same tile size)
        mScaleMax = (float)mGalaxy.getMaxDiameter() / SCALE_DIV;
    }

////// Public methods: UI related Calculations //////

    public FleetData findPreviousFleet(PlanetData planetFrom, PlanetData planetTo) {
        int currentTurn = mGalaxy.getCurrentTurn();

        if (planetFrom != null && planetTo != null) {
            // Find previous fleet sent in same turn from same origin to same destination
            for (FleetData fleet : mGalaxy.getFleets()) {
                if ( (fleet.turn == currentTurn)
                        && (fleet.from == planetFrom.index)
                        && (fleet.to == planetTo.index) ) {
                    return fleet;
                }
            }
        }
        return null;
    }

    public PlanetData findClosestPlanetData(double x, double y, int player) {
        // negative player value in order to search for any owner
        double xp, yp;
        double distance;
        double minDistance = mSize;
        PlanetData closestPlanet = null;

        if (mGalaxy != null) {
            for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
                xp = ij2xy(planet.i);
                yp = ij2xy(planet.j);
                distance = getDistanceDouble(x, y, xp, yp);
                if (distance < minDistance) {
                    if ( (player < 0) || (player == planet.player) ) {
                        minDistance = distance;
                        closestPlanet = planet;
                    }
                }
            }
        }
        return closestPlanet;
    }

    public void setInflightPoint(double fx, double fy, double tx, double ty, double distance, float[] point) {
        double xProjection, yProjection, arcTan;

        arcTan = Math.atan2((tx - fx), (ty - fy));
        xProjection = distance * Math.sin(arcTan);
        yProjection = distance * Math.cos(arcTan);
            
        point[0] = (float)(tx - xProjection);
        point[1] = (float)(ty - yProjection);
    }

    public void setInflightVertices(double fx, double fy, double tx, double ty, double distance, double size, float[] vertex) {
        double xProjection, yProjection, arcTan;

        arcTan = Math.atan2((tx - fx), (ty - fy));
        xProjection = distance * Math.sin(arcTan);
        yProjection = distance * Math.cos(arcTan);
        vertex[0] = (float)(tx - xProjection);
        vertex[1] = (float)(ty - yProjection);

        // Vertex for the tail of the ship: same angle
        xProjection = (distance + size) * Math.sin(arcTan);
        yProjection = (distance + size) * Math.cos(arcTan);
        vertex[4] = (float)(tx - xProjection);
        vertex[5] = (float)(ty - yProjection);

        // Vertex for the wings of the ship: +22 degrees
        xProjection = (size * WING_LENGTH) * Math.sin(arcTan + WING_ARC);
        yProjection = (size * WING_LENGTH) * Math.cos(arcTan + WING_ARC);
        vertex[2] = (float)(vertex[0] - xProjection);
        vertex[3] = (float)(vertex[1] - yProjection);

        // Vertex for the wings of the ship: -22 degrees
        xProjection = (size * WING_LENGTH) * Math.sin(arcTan - WING_ARC);
        yProjection = (size * WING_LENGTH) * Math.cos(arcTan - WING_ARC);
        vertex[6] = (float)(vertex[0] - xProjection);
        vertex[7] = (float)(vertex[1] - yProjection);
    }

    public void setInflightTextPoint(double fx, double fy, double tx, double ty, double distance, double size, double textSize, float[] point) {
        double xProjection, yProjection, arcTan;

        arcTan = Math.atan2((tx - fx), (ty - fy));
        // Point for the text
        xProjection = (distance + size + textSize) * Math.sin(arcTan);
        yProjection = (distance + size + textSize) * Math.cos(arcTan);
        point[0] = (float)(tx - xProjection);
        point[1] = (float)(ty - yProjection + textSize/2);
    }

    public void setInflightPath(float[] vertex, Path arrowPath) {
        arrowPath.reset();
        arrowPath.moveTo(vertex[0], vertex[1]);
        arrowPath.lineTo(vertex[2], vertex[3]);
        arrowPath.lineTo(vertex[4], vertex[5]);
        arrowPath.lineTo(vertex[6], vertex[7]);
    }

    public double getDistanceDouble(double fx, double fy, double tx, double ty) {
        return Math.sqrt( (tx-fx) * (tx-fx) + (ty-fy) * (ty-fy) );
    }

//    @SuppressWarnings("deprecation")
    public static int getColor(Context context, int id){
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return context.getColor(id);
        } else {
            return context.getResources().getColor(id);
        }
    }

}

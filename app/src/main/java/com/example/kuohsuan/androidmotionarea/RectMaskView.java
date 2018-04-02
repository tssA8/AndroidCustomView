package com.example.kuohsuan.androidmotionarea;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;



public class RectMaskView extends View {
    private final static String TAG = RectMaskView.class.getSimpleName();

    public enum PaintStatus {
        SELECT,
        UNSELECT,
        TRIGGER,
        OTHER
    }

    private int width = 0;

    private int height = 0;

    private int MIN_SIZE;     //默认的宽高

    private float mRectStrokeWidth;  //線的厚度

    private Paint rectSelectPaint = new Paint(); //画笔

    private Paint rectUnSelectPaint = new Paint(); //画笔

    private Paint rectTriggerPaint = new Paint(); //画笔

    private int rectRowColumn = -1;

    private final int RECT_ROX_COLUMN_PRESET = 16;

    private int rectSelectColor = -1;

    private int rectUnSelectColor = -1;

    private int rectTriggertColor = -1;

    private int multipleForRowColumn = 1;


    private PaintStatus paintStatus = PaintStatus.OTHER;

    private final float RECT_STROKE_WIDTH = 1f; //dp

    private ArrayList<PaintStatus> paintLists = new ArrayList<PaintStatus>();

    public RectMaskView(Context aContext) {
        super(aContext);
        init(aContext, null);
    }

    public RectMaskView(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);
        init(aContext, attrs);
    }

    public RectMaskView(Context aContext, AttributeSet attrs, int defStyleAttr) {
        super(aContext, attrs, defStyleAttr);
        init(aContext, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RectMaskView(Context aContext, AttributeSet attrs, int defStyleAttr,
                        int defStyleRes) {
        super(aContext, attrs, defStyleAttr, defStyleRes);
        init(aContext, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        //get xml values
        if(attrs != null) {
            TypedArray attrValues = context.obtainStyledAttributes(attrs, R.styleable.RectMaskViewViewAttr);
            rectRowColumn = attrValues.getInteger(R.styleable.RectMaskViewViewAttr_row_column, RECT_ROX_COLUMN_PRESET);
            rectSelectColor = attrValues.getColor(R.styleable.RectMaskViewViewAttr_select_color, getContext().getResources().getColor(R.color.motion_aera_select_color));
            rectUnSelectColor = attrValues.getColor(R.styleable.RectMaskViewViewAttr_unselect_color, getContext().getResources().getColor(R.color.rect_bg));
            rectTriggertColor = attrValues.getColor(R.styleable.RectMaskViewViewAttr_trigger_color, getContext().getResources().getColor(R.color.motion_aera_select_color));
        }

        mRectStrokeWidth = RECT_STROKE_WIDTH; //線的厚度


        MIN_SIZE = DensityUtil
                .dip2px(getContext(), 72); //这里的dip2px方法就是简单的将72dp转换为本机对应的px,可以去网上随便搜一个

//        multipleForRowColumn = conversionsMultipleForRowColumn();

        // init paint

        if(rectSelectColor != -1) {
            rectSelectPaint.setColor(rectSelectColor);
        }else {
            rectSelectPaint.setColor(getContext().getResources().getColor(R.color.motion_aera_select_color));
        }
        rectSelectPaint.setAntiAlias(true);
        rectSelectPaint.setStrokeWidth(mRectStrokeWidth);
        rectSelectPaint.setStyle(Paint.Style.FILL);

        if(rectUnSelectColor != -1) {
            rectUnSelectPaint.setColor(rectUnSelectColor);
        }else {
            rectUnSelectPaint.setColor(getContext().getResources().getColor(R.color.unselect_color));
        }
        rectUnSelectPaint.setAntiAlias(true);
        rectUnSelectPaint.setStrokeWidth(mRectStrokeWidth);
        rectUnSelectPaint.setStyle(Paint.Style.STROKE);

        if(rectTriggertColor != -1) {
            rectTriggerPaint.setColor(rectTriggertColor);
        }else {
            rectTriggerPaint.setColor(getContext().getResources().getColor(R.color.motion_aera_select_color));
        }
        rectTriggerPaint.setAntiAlias(true);
        rectTriggerPaint.setStrokeWidth(mRectStrokeWidth);//這邊故意條粗
        rectTriggerPaint.setStyle(Paint.Style.STROKE);

        if(rectRowColumn == -1) {
            rectRowColumn = RECT_ROX_COLUMN_PRESET;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(MIN_SIZE, MIN_SIZE);
        } else if (widthMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(MIN_SIZE, heightSize);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSize, MIN_SIZE);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        /*注意,绘制的坐标是以当前View的左上角为圆点的,而不是当前View的坐标*/
        //圆心坐标计算
        width = getWidth();
        height = getHeight();

        Log.d(TAG, "width: " + width + " height: " + height);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw" + " width: " + width + " height: " + height + " paintLists size:" + paintLists.size());

//        canvas.drawRect(0,0,width,height, rectSelectPaint);

        int indexX = 0;
        int indexY = 0;
        int index = 0;
        if(paintLists.size()>0){
            for(int Y = 0; Y < rectRowColumn; Y++) {
                indexY = Y*(height/rectRowColumn);
                for(int X = 0; X < rectRowColumn; X++) {
                    indexX = X*(width/rectRowColumn);
//                Log.d(TAG,"indexX: " + indexX + " indexY: " + indexY);

                    if(paintLists.get(index).equals(PaintStatus.SELECT)) {
                        paintRect(canvas, indexX, indexY, rectSelectPaint);
                    } else if(paintLists.get(index).equals(PaintStatus.UNSELECT)) {
                        paintRect(canvas, indexX, indexY, rectUnSelectPaint);
                    } else if(paintLists.get(index).equals(PaintStatus.TRIGGER)) {
                        paintRect(canvas, indexX, indexY, rectTriggerPaint);
                    } else if(paintLists.get(index).equals(PaintStatus.OTHER)) {
                        paintRect(canvas, indexX, indexY, rectUnSelectPaint);
                    }
                    index++;
                }
            }
        }
    }

    private void paintRect(Canvas canvas, int indexX, int indexY, Paint paint) {
        if(indexX != width) {
//            Log.d(TAG,"leftPaddingDraw: " +indexX+" + "+ leftPaddingDraw + " topPaddingDraw: " + topPaddingDraw);
            canvas.drawRect(
                    indexX,
                    indexY,
                    indexX + (width/rectRowColumn),
                    indexY + (height/rectRowColumn),
                    paint);

        } else if(indexX == width && indexY != height){
            canvas.drawRect(indexX, indexY, indexX, indexY + (height/rectRowColumn), paint);
        }
    }


    public boolean onTouchEvent(MotionEvent event) {

//        Log.d(TAG,"onTouchEvent PointerId: " + event.getPointerId(event.getActionIndex()));

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG,"ACTION_DOWN x: " + event.getX() + " y: " + event.getY());

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d(TAG,"ACTION_POINTER_DOWN x: " + event.getX() + " y: " + event.getY());

                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d(TAG,"ACTION_POINTER_UP x: " + event.getX() + " y: " + event.getY());

                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG,"ACTION_MOVE x: " + event.getX() + " y: " + event.getY());

                invalidate();//更新
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG,"ACTION_UP x: " + event.getX() + " y: " + event.getY());

                invalidate();//更新
                break;
        }
        return true;
    }

    private int conversionsMultipleForRowColumn() {
        int multiple = 0;
        if (getResources().getDisplayMetrics().density == 1) {
            //mdpi
            multiple = 1;
        } else if (getResources().getDisplayMetrics().density == 1.5) {
            //hdpi
            multiple = 1;
        } else if (getResources().getDisplayMetrics().density == 2) {
            //xhdpi
            multiple = 2;
        } else if (getResources().getDisplayMetrics().density == 3) {
            //xxhdpi
            multiple = 3;
        } else if (getResources().getDisplayMetrics().density == 4) {
            //xxxhdpi
            multiple = 4;
        }
        return multiple;
    }

    /** Rect
     * set function
     */

    public void onDrawRect(ArrayList<PaintStatus> aPaintLists) {
        paintLists.clear();

        //width = aWidth;
        //height = aHeight;

        paintLists = (ArrayList<PaintStatus>) aPaintLists.clone();
        invalidate();//更新
    }

    public void setPaintColor(int selectColor , int unSelectColor , int triggertColor ){
        rectSelectColor = selectColor;
        rectUnSelectColor = unSelectColor;
        rectTriggertColor = triggertColor;
    }

}

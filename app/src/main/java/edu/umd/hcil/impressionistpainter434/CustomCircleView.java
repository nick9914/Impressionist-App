package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Nicolas on 4/8/2016.
 */
public class CustomCircleView extends View {
    private int mBrushSize;
    private Paint mPaint;
    private BrushType mBrushType;

    public CustomCircleView(Context context) {
        super(context);
        init(null,0);
    }

    public CustomCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs,0);
    }

    public CustomCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, 0);
    }

    private void init(AttributeSet attrs, int defStyle) {
        mBrushSize = 5;
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(7);
        mBrushType = BrushType.Circle;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if(enabled) {
            mPaint.setColor(Color.WHITE);
            invalidate();
        } else {
            mPaint.setColor(Color.LTGRAY);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = getWidth();
        int y = getHeight();
        switch (mBrushType) {
            case Circle:
                canvas.drawCircle(x /2, y / 2, mBrushSize, mPaint);
                break;
            case Square:
                canvas.drawRect(x /2 - mBrushSize, y/2 - mBrushSize, x/2 + mBrushSize, y/2 + mBrushSize, mPaint);
                break;
            case Random:
                canvas.drawCircle(x /2, y / 2, mBrushSize, mPaint);
                canvas.drawRect(x /2 - mBrushSize, y/2 - mBrushSize, x/2 + mBrushSize, y/2 + mBrushSize, mPaint);
                break;
        }
    }

    public void setmBrushSize(int brushSize) {
        mBrushSize = brushSize;
        invalidate();
    }

    public void setmBrushType(BrushType brushType) {
        mBrushType = brushType;
        invalidate();
    }
}

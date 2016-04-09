package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.Random;
import java.util.UUID;

public class ImpressionistView extends View {
    private Rect _imageViewRect;

    private Bitmap _imageViewBitmap;

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = false;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private VelocityTracker mVelocityTracker  = null;
    private float currBrushSize = _minBrushRadius;
    private Random r;

    private static final double MAX_SPEED = 3000.0;
    private static final float MAX_BRUSH_SIZE = 50;
    private static final float MIN_BRUSH_SIZE = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        r = new Random();

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
            clearPainting();
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }


    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        int index = motionEvent.getActionIndex();
        int pointerId = motionEvent.getPointerId(index);

        //TODO
        //Basically, the way this works is to listen for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();

        if(!validCoordinates(Math.round(curTouchX), Math.round(curTouchY))) {
            Log.d("ImpressionitView", "Invalid Coordinates 1");
            return true;
        }
        int touchedRGB = getPixelColor(Math.round(curTouchX), Math.round(curTouchY));
        _paint.setColor(touchedRGB);

        switch(motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(mVelocityTracker == null) {
                    // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                    mVelocityTracker = VelocityTracker.obtain();
                }
                else {
                    // Reset the velocity tracker back to its initial state.
                    mVelocityTracker.clear();
                }
                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(motionEvent);
                // When you want to determine the velocity, call
                // computeCurrentVelocity(). Then call getXVelocity()
                // and getYVelocity() to retrieve the velocity for each pointer ID.
                mVelocityTracker.computeCurrentVelocity(1000);
                double velocity = calculateVelocity(pointerId);
                float velocityBrushSize = calculateBrushSize(velocity);
                int historySize = motionEvent.getHistorySize();
                for (int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if(!validCoordinates(Math.round(touchX), Math.round(touchY))) {
                        Log.d("ImpressionitView", "Invalid Coordinates 2");
                        return true;
                    }

                    // TODO: draw to the offscreen bitmap for historical x,y points
                    _paint.setColor(getPixelColor(Math.round(touchX), Math.round(touchY)));
                    drawShape(touchX, touchY, velocityBrushSize);
                }
                // TODO: draw to the offscreen bitmap for current x,y point.
                // Insert one line of code here
                if(!validCoordinates(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()))){
                    Log.d("ImpressionitView", "Invalid Coordinates 3");
                    return true;
                }
                _paint.setColor(getPixelColor(Math.round(motionEvent.getX()), Math.round(motionEvent.getY())));
                drawShape(motionEvent.getX(), motionEvent.getY(), velocityBrushSize);
                invalidate();
                break;
        }
        return true;
    }

    public double calculateVelocity (int pointerId) {
        float xVelocity = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId);
        float yVelocity = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId);
        double zVelocity = Math.sqrt(Math.pow(xVelocity, 2) + Math.pow(yVelocity, 2));
        Log.d("VELOCITY", "Velocity = " + zVelocity);
        if(zVelocity > MAX_SPEED) {
            return MAX_SPEED;
        } else {
            return zVelocity;
        }

    }



    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    private boolean validCoordinates(int x, int y) {
        if(_imageViewRect == null) {
            return false;
        }
        return x > _imageViewRect.left && x < _imageViewRect.right && y > _imageViewRect.top && y < _imageViewRect.bottom;
    }

    private int getPixelColor(int x, int y) {
        if(validCoordinates(x, y)) {
            return _imageViewBitmap.getPixel(x, y);
        } else {
            return Color.WHITE;
        }
    }

    private void drawShape(float x, float y, float speedBrushSize) {
        switch (_brushType) {
            case Circle:
                if(_useMotionSpeedForBrushStrokeSize) {
                    _offScreenCanvas.drawCircle(x, y, speedBrushSize, _paint);
                } else {
                    _offScreenCanvas.drawCircle(x, y, currBrushSize, _paint);
                }
                break;
            case Square:
                if(_useMotionSpeedForBrushStrokeSize) {
                    _offScreenCanvas.drawRect(x - speedBrushSize, y - speedBrushSize, x + speedBrushSize, y + speedBrushSize, _paint);
                } else {
                    _offScreenCanvas.drawRect(x - currBrushSize, y - currBrushSize, x + currBrushSize, y + currBrushSize, _paint);
                }
                break;
            case Random:
                if(r.nextBoolean()){
                    _offScreenCanvas.drawCircle(x, y, currBrushSize, _paint);
                } else {
                    _offScreenCanvas.drawRect(x - currBrushSize, y - currBrushSize, x + currBrushSize, y + currBrushSize, _paint);
                }
                break;
        }
    }

    public float calculateBrushSize(double speed) {
        float brushSize = Math.round((MAX_BRUSH_SIZE * speed) / MAX_SPEED);
        if(brushSize < MIN_BRUSH_SIZE) {
            return MIN_BRUSH_SIZE;
        } else {
            return brushSize;
        }
    }

    public void updateImageViewInfo() {
        _imageViewRect = getBitmapPositionInsideImageView(_imageView);
        Drawable imgDrawable = _imageView.getDrawable();
        if(imgDrawable != null) {
            _imageViewBitmap = ((BitmapDrawable)imgDrawable).getBitmap();
        }
    }

    public void saveImagePainting(Context context) {

        String fName = UUID.randomUUID().toString() + ".png";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fName);
        try {
            boolean compressSucceeded = _offScreenBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
            FileUtils.addImageToGallery(file.getAbsolutePath(), context);
            Toast.makeText(context, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void speedOfMotionCheckBox(Boolean checked) {
        if(checked) {
            _useMotionSpeedForBrushStrokeSize = true;

        } else {
            _useMotionSpeedForBrushStrokeSize = false;
        }
    }

    public void changeBrushSize(float size) {
        currBrushSize = size;
    }
}


package ca.sapphire.gettemp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by apreston on 7/13/2015.
 */
public class WaveView extends View {

    public String TAG = "WaveView";

//    private ShapeDrawable mDrawable;
    Paint paint = new Paint();

    short[] wave;
    int start, size, length;

    float startX, startY, endX, endY;
    int scroll = 0;
    int zoom = 0;
    int pan = 0;

    int gain = 20;
    int ofs = 0;
    int autoRange = 0;
    boolean hasBeenAutoranged = false;

    public WaveView(Context context, short[] wave, int start, int size) {
        super(context);
        this.wave = wave;
        this.start = start;
        this.size = size;
        this.length = wave.length;

        this.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    startX = event.getRawX();
                    startY = event.getRawY();
                }

                if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    endX = event.getRawX();
                    endY = event.getRawY();

                    pan = -( (int)(endX-startX) ) / 100;
                    zoom = +( (int) (endY-startY) ) / 100;

//                    if( scroll == 0 && zoom == 0 ) {
//                        if( startX < 100 )
//                            pan = -1;
//                        if( startX > 100 )
//                            pan = 1;
//                    }

                    v.invalidate();
                    // Do what you want
                    return true;
                }
                return true;
            }
            // Implementation;
        });

//        addListenerTouch(this);

    }

//    public void addListenerTouch( View v ) {
//        this.setOnTouchListener() {
//            @Override
//                    public void onTouch( View v, MotionEvent me) {
//
//            }
//
//        };
//    }

    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if( !hasBeenAutoranged ) {
            autoRange(height);
            thumbnail(300, 100);
        }

        if( pan != 0 ) {
            ofs += pan * width/2;
            if( ofs < 0 ) ofs = 0;
            if( ofs > (length-width) ) ofs = length-width;
        }

        if( zoom != 0 ) {
            gain += zoom;
            if( gain < 1 ) gain = 1;
            if( gain > 200 ) gain = 200;
        }

        pan = zoom = 0;

        paint.setColor(Color.RED);
        drawThumbnail( canvas, 100, 100 );

        paint.setColor(Color.BLUE);
        drawRegion(canvas, 8820, width, gain, 1, 0, height / 2);
//        drawRegion(canvas, ofs, width, gain, 1, 0, height / 2);
  }

    public void autoRange( int height ) {
        short minX = 0, maxX = 0;
        short element;

        for (int i = 0; i < wave.length; i++) {
            element = wave[i];
            maxX = element > maxX ? element : maxX;
            minX = element < minX ? element : minX;
        }

        autoRange = maxX > (-minX) ? maxX : -minX;

        gain = autoRange > (height/2) ? autoRange / (height/2) : 1;

        hasBeenAutoranged = true;

        Log.i( TAG, "Autorage: " + gain );
    }

    /**
     *
     * @param width
     * @param height
     */

    byte[] thumbnail;

    public void thumbnail( int width, int height ) {
        int skip = wave.length/width;
        int element = 0;
        int divider = 65536 / height;
        thumbnail = new byte[width];

        for (int i = 0; i < width; i++) {
            thumbnail[i] = (byte) (wave[element] / divider);
            element += skip;
        }
    }

    public void drawThumbnail( Canvas canvas, int x, int y ) {
        for (int i = 1; i < thumbnail.length; i++) {
            canvas.drawLine( x+i-1, y+thumbnail[i-1], x+i, y+thumbnail[i], paint );
        }
    }


    public void drawRegion( Canvas canvas, int startIndex, int length, int gain, int zoom, int xOffset, int yOffset ) {
        int x, newX;
        int y, newY;

        x = xOffset;
        y = wave[startIndex]/gain + yOffset;

        for (int i = startIndex+1; i < startIndex+length ; i+=zoom ) {
            newX = x+1;
            newY = (wave[i]/gain) + yOffset;
            canvas.drawLine( x, y, newX, newY, paint );
            x = newX;
            y = newY;
        }
    }
}

package ca.sapphire.gettemp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;
import android.util.TypedValue;
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
    boolean isMono;
    int[] markers;

    float startX, startY, endX, endY;
    int scroll = 0;
    int zoom = 0;
    int pan = 0;

    int gain = 20;
    int ofs = 0;
    int autoRange = 0;
    boolean hasBeenAutoranged = false;

    final static int MONO_TRACK = 2;
    final static int LEFT_TRACK = 0;
    final static int RIGHT_TRACK = 1;

    final static boolean MONO_MODE = true;
    final static boolean STEREO_MODE = false;

    public WaveView(Context context, short[] wave, int start, int size, boolean isMono, int[] markers ) {
        super(context);
        this.wave = wave;
        this.start = start;
        this.size = size;
        this.isMono = isMono;
        this.length = wave.length;
        this.markers = markers;

        paint.setTextSize( paint.getTextSize() * 2.0f );

        this.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = event.getRawX();
                    startY = event.getRawY();
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    endX = event.getRawX();
                    endY = event.getRawY();

                    pan = -((int) (endX - startX)) / 100;
                    zoom = +((int) (endY - startY)) / 100;

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

        paint.setStyle( Paint.Style.STROKE );
        paint.setColor(Color.RED);
        drawThumbnail(canvas, 100, 100);

        paint.setColor(Color.DKGRAY);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(Integer.toString(ofs), 10, height - 50, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(Integer.toString(ofs + width), width - 10, height - 50, paint);

        if( isMono ) {
            paint.setColor(Color.BLUE);
            drawRegion(canvas, MONO_TRACK, ofs, width, gain, 1, 0, height / 2);
        }
        else {
            paint.setColor(Color.BLUE);
            drawRegion(canvas, LEFT_TRACK, ofs, width, gain, 1, 0, height / 2);

            paint.setColor(Color.GREEN);
            drawRegion(canvas, RIGHT_TRACK, ofs, width, gain, 1, 0, height / 2);
        }
  }

    public void autoRange(int height) {
        short minX = 0, maxX = 0;

        for (short element : wave) {
            maxX = element > maxX ? element : maxX;
            minX = element < minX ? element : minX;
        }

        autoRange = maxX > (-minX) ? maxX : -minX;

        gain = autoRange > (height / 2) ? autoRange / (height / 2) : 1;

        hasBeenAutoranged = true;

        Log.i(TAG, "Autorange: " + gain);
    }

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
            canvas.drawLine( x+i-1, y-thumbnail[i-1], x+i, y-thumbnail[i], paint );
        }
    }


    public void drawRegion( Canvas canvas, int track, int startIndex, int length, int gain, int zoom, int xOffset, int yOffset ) {
        int x, newX;
        int y, newY;

        x = xOffset;
        y = (-wave[startIndex]/gain) + yOffset;

        if( track == MONO_TRACK ) {
            for (int i = startIndex + 1; i < startIndex + length; i += zoom) {
                newX = x + 1;
                newY = (-wave[i] / gain) + yOffset;
                canvas.drawLine(x, y, newX, newY, paint);
                x = newX;
                y = newY;
            }

            if( markers != null ) {
                for (int i = 0; i < markers.length; i++) {
                    // check if marker is visible
                    if( markers[i] >= startIndex && markers[i] <= startIndex+length ) {
                        int markX = markers[i]-startIndex+xOffset;
                        int markY = ( -wave[markers[i]] / gain ) + yOffset;
                        canvas.drawRect( markX-10, markY-10, markX+10, markY+10, paint);
                        canvas.drawText( Integer.toString(i), markX+15, markY, paint );
                    }
                }
            }
        }
        else {
            x += track;
            for (int i = startIndex + 2 + track; i < (startIndex+length)*2; i += zoom+zoom) {
                newX = x + 1;
                newY = (-wave[i] / gain) + yOffset;
                canvas.drawLine(x, y, newX, newY, paint);
                x = newX;
                y = newY;
            }
        }
    }

}

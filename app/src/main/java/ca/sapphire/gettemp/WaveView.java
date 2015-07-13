package ca.sapphire.gettemp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.view.View;

/**
 * Created by apreston on 7/13/2015.
 */
public class WaveView extends View {
    private ShapeDrawable mDrawable;
    Paint paint = new Paint();

    short[] wave;
    int start, size;

    public WaveView(Context context, short[] wave, int start, int size) {
        super(context);
        this.wave = wave;
        this.start = start;
        this.size = size;
    }

    protected void onDraw(Canvas canvas) {
        paint.setColor(Color.RED);

        canvas.drawLine(0, 0, 100, 100, paint);
        canvas.drawLine(0, 100, 100, 0, paint);


        paint.setColor(Color.BLUE);

        int y, newY;

        y = (wave[start]/20) + 100;
        for (int x = 1; x < size; x++) {
            newY = (wave[start+x]/20)+ 100;
            canvas.drawLine( x-1, y, x, newY, paint );
            y = newY;
        }
        y = (wave[start+400]/20) + 200;
        for (int x = 0; x < size; x++) {
            newY = (wave[start+400+x]/20)+ 200;
            canvas.drawLine( x-1, y, x, newY, paint );
            y = newY;
        }
        y = (wave[start+800]/20) + 300;
        for (int x = 0; x < size; x++) {
            newY = (wave[start+800+x]/20)+ 300;
            canvas.drawLine( x-1, y, x, newY, paint );
            y = newY;
        }
        y = (wave[start+1200]/20) + 400;
        for (int x = 0; x < size; x++) {
            newY = (wave[start+1200+x]/20)+ 400;
            canvas.drawLine( x-1, y, x, newY, paint );
            y = newY;
        }
        y = (wave[start+1600]/20) + 500;
        for (int x = 0; x < size; x++) {
            newY = (wave[start+1600+x]/20)+ 500;
            canvas.drawLine( x-1, y, x, newY, paint );
            y = newY;
        }
        y = (wave[start+2000]/20) + 600;
        for (int x = 0; x < size; x++) {
            newY = (wave[start+x+2000]/20)+ 600;
            canvas.drawLine( x-1, y, x, newY, paint );
            y = newY;
        }
        y = (wave[start+2400]/20) + 700;
        for (int x = 0; x < size; x++) {
            newY = (wave[start+2400+x]/20)+ 700;
            canvas.drawLine( x-1, y, x, newY, paint );
            y = newY;
        }
    }
}

package ca.sapphire.gettemp;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static ca.sapphire.gettemp.SignalProcess.bubbleSort;
import static ca.sapphire.gettemp.SignalProcess.rms;
import static ca.sapphire.gettemp.SignalProcess.zeroCross;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GetTemp";

    Handler measureHandler = new Handler();

    private AudioRecord mRecorder = null;

    Button measureButton = null;

    // 32768 samples at 44100 is 743 mS
    // it takes about 100mS for the tone to start
    // tone is 300mS long
    // therefore consider the data to be at 200mS into the sample for 100mS

    int status;
    private int bufSize = 32768;

    final int sampleRate = 44100;
    final int frequency = 900;
    final int wavelength = 49;
    final int period100ms = 4410;

    final double seriesResistorValue = 3300.0;

    short[] tone;
    private short[] buffer = new short[bufSize];
    int[] markers = new int[10];

    boolean loopedToneIsRunning = false;
    boolean measuring = false;

    TextView valueText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        valueText = (TextView) findViewById(R.id.ValueText);
        measureButton = (Button) findViewById(R.id.MeasureButton );
        measureButton.setText("Measure");

        tone = MakeTone.makeSplitTone(frequency, sampleRate, 1.0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        stopMeasuring();
        super.onPause();
    }

    // called both when the Measure/Stop button is pressed and when Activity is paused
    public void stopMeasuring() {
        measuring = false;

        measureHandler.removeCallbacks(measureRunnable);
        loopedToneIsRunning = false;

        if( mRecorder != null ) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    // invoked when Measure/Stop button is clicked
    public void measure(View v) {
        if( !measuring ) {
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);

            measuring = true;
            measureButton.setText("Stop");

            valueText.setText( "" );

            if( !loopedToneIsRunning )
                new PlayLoopedSound().execute();

            mRecorder.startRecording();
            measureHandler.postDelayed(measureRunnable, 500);
        }
        else {
            measureButton.setText("Measure");
            stopMeasuring();
        }
    }

    Runnable measureRunnable = new Runnable() {
        @Override
        public void run() {
            status = mRecorder.read(buffer, 0, bufSize);
            Log.i(TAG, "\nRecord read status = " + status);
            splitRangeCalculate();
            mRecorder.startRecording();
            measureHandler.postDelayed( measureRunnable, 500 );
        }
    };

    public void calibrate(View v) {
        Intent intent = new Intent(this, WaveViewActivity.class);
        intent.putExtra("wave", tone);
        intent.putExtra("mode", WaveView.STEREO_MODE);
        intent.putExtra("markers", (String) null);
        startActivity(intent);
    }

    /**
     * Calculates the current value
     */
    public void splitRangeCalculate() {
        // start looking for a zero crossing at 10 periods into the waveform, about 11ms @ 900hz

        int[] values = new int[8];
        double value;
        int scan = zeroCross( buffer, wavelength*10, 75, 6 );

        markers[0] = period100ms;
        markers[1] = scan;

        Log.i(TAG, "\nZero:" + scan);


        for (int j = 0; j < 8; j++) {
            int min = Short.MAX_VALUE;
            int max = Short.MIN_VALUE;

            // scan first 1kHz period for peaks.
            for (int i = scan + j*wavelength; i < scan + (j+1)*wavelength; i++) {
                min = Math.min(min, buffer[i]);
                max = Math.max(max, buffer[i]);
            }
            values[j] = max-min;
        }

        bubbleSort( values );

        value = ((double)values[5]+(double)values[6]) / ((double)values[1]+(double)values[2]) ;

        valueText.append("\nVal: " + String.format( "%.2f :  %.2f  :  %.1f'C", value, value*seriesResistorValue, Thermistor.temperature(value*seriesResistorValue) ));
        Log.i(TAG, "\nValue:" + value);
    }

    public void rmsCalculate() {
        double value;
        double[] rmsA = new double[16];
        double[] rmsB = new double[16];

        // start looking for a zero crossing at 100mS into the waveform.
        // search 3 wavelengths to look for a good crossing
        int scan = zeroCross( buffer, period100ms, wavelength*3, 6 );

        for (int i = 0; i < 16; i++) {
            rmsA[i] = rms( buffer, scan + wavelength*(i*2), wavelength );
            rmsB[i] = rms( buffer, scan + wavelength*(i*2+1), wavelength);
            Log.i(TAG, "A:B  " + rmsA[i] + " : " + rmsB[i] );
        }

        bubbleSort( rmsA );
        bubbleSort( rmsB );

        double rA = 0, rB = 0;

        for (int i = 4; i < 12; i++) {
            rA += rmsA[i];
            rB += rmsB[i];
        }

        rA /= 8;
        rB /= 8;

        value = (float) (rB/rA);
        if( value > 1 )
            value = 1/value;

        Log.i(TAG, "\nRMS A8:" + rA);
        Log.i(TAG, "\nRMS B8:" + rB);
        Log.i(TAG, "\nValue:" + value);

        valueText.append("  RMS: " + value);
    }

    public void wave(View v) {
        Intent intent = new Intent(this, WaveViewActivity.class);
        intent.putExtra("wave", buffer);
        intent.putExtra("mode", WaveView.MONO_MODE);
        intent.putExtra("markers", markers);
        startActivity(intent);
    }

    private class PlayLoopedSound extends AsyncTask<Void, Void, Void> {

        AudioTrack track;

        @Override
        protected void onPreExecute() {
            track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    tone.length, AudioTrack.MODE_STREAM);
            track.write(tone, 0, tone.length);
            track.play();
            loopedToneIsRunning = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while( measuring )
                track.write(tone, 0, tone.length);
            return null;
        }

        @Override
        protected void onPostExecute(Void result ) {
            track.pause();
            track.flush();
            track.release();
            loopedToneIsRunning = false;
        }
    }
}

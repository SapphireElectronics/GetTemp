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
import static java.lang.System.arraycopy;

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

    short[] tone;
    private short[] buffer = new short[bufSize];

    boolean loopedToneIsRunning = false;
    boolean measuring = false;

    TextView valueText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        valueText = (TextView) findViewById(R.id.ValueText);
        measureButton = (Button) findViewById(R.id.MeasureButton );
//        makeTone( 1000, 44100, 1.0 );
        makeSplitTone(900, 44100, 1.0);
//        Log.i(TAG, "\nMinBufferSize = " + AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT));
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

    public void measure(View v) {
        if( !measuring ) {
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);

            measuring = true;
            measureButton.setText("Stop");

            valueText.setText( "" );

            if( !loopedToneIsRunning )
                new PlayLoopedSound().execute();

            mRecorder.startRecording();
            measureHandler.postDelayed(measureRunnable, 500);
        }
        else {
            measuring = false;
            measureButton.setText("Measure");

            measureHandler.removeCallbacks(measureRunnable);
            loopedToneIsRunning = false;

            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    Runnable measureRunnable = new Runnable() {
        @Override
        public void run() {
            status = mRecorder.read(buffer, 0, bufSize);
            Log.i(TAG, "\nRecord read status = " + status);
            splitCalculate();
            rmsCalculate();
            mRecorder.startRecording();
            measureHandler.postDelayed( measureRunnable, 500 );
        }
    };

    public void calibrate(View v) {
        calibrate();
    }

    /**
     * Calculates the current value
     */

    int reference = 0;
    float value = 0;

        /**
     * Calculates the current value using split tone.
     * Evaluates relative amplitudes of signals separately from LH and RH tones
     * TODO: put in a marker as LH and RH get lost on a mono input.
     */
    public void splitCalculate() {
    // start looking for a zero crossing at 200mS into the waveform.
        int scan = zeroCross( buffer, 8820, 75 );

        Log.i(TAG, "\nZero:" + scan);

        int minA = Short.MAX_VALUE;
        int maxA = Short.MIN_VALUE;
        int minB = Short.MAX_VALUE;
        int maxB = Short.MIN_VALUE;

//todo: Replace magic numbers
        // scan first 1kHz period for peaks.
        for (int i = scan; i < scan + 49; i++) {
            minA = Math.min(minA, buffer[i]);
            maxA = Math.max(maxA, buffer[i]);
        }

        // scan second 1kHz period for peaks.
        for (int i = scan + 49; i < scan + 98; i++) {
            minB = Math.min(minB, buffer[i]);
            maxB = Math.max(maxB, buffer[i]);
        }

        value = (float) (maxA-minA) / (float) (maxB-minB);
//        value = (float) (maxA) / (float) (maxB);
//        if (value < 1) value = 1 / value;

//        float valb = (float) (minA) / (float) (minB);
//        if (valb < 1) valb = 1 / valb;

//        valueText.append("\nVmax: " + value + "  Vmin: " + valb);
        valueText.append("\nVal: " + value );

        Log.i(TAG, "\nMaxA:" + maxA);
        Log.i(TAG, "\nMinA:" + minA);
        Log.i(TAG, "\nMaxB:" + maxB);
        Log.i(TAG, "\nMinB:" + minB);
        Log.i(TAG, "\nValue:" + value);

    }

    public void rmsCalculate() {
        // start looking for a zero crossing at 200mS into the waveform.
        int scan = zeroCross( buffer, 8820, 75 );

        double[] rmsA = new double[16];
        double[] rmsB = new double[16];
//        rmsA = rms( buffer, scan, 49 );
//        rmsB = rms( buffer, scan+49, 49 );
//todo: Replace magic numbers

        for (int i = 0; i < 16; i++) {
            rmsA[i] = rms( buffer, scan + 49*(i*2), 49 );
            rmsB[i] = rms( buffer, scan + 49*(i*2+1), 49);
            Log.i(TAG, "A:B  " + rmsA[i] + " : " + rmsB[i]);
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

        value = (float) (rA/rB);
        if (value < 1) value = 1 / value;

        Log.i(TAG, "\nRMS A8:" + rA);
        Log.i(TAG, "\nRMS B8:" + rB);
        Log.i(TAG, "\nValue:" + value);

        valueText.append("  RMS: " + value);
    }
    /**
     * Calibrates the system using a previously recorded sample
     * Valid range is considered starting at 200mS for 100mS in length
     * Sample is 440 Hz, therefore one full waveform is 44100 / 440 = 100 samples long
     * Use two complete waveforms to ensure capturing the full sample
     * Sample rate is 44100, therefore start index is 44100 * .2 = 8820, end is 8820+200=9020
     */
    public void calibrate() {
        reference = 0;
        int max = Short.MIN_VALUE;
        int min = Short.MAX_VALUE;

        for (int i = 8820; i < 9020; i++) {
            max = Math.max(buffer[i], max);
            min = Math.min(buffer[i], min);
        }

        reference = (max - min) / 2;

        Log.i(TAG, "\nReference:" + reference);
        Log.i(TAG, "\nMax:" + max);
        Log.i(TAG, "\nMin:" + min);
    }

    public void wave(View v) {
        Intent intent = new Intent(this, WaveViewActivity.class);
        intent.putExtra("wave", buffer);
        startActivity(intent);
    }



    private class PlayLoopedSound extends AsyncTask<Void, Void, Void> {

        AudioTrack track;

        @Override
        protected void onPreExecute() {
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    tone.length, AudioTrack.MODE_STREAM);
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
        protected void onPostExecute( Void result ) {
            loopedToneIsRunning = false;
        }
    }

    public int zeroCross( short[] wave, int index, int wavelength ) {
        int slope;
        int maxSlope = 0;
        int maxSlopeIndex = 0;
        int start, end;

        // calculate the slope at each point in one wavelength
        // if this is the maximum slope, the zero cross is 1/2 way along this line
        for (int i = 0; i < wavelength; i++) {
            start = wave[i+index];
            end = wave[i+index+6];
            if( start<0 && end>0 ) {
                slope = end-start;
                if( slope > maxSlope ) {
                    maxSlopeIndex = i+3;
                }
            }
        }
        return maxSlopeIndex;
    }

    public double rms( short[] wave, int start, int length ) {
        long sum = 0;
        for (int i = start; i < start+length; i++) {
            sum += (long)wave[i] * (long)wave[i];
        }
        return Math.sqrt( (double)(sum)/ (double)(length) );
    }

    public void makeTone(double frequency, int sampleRate, double duration) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);

        tone = new short[period * waves];

        for (int i = 0; i < period; i++) {
            tone[i] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
        }

        // fill out rest of the array
        for (int i = 0; i < waves; ++i) {
            arraycopy( tone, 0, tone, i * period, period );
        }
    }

    public void makeSplitTone(double frequency, int sampleRate, double duration) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);
        waves &= 0xfffc;    // make waves a multiple of 4

        tone = new short[period * waves * 2];

        // first wave is Left Hand
        int lh = 0;
        int rh = period * 2;
        for (int i = 0; i < period; i++) {
            tone[rh++] = 0;
            tone[lh] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
            tone[rh++] = tone[lh++];
            tone[lh++] = 0;
        }

        // fill out rest of the array
        for (int i = 1; i < waves / 2; ++i) {
            arraycopy(tone, 0, tone, i * period * 4, period * 4);
        }
    }

//    private static void bubbleSort(double[] num) {
//        for (int i = 0; i < num.length; i++) {
//            for (int x = 1; x < num.length - i; x++) {
//                if (num[x - 1] > num[x]) {
//                    double temp = num[x - 1];
//                    num[x - 1] = num[x];
//                    num[x] = temp;
//                }
//            }
//        }
//    }
}

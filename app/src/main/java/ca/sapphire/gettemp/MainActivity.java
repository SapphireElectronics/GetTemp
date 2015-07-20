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

        tone = MakeTone.makeSplitTone(900, 44100, 1.0);
//        tone = MakeTone.makeBurstTone(900, 44100, 1.0);
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
            rmsCalculate();
//            burstCalculate();
//stopMeasuring();
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

//        calibrate();
    }

    /**
     * Calculates the current value
     */

    int reference = 0;
    float value = 0;


    public void burstCalculate() {
        // Step 1: looks for silence of more than 2 wavelengths
        // Step 2: Scans to end of silence by looking for signal
        // Step 3: Scan into first signal, get past noise etc.
        // Step 4: Looks for first signal peak
        // Step 5: Skips 3.5 wavelengths
        // Step 6: Scans for zero cross - this will be the cross of the start of the third full wave
        // Step 7: Read the next 16 A/B waveforms and RMS (may change this to 8)
        // Step 8: Sort the RMS values
        // Step 9: Toss away the highest and lowest one or two values
        // Step 10: Average the remaining values.

        double[] rmsA = new double[16];
        double[] rmsB = new double[16];

        int scan = 0;
        markers[0] = scan;
        Log.i( TAG, "Step length: " + buffer.length );


        // Steps 1 and 2
        int start;
        do {
            start = scan;
            while (Math.abs(buffer[scan]) < 8192)
                scan++;
        } while ( (scan-start) < 98 );
        Log.i( TAG, "Step 1,2: " + scan );
        markers[1] = scan;

        // Step 3
        while( buffer[scan] < 2048 )
            scan++;
        Log.i( TAG, "Step 3: " + scan );
        markers[3] = scan;

        // Step 4
        int level;
        do {
            level = buffer[scan++];
        } while ( buffer[scan] >= level );
        Log.i( TAG, "Step 4: " + scan );
        markers[4] = scan;

        // Step 5: 49 * 3.5 = 171
        scan += 171;
        Log.i( TAG, "Step 5: " + scan );
        markers[5] = scan;

        // Step 6
        scan = zeroCross( buffer, scan, 75, 6 );
        Log.i( TAG, "Step 6: " + scan );
        markers[6] = scan;

        // Step 7
        for (int i = 0; i < 16; i++) {
            rmsA[i] = rms( buffer, scan + 49*(i*2), 49 );
            rmsB[i] = rms( buffer, scan + 49*(i*2+1), 49);
            Log.i(TAG, "A:B  " + rmsA[i] + " : " + rmsB[i] );
        }

        // Step 8
        bubbleSort( rmsA );
        bubbleSort( rmsB );

        // Step 9 & 10
        double rA = 0, rB = 0;
        for (int i = 4; i < 12; i++) {
            rA += rmsA[i];
            rB += rmsB[i];
        }
        rA /= 8;
        rB /= 8;

        value = (float) (rB/rA);

        Log.i(TAG, "\nRMS A8:" + rA);
        Log.i(TAG, "\nRMS B8:" + rB);
        Log.i(TAG, "\nValue:" + value);

        valueText.append("  RMS: " + value);
    }

        /**
     * Calculates the current value using split tone.
     * Evaluates relative amplitudes of signals separately from LH and RH tones
     * TODO: put in a marker as LH and RH get lost on a mono input.
     */
    public void splitCalculate() {
    // start looking for a zero crossing at 200mS into the waveform.
        int scan = zeroCross( buffer, 8820, 75, 6 );

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
        if (value > 1) value = 1 / value;

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

    public void splitRangeCalculate() {
        // start looking for a zero crossing at 200mS into the waveform.

        int[] values = new int[8];
        double value;
        int scan = zeroCross( buffer, 8820, 75, 6 );

        Log.i(TAG, "\nZero:" + scan);


        for (int j = 0; j < 8; j++) {
            int minA = Short.MAX_VALUE;
            int maxA = Short.MIN_VALUE;
            int minB = Short.MAX_VALUE;
            int maxB = Short.MIN_VALUE;

//todo: Replace magic numbers
            // scan first 1kHz period for peaks.
            for (int i = scan + j*49; i < scan + (j+1)*49; i++) {
                minA = Math.min(minA, buffer[i]);
                maxA = Math.max(maxA, buffer[i]);
            }
            values[j] = maxA-minA;
        }

        bubbleSort( values );

        value = ((double)values[1]+(double)values[2]) / ((double)values[5]+(double)values[6]) ;

//        valueText.append("\nVmax: " + value + "  Vmin: " + valb);
        valueText.append("\nVal: " + value + " : " + 3300*(2*value-1));

//        Log.i(TAG, "\nMaxA:" + maxA);
//        Log.i(TAG, "\nMinA:" + minA);
//        Log.i(TAG, "\nMaxB:" + maxB);
//        Log.i(TAG, "\nMinB:" + minB);
        Log.i(TAG, "\nValue:" + value);

    }

    public void rmsCalculate() {
        // start looking for a zero crossing at 100mS into the waveform.
        // search 3 wavelengths to look for a good crossing
        int scan = zeroCross( buffer, 4410, 49*3, 6 );

        double[] rmsA = new double[16];
        double[] rmsB = new double[16];
//        rmsA = rms( buffer, scan, 49 );
//        rmsB = rms( buffer, scan+49, 49 );
//todo: Replace magic numbers

        for (int i = 0; i < 16; i++) {
            rmsA[i] = rms( buffer, scan + 49*(i*2), 49 );
            rmsB[i] = rms( buffer, scan + 49*(i*2+1), 49);
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

    public void rmsTernaryCalculate() {
        // start looking for a zero crossing at 100mS into the waveform.
        // search 3 wavelengths to look for a good crossing
        int scan = zeroCross( buffer, 4410, 49*3, 6 );

        double[] rmsA = new double[16];
        double[] rmsB = new double[16];
        double[] rmsC = new double[16];
//        rmsA = rms( buffer, scan, 49 );
//        rmsB = rms( buffer, scan+49, 49 );
//todo: Replace magic numbers

        rmsA[0] = rms( buffer, scan, 49 );
        rmsB[0] = rms( buffer, scan + 49, 49);
        rmsC[0] = rms( buffer, scan + 98, 49);

        Log.i(TAG, "A:B:C  " + rmsA[0] + " : " + rmsB[0] + " : " + rmsC[0]);

        // look for smallest RMS, it is the silent marker portion of the wave
        if (rmsA[0] < rmsC[0] ) {
            if( rmsA[0] < rmsB[0] ) {   // A is smallest, therefore B is reference, C is measurement
                scan += 49;             // Next scan will be the silence, so skip it.
                rmsA[0] = rmsB[0];      // copy reference and measurement as appropriate
                rmsB[0] = rmsC[0];
            }
            else {                      // B is smallest, therefore C is reference, A is measurement
                scan += 98;             // Next scan is the measurement, so skip that and the silence
                rmsB[0] = rmsA[0];      // copy reference and measurement as appropriate
                rmsA[0] = rmsC[0];
            }
        }
        // else                         // C is smallest, therefore A is reference, B is measurement
                                        // nothing to do in this case, we're in correct sync


        for (int i = 1; i < 16; i++) {
            rmsA[i] = rms( buffer, scan + 49*(i*3), 49 );
            rmsB[i] = rms( buffer, scan + 49*(i*3+1), 49);
            rmsC[i] = rms( buffer, scan + 49*(i*3+2), 49);
            Log.i(TAG, "A:B:C  " + rmsA[i] + " : " + rmsB[i] + " : " + rmsC[i]);
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
        intent.putExtra("mode", WaveView.MONO_MODE);
        intent.putExtra("markers", markers);
        startActivity(intent);
    }

    private class PlayLoopedSound extends AsyncTask<Void, Void, Void> {

        AudioTrack track;

        @Override
        protected void onPreExecute() {
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
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

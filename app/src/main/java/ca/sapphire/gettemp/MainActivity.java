package ca.sapphire.gettemp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.media.audiofx.AutomaticGainControl;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.lang.System.arraycopy;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GetTemp";

    Handler recordHandler = new Handler();
    Handler playHandler = new Handler();
    Handler measureHandler = new Handler();

    WaveView waveView = null;

    // 32768 samples at 44100 is 743 mS
    // it takes about 100mS for the tone to start
    // tone is 300mS long
    // therefore consider the data to be at 200mS into the sample for 100mS

    int status;    private int bufSize = 32768;

    private short[] buffer = new short[bufSize];

    TextView valueText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        valueText = (TextView) findViewById(R.id.ValueText);
//        makeTone( 1000, 44100, 1.0 );
        makeSplitTone(1000, 44100, 1.0);
        Log.i( TAG, "\nMinBufferSize = " + AudioTrack.getMinBufferSize( 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT ) );
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
//        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC,100 );
//        startRecording();
//        recordHandler.postDelayed(recordRunnable, 400);
//        tone.startTone(ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING);
        startMeasuring();
//        measureHandler.postDelayed(measureRunnable, 3000);

    }

    public void calibrate(View v) {
        calibrate();
    }

    /**
     * Calculates the current value
     */

    int reference = 0;
    int reading = 0;
    float value = 0;

    public void calculate() {
        if( reference == 0 ) {
            valueText.setText( "Not calibrated");
            return;
        }

        reading = 0;
        int max = Short.MIN_VALUE;
        int min = Short.MAX_VALUE;

        for (int i = 8820; i < 9020; i++) {
            max = Math.max(buffer[i], max);
            min = Math.min( buffer[i], min );
        }
        reading = (max-min)/2;

        value = (float)reading / (float)reference;

        valueText.setText( "Value = " + value);

        Log.i(TAG, "\nReading:" + reading);
        Log.i(TAG, "\nMax:" + max);
        Log.i(TAG, "\nMin:" + min);
        Log.i(TAG, "\nValue:" + value);

    }

    /**
     * Calculates the current value using split tone.
     * Evaluates relative amplitudes of signals separately from LH and RH tones
     * TODO: put in a marker as LH and RH get lost on a mono input.
     */
    public void splitCalculate() {
        int scan = 8820;    // start looking for a zero crossing at 200mS into the waveform.

        if( tone[scan] < 0) // look for zero cross
            while( buffer[++scan] < 0);
        else
            while( buffer[++scan] >= 0);

        Log.i(TAG, "\nZero:" + scan);

        int minA = Short.MAX_VALUE;
        int maxA = Short.MIN_VALUE;
        int minB = Short.MAX_VALUE;
        int maxB = Short.MIN_VALUE;

        // scan first 1kHz period for peaks.
        for (int i = scan; i < scan+44; i++) {
            minA = Math.min( minA, buffer[i] );
            maxA = Math.max( maxA, buffer[i] );
        }

        // scan second 1kHz period for peaks.
        for (int i = scan+44; i < scan+88; i++) {
            minB = Math.min( minB, buffer[i] );
            maxB = Math.max( maxB, buffer[i] );
        }

//        value = (float) (maxA-minA) / (float) (maxB-minB);
        value = (float) (maxA) / (float) (maxB);
        if( value < 1) value = 1/value;

        float valb = (float)(minA) / (float)(minB);
        if( valb < 1 ) valb = 1/valb;

        valueText.setText("Value = " + value + " : " + valb);

        Log.i(TAG, "\nMaxA:" + maxA);
        Log.i(TAG, "\nMinA:" + minA);
        Log.i(TAG, "\nMaxB:" + maxB);
        Log.i(TAG, "\nMinB:" + minB);
        Log.i(TAG, "\nValue:" + value);

    }
    
    /**
     * Calibrates the system using a previously recorded sample
     * Valid range is considered starting at 200mS for 100mS in length
     * Sample is 440 Hz, therefore one full waveform is 44100 / 440 = 100 samples long
     * Use two complete waveforms to ensure capturing the full sample
     * Sample rate is 44100, therefore start index is 44100 * .2 = 8820, end is 8820+200=9020
     *
     */
    public void calibrate() {
        reference = 0;
        int max = Short.MIN_VALUE;
        int min = Short.MAX_VALUE;

        for (int i = 8820; i < 9020; i++) {
            max = Math.max(buffer[i], max);
            min = Math.min( buffer[i], min );
        }

        reference = (max - min) / 2;

        Log.i(TAG, "\nReference:" + reference);
        Log.i(TAG, "\nMax:" + max);
        Log.i(TAG, "\nMin:" + min);
    }

    public void play(View v) {
        startPlaying();
        playHandler.postDelayed(playRunnable, 500);
    }

    public void wave(View v) {
        Intent intent = new Intent( this, WaveViewActivity.class );
        intent.putExtra( "wave", buffer );
        startActivity( intent );
    }


    private AudioRecord mRecorder = null;

    private AudioTrack mPlayer = null;


    private void startPlaying() {
        mPlayer = new AudioTrack( AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize, AudioTrack.MODE_STATIC );
//        buffer.rewind();
//        status = mPlayer.write( buffer, bufSize, AudioTrack.WRITE_NON_BLOCKING );
        status = mPlayer.write( buffer, 0, bufSize );
        Log.i( TAG, "\nPlay write status = " + status );
        mPlayer.play();
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    AutomaticGainControl agc = null;

    private void startMeasuring() {
//        mPlayer = new AudioTrack( AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufSize, AudioTrack.MODE_STATIC );
//        mPlayer.setLoopPoints( 0, 4400, -1 );
//        status = mPlayer.write( tone, 0, 8192 );
//        Log.i( TAG, "\nMeasure write status = " + status );

        startRecording();
        recordHandler.postDelayed(recordRunnable, 1000);

//        mPlayer.play();
        PlayLoopedSound playTone = new PlayLoopedSound();
        playTone.execute().getStatus();
    }

    private void stopMeasuring() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
//        bufSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.i(TAG, "\nBufsize = " + bufSize);
//        buffer = new byte[bufSize];

        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);
//        agc = AutomaticGainControl.create( mRecorder.getAudioSessionId() );
//        Log.i( TAG, "AGC : " + agc.getEnabled() );
//        agc.setEnabled(false);


        mRecorder.startRecording();
    }

    private void stopRecording() {
//        status = mRecorder.read(buffer, bufSize);
        status = mRecorder.read(buffer, 0, bufSize);
        Log.i( TAG, "\nRecord read status = " + status);
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            stopRecording();
//            calculate();
            splitCalculate();
        }
    };

    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            stopPlaying();
        }
    };

    Runnable measureRunnable = new Runnable() {
        @Override
        public void run() {
            stopMeasuring();
        }
    };

    short[] tone = null;

    public void makeTone( double frequency, int sampleRate, double duration ) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);

        tone = new short[period * waves];

        // fill out one waveform in the array
        int samples = (int) (sampleRate/frequency);

        int k = 0;
        for (int i = 0; i < period; i++) {
            tone[i] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
        }

        // fill out rest of the array
        for (int i = 0; i < waves; ++i) {
            int offset = i * period;
            for (int j = 0; j < period; j++) {
                tone[offset + j] = tone[j];
            }
        }
    }
    public void makeSplitTone( double frequency, int sampleRate, double duration ) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);
        waves &= 0xfffc;    // make waves a multiple of 4

        tone = new short[period * waves * 2];

        // fill out one waveform in the array
        int samples = (int) (sampleRate/frequency);

        // first wave is Left Hand
        int lh = 0;
        int rh = period*2;
        for (int i = 0; i < period; i++) {
            tone[rh++] = 0;
            tone[lh] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
            tone[rh++] = tone[lh++];
            tone[lh++] = 0;
        }

        // fill out rest of the array
        for (int i = 1; i < waves/2; ++i) {
            arraycopy( tone, 0, tone, i*period*4, period*4 );
        }
    }

}

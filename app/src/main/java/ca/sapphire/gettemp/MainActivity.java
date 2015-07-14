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


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GetTemp";

    Handler recordHandler = new Handler();
    Handler playHandler = new Handler();

    WaveView waveView = null;


    int status;    private int bufSize = 32768;
//    private ByteBuffer buffer = ByteBuffer.allocate( 32768 );

    private short[] buffer = new short[bufSize];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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
        ( (TextView) findViewById( R.id.StatusText) ).append( "Measure\n" );
    }

    public void calibrate(View v) {
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC,100 );

        startRecording();

        recordHandler.postDelayed(recordRunnable, 400);
        tone.startTone(ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING);

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


//        waveView = new WaveView( this, buffer, 4096, 400 );
//        setContentView(waveView);
//        waveView.invalidate();


    }

    private void startRecording() {
//        bufSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.i(TAG, "\nBufsize = " + bufSize);
//        buffer = new byte[bufSize];

        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);
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
        }
    };

    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            stopPlaying();
        }
    };
}

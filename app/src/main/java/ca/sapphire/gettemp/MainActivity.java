package ca.sapphire.gettemp;

import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    Handler recordHandler = new Handler();
    Handler playHandler = new Handler();

    int status;

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


    private static final String TAG = "GetTemp";
    private static String mFileName = null;

//    private RecordButton mRecordButton = null;
//    private MediaRecorder mRecorder = null;

    private AudioRecord mRecorder = null;

//    private PlayButton   mPlayButton = null;
//    private MediaPlayer   mPlayer = null;

    private AudioTrack mPlayer = null;


    private void startPlaying() {
        mPlayer = new AudioTrack( AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize, AudioTrack.MODE_STATIC );
        status = mPlayer.write( buffer, 0, bufSize );
        Log.i( TAG, "\nPlay write status = " + status );
        mPlayer.play();

//        mPlayer = new MediaPlayer();
//        try {
//            mPlayer.setDataSource(mFileName);
//            mPlayer.prepare();
//            mPlayer.start();
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "prepare() failed");
//        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private int bufSize;
    private byte[] buffer = null;

    private void startRecording() {
        bufSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        bufSize = 32768;
        Log.i( TAG, "\nBufsize = " + bufSize );
        buffer = new byte[bufSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);
        mRecorder.startRecording();

//        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mRecorder.setOutputFile(mFileName);
//        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

//        try {
//            mRecorder.prepare();
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "prepare() failed");
//        }

//        mRecorder.start();
    }

    private void stopRecording() {
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

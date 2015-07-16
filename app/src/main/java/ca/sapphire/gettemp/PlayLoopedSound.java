package ca.sapphire.gettemp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import static java.lang.System.arraycopy;

/**
 * Created by apreston on 7/16/2015.
 */
public class PlayLoopedSound extends AsyncTask<Void, Void, Void> {

    short[] tone;
    AudioTrack track;

    @Override
    protected void onPreExecute() {
        makeSplitTone( 1000, 44100, 1.0 );

        track = new AudioTrack( AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                tone.length, AudioTrack.MODE_STREAM);
        track.play();
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (int i = 0; i < 20; i++)
            track.write( tone, 0, tone.length );
        return null;
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

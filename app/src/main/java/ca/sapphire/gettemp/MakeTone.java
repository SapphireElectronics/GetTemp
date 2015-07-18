package ca.sapphire.gettemp;

import static java.lang.System.arraycopy;

/**
 *
 */
public final class MakeTone {

    /**
     *
     * @param frequency     Frequency of tone
     * @param sampleRate    Sample rate used when playing tone (use 44100)
     * @param duration      Duration of tone
     */
    public static short[] makeTone(double frequency, int sampleRate, double duration) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);

        short[] tone = new short[period * waves];

        for (int i = 0; i < period; i++) {
            tone[i] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
        }

        // fill out rest of the array
        for (int i = 0; i < waves; ++i) {
            arraycopy(tone, 0, tone, i * period, period);
        }

        return tone;
    }

    /**
     *
     * @param frequency     Frequency of generated tone
     * @param sampleRate    Sampling rate used when playing tone (use 44100)
     * @param duration      Duration of generated tone
     */
    public static short[] makeSplitTone(double frequency, int sampleRate, double duration) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);
        waves &= 0xfffc;    // make waves a multiple of 4

        short[] tone = new short[period * waves * 2];

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
        return tone;
    }
}

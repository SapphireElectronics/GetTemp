package ca.sapphire.gettemp;

import static java.lang.System.arraycopy;


/**
 * @author      Ashley Preston       <ashley.cobalt@gmail.com>
 * @version     1.0
 * @since       2015-07-18
 */

public final class MakeTone {
/**
 * Short one line description.                           (1)
 * <p>
 * Longer description. If there were any, it would be    [2]
 * here.
 * <p>
 * And even more explanations to follow in consecutive
 * paragraphs separated by HTML paragraph breaks.
 *
 * @param  variable Description text text text.          (3)
 * @return Description text text text.
 */



    /**
     * Generates a sine wave tone
     *
     * Generates a mono sine wave tone at a given frequency, for a given sample rate,
     * and for a given duration.
     *
     * The tone generated is a repeated sine wave which can either be played back using
     * a mono or stero stream.
     *
     * @param frequency     Frequency of tone
     * @param sampleRate    Sample rate used when playing tone (typically use 44100)
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
            arraycopy(tone, 0, tone, i*period, period);
        }

        return tone;
    }


    /**
     * Generates a special purpose sine wave tone
     *
     * Generates a stereo sine wave tone at a given frequency, for a given sample rate,
     * and for a given duration.  The tone alternates between LH and RH.
     *
     * The tone generated is in stereo.
     * The first complete waveform is LH only, the second complete waveform is RH only.
     * This is repeated for the duration of the tone.
     *
     * @param frequency     Frequency of generated tone
     * @param sampleRate    Sampling rate used when playing tone (typically use 44100)
     * @param duration      Duration of generated tone
     */
    public static short[] makeSplitTone(double frequency, int sampleRate, double duration) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) ( sampleRate / frequency );

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) ( duration * frequency );
        waves &= 0xfffc;    // make waves a multiple of 4 so playing successive stereo waves is contiguous

        short[] tone = new short[period * waves * 2];   // array twice as long because it's stereo

        // first wave is Left Hand, second wave is RH
        // both are generated together by writing the LH values one full period later to RH
        // since it is stereo, LH and RH are interleaved, so all calculations double
        int lh = 0;
        int rh = period * 2;
        for (int i = 0; i < period; i++) {
            tone[rh++] = 0;
            tone[lh] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
            tone[rh++] = tone[lh++];
            tone[lh++] = 0;
        }

        // fill out rest of the array, making copies of the two wave section
        for (int i = 1; i < waves / 2; ++i) {
            arraycopy(tone, 0, tone, i*period*4, period*4);
        }
        return tone;
    }

    /**
     * Generates a special purpose sine wave tone
     *
     * Generates a stereo sine wave tone at a given frequency, for a given sample rate,
     * and for a given duration.  The tone alternates between LH and RH and silent.
     *
     * The tone generated is in stereo.
     * The first complete waveform is LH only, the second complete waveform is RH only.
     * The third complete waveform is silent.
     * This is repeated for the duration of the tone.
     *
     * @param frequency     Frequency of generated tone
     * @param sampleRate    Sampling rate used when playing tone (typically use 44100)
     * @param duration      Duration of generated tone
     */
    public static short[] makeTernaryTone(double frequency, int sampleRate, double duration) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);
        waves = (waves/6) * 6;  // make waves a multiple of 6 so playing successive stereo waves is contiguous

        short[] tone = new short[period * waves * 2];

        // first wave is Left Hand, second wave is RH
        // both are generated together by writing the LH values one full period later to RH
        // since it is stereo, LH and RH are interleaved, so all calculations double
        int lh = 0;
        int rh = period * 2;
        for (int i = 0; i < period; i++) {
            tone[rh++] = 0;
            tone[lh] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
            tone[rh++] = tone[lh++];
            tone[lh++] = 0;
        }

        // next is a silent wavelength which we can ignore since defaults values are already 0.

        // fill out rest of the array, leaving one wavelength blank
        for (int i = 1; i < waves / 3; ++i) {
            arraycopy(tone, 0, tone, i*period*6, period*6);
        }
        return tone;
    }

    /**
     * Generates a special purpose sine wave tone
     *
     * Generates a stereo sine wave tone at a given frequency, for a given sample rate,
     * and for a given duration.  The tone alternates between LH and RH for 20 cycles
     * then silent for 4 cycles for 24 cycles total
     *
     * The tone generated is in stereo.
     * The first complete waveform is LH only, the second complete waveform is RH only.
     * This is repeated for a total of 20 cycles.
     * The next four complete waveforms are silent.
     *
     * This is repeated for the duration of the tone.
     *
     * The silence is used as a marker to allow synchronization to the tone
     *
     * @param frequency     Frequency of generated tone
     * @param sampleRate    Sampling rate used when playing tone (typically use 44100)
     * @param duration      Duration of generated tone
     */
    public static short[] makeBurstTone(double frequency, int sampleRate, double duration) {
        // number of samples in one wavelength = period = 1/f * samplerate
        int period = (int) (sampleRate / frequency);

        // number of wavelengths for a 'duration' length of tone frequency = duration * frequency
        int waves = (int) (duration * frequency);
        waves = (waves/48) * 48;  // make waves a multiple of 6 so playing successive stereo waves is contiguous

        short[] tone = new short[period * waves * 2];

        // first wave is Left Hand, second wave is RH
        // both are generated together by writing the LH values one full period later to RH
        // since it is stereo, LH and RH are interleaved, so all calculations double
        int lh = 0;
        int rh = period * 2;
        for (int i = 0; i < period; i++) {
            tone[rh++] = 0;
            tone[lh] = (short) ((Math.sin(2 * Math.PI * i / period)) * 32767);
            tone[rh++] = tone[lh++];
            tone[lh++] = 0;
        }

        // repeat this 19 times for a total of 20 cycles in the burst
        // fill out rest of the array, making copies of the two wave section
        for (int i = 1; i < 20; ++i) {
            arraycopy(tone, 0, tone, i*period*4, period*4);
        }

        // four blank cycles are left implicitly since initialized array is 0.

        // fill out rest of the array by copying the first 24 cycles
        // (24*2*2 shorts since it's stereo)
        for (int i = 1; i < waves/48; i++) {
            arraycopy(tone, 0, tone, i*period*96, period*96);
        }

        return tone;
    }
}

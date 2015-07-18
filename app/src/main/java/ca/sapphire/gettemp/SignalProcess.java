package ca.sapphire.gettemp;

/**
 * Signal Processing Functions
 */
public final class SignalProcess {

    /**
     * Sorts an array
     * @param num Array to be sorted
     */
    public static void bubbleSort(double[] num) {
        for (int i = 0; i < num.length; i++) {
            for (int x = 1; x < num.length - i; x++) {
                if (num[x - 1] > num[x]) {
                    double temp = num[x - 1];
                    num[x - 1] = num[x];
                    num[x] = temp;
                }
            }
        }
    }

    /**
     *
     * @param wave      Waveform to calculate RMS value for
     * @param start     Starting element in waveform
     * @param length    Number of elements in the RMS calculation
     * @return          The RMS value.
     */
    public static double rms( short[] wave, int start, int length ) {
        long sum = 0;
        for (int i = start; i < start+length; i++) {
            sum += (long)wave[i] * (long)wave[i];
        }
        return Math.sqrt( (double)(sum)/ (double)(length) );
    }
}

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

    public static void bubbleSort(int[] num) {
        for (int i = 0; i < num.length; i++) {
            for (int x = 1; x < num.length - i; x++) {
                if (num[x - 1] > num[x]) {
                    int temp = num[x - 1];
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

    /**
     *
     * @param wave          Waveform to inspect for a zero crossing
     * @param index         Starting element to examine for a zero cross
     * @param wavelength    Expected wavelength (distance to search for a zero cross)
     * @param span          Distance around the element to span when looking for a zero cross.  Use 1/8 to 1/4 wavelength
     * @return              Index of the value containing the zero crossing point
     */
    public static int zeroCross( short[] wave, int index, int wavelength, int span ) {
        int slope;
        int maxSlope = 0;
        int maxSlopeIndex = 0;
        int start, end;

        // calculate the slope at each point in one wavelength
        // if this is the maximum slope, the zero cross is 1/2 way along this line
        for (int i = 0; i < wavelength; i++) {
            start = wave[i+index];
            end = wave[i+index+span];
            if( start<0 && end>0 ) {
                slope = end-start;
                if( slope > maxSlope ) {
                    maxSlopeIndex = i+(span/2)+index;
                }
            }
        }
        return maxSlopeIndex;
    }
}

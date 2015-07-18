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
}

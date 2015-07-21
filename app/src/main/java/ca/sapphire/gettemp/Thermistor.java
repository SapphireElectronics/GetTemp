package ca.sapphire.gettemp;

import android.util.Log;

/**
 * Converts between temperatures and resistance values
 */
public final class Thermistor {
    // specified resistance from -40 to +150 in 5'C steps.
    final static double specification[] = {
            243448, 180772, 135623, 102751, 78576,  60623,  47168,  36995,  29240,  23280,
            18664,  15064,  12236,  10000,  8220.3, 6795.2, 5647.3, 4717.5, 3960.3, 3340.4,
            2830.3, 2408.6, 2058.4, 1766.2, 1521.4, 1315.4, 1141.4, 993.91, 868.35, 761.11,
            669.19, 590.14, 521.94, 462.92, 411.68, 367.08, 328.14, 294.05, 264.12
    };


    public static double temperature( double resistance ) {
        if( resistance > specification[0])
            return -999;
        if( resistance < specification[specification.length-1] )
            return 999;

        int index = 0;

        // locate bounding values in the array
        while( resistance < specification[index] )
            index++;

        index--;

        double slope = 5.0 / (specification[index+1]-specification[index]);
        double baseTemp = -40 + index*5;

        return baseTemp + slope * (resistance - specification[index]);
    }
}

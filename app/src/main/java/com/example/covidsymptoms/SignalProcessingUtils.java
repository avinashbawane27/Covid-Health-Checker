package com.example.covidsymptoms;

import java.util.ArrayList;

import static java.lang.Math.abs;

/**
 * Pure signal-processing helpers used to turn raw sensor/frame samples (accelerometer X values
 * or average frame redness values) into a beats/breaths-per-window count. Kept free of any
 * Android framework dependency so it can be exercised directly by plain JUnit tests.
 */
public final class SignalProcessingUtils {

    private SignalProcessingUtils() {
    }

    /**
     * Reduces noise such as irregular close peaks from input data using a moving average.
     * @param data ArrayList data to remove noise from (average redness values / accelerometer X values)
     * @param filter Window size to use for the moving average
     * @return Data with noise reduced
     */
    public static ArrayList<Integer> denoise(ArrayList<Integer> data, int filter) {

        ArrayList<Integer> movingAvgArr = new ArrayList<>();

        if (data == null || filter <= 0) {
            return movingAvgArr;
        }

        int movingAvg = 0;

        for (int i = 0; i < data.size(); i++) {
            movingAvg += data.get(i);
            if (i + 1 < filter) {
                continue;
            }
            movingAvgArr.add(movingAvg / filter);
            movingAvg -= data.get(i + 1 - filter);
        }

        return movingAvgArr;
    }

    /**
     * Calculates the number of times the sign of the slope of the data curve reverses (zero
     * crossings), used as a proxy for the number of peaks/troughs in a periodic signal.
     * @param data ArrayList data to analyse (average redness values / accelerometer X values)
     * @return Number of zero crossings, or 0 if there isn't enough data to determine a slope
     */
    public static int peakFinding(ArrayList<Integer> data) {

        if (data == null || data.size() < 2) {
            return 0;
        }

        int diff, prev, slope = 0, zeroCrossings = 0;
        int j = 0;
        prev = data.get(0);

        // Get initial slope
        while (slope == 0 && j + 1 < data.size()) {
            diff = data.get(j + 1) - data.get(j);
            if (diff != 0) {
                slope = diff / abs(diff);
            }
            j++;
        }

        // Get total number of zero crossings in the data curve
        for (int i = 1; i < data.size(); i++) {

            diff = data.get(i) - prev;
            prev = data.get(i);

            if (diff == 0) continue;

            int currSlope = diff / abs(diff);

            if (currSlope == -1 * slope) {
                slope *= -1;
                zeroCrossings++;
            }
        }

        return zeroCrossings;
    }
}

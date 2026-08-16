package com.example.covidsymptoms;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SignalProcessingUtilsTest {

    private static ArrayList<Integer> listOf(Integer... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    // ---- peakFinding ----

    @Test
    public void peakFinding_nullData_returnsZero() {
        assertEquals(0, SignalProcessingUtils.peakFinding(null));
    }

    @Test
    public void peakFinding_emptyData_returnsZero() {
        assertEquals(0, SignalProcessingUtils.peakFinding(listOf()));
    }

    @Test
    public void peakFinding_singleElement_returnsZero() {
        assertEquals(0, SignalProcessingUtils.peakFinding(listOf(5)));
    }

    @Test
    public void peakFinding_flatSignal_returnsZero() {
        assertEquals(0, SignalProcessingUtils.peakFinding(listOf(3, 3, 3, 3, 3)));
    }

    @Test
    public void peakFinding_singleSineLikeWave_countsEachDirectionReversal() {
        // Up, up, down, down, up, up: two direction reversals.
        assertEquals(2, SignalProcessingUtils.peakFinding(listOf(0, 1, 2, 1, 0, 1, 2)));
    }

    @Test
    public void peakFinding_isInvariantToFlatRunsBetweenPeaks() {
        // Plateaus (equal consecutive values) should be skipped, not counted as reversals.
        assertEquals(2, SignalProcessingUtils.peakFinding(listOf(0, 2, 2, 2, 4, 4, 1, 1, 5)));
    }

    @Test
    public void peakFinding_periodicSquareWave_countsExpectedCrossings() {
        // Alternating 0/10 zig-zag of 8 points: the first step sets the initial slope
        // (not counted), then each subsequent step reverses direction -> 6 reversals.
        assertEquals(6, SignalProcessingUtils.peakFinding(listOf(0, 10, 0, 10, 0, 10, 0, 10)));
    }

    // ---- denoise ----

    @Test
    public void denoise_nullData_returnsEmptyList() {
        assertTrue(SignalProcessingUtils.denoise(null, 5).isEmpty());
    }

    @Test
    public void denoise_dataShorterThanFilter_returnsEmptyList() {
        assertTrue(SignalProcessingUtils.denoise(listOf(1, 2, 3), 5).isEmpty());
    }

    @Test
    public void denoise_zeroOrNegativeFilter_returnsEmptyList() {
        assertTrue(SignalProcessingUtils.denoise(listOf(1, 2, 3), 0).isEmpty());
        assertTrue(SignalProcessingUtils.denoise(listOf(1, 2, 3), -1).isEmpty());
    }

    @Test
    public void denoise_constantSignal_isUnchanged() {
        ArrayList<Integer> result = SignalProcessingUtils.denoise(listOf(4, 4, 4, 4, 4, 4), 3);
        for (int value : result) {
            assertEquals(4, value);
        }
        assertEquals(4, result.size());
    }

    @Test
    public void denoise_slidingWindowAverage_matchesManualCalculation() {
        // filter = 3, so each output is the average of the previous 3 samples.
        ArrayList<Integer> result = SignalProcessingUtils.denoise(listOf(1, 2, 3, 4, 5, 6), 3);
        // windows: (1+2+3)/3=2, (2+3+4)/3=3, (3+4+5)/3=4, (4+5+6)/3=5
        assertEquals(listOf(2, 3, 4, 5), result);
    }

    @Test
    public void denoise_smoothsOutASingleSpike() {
        // A single large spike should be flattened by the moving average.
        ArrayList<Integer> raw = listOf(0, 0, 0, 100, 0, 0, 0);
        ArrayList<Integer> denoised = SignalProcessingUtils.denoise(raw, 3);
        for (int value : denoised) {
            assertTrue("Spike should be smoothed below raw peak of 100", value < 100);
        }
    }
}

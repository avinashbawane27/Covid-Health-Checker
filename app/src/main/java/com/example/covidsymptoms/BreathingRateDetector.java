package com.example.covidsymptoms;

import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;

public class BreathingRateDetector extends MainPage implements Runnable{

    public float breathingRate;
    ArrayList<Integer> accelValuesX;
    private String rootPath = Environment.getExternalStorageDirectory().getPath();

    BreathingRateDetector(ArrayList<Integer> accelValuesX){
        this.accelValuesX = accelValuesX;
    }

    @Override
    public void run() {
        String csvFilePath = rootPath + "/x_values.csv";
        saveToCSV(accelValuesX, csvFilePath);

        //Noise reduction from Accelerometer X values
        ArrayList<Integer> accelValuesXDenoised = SignalProcessingUtils.denoise(accelValuesX, 10);

        csvFilePath = rootPath + "/x_values_denoised.csv";
        saveToCSV(accelValuesXDenoised, csvFilePath);

        //Peak detection algorithm running on denoised Accelerometer X values
        if (accelValuesXDenoised.size() < 2) {
            Log.e("log", "Not enough accelerometer samples to compute respiratory rate");
            breathingRate = 0;
            return;
        }

        int zeroCrossings = SignalProcessingUtils.peakFinding(accelValuesXDenoised);
        breathingRate = (zeroCrossings * 60f) / 90;
        Log.i("log", "Respiratory rate" + breathingRate);
    }
}

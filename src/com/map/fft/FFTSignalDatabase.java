package com.map.fft;

import android.util.Log;

public class FFTSignalDatabase {
	private int DETECT_THRESHOLD = 2;
	private int currentCount = 0;

	public FFTSignal searchChunk(int vals[]) {
		FFTSignal sig = null;
		int maxVal = -1;
		int maxIdx = -1;

		for (int j = 100; j < 250; j++) {
			int mag = Math.abs(vals[j]);
			if (mag > maxVal) {
				maxVal = mag;
				maxIdx = j;
			}
		}

		Log.i("FFTSignalDatabase", "Max Index: " + maxIdx);

		if (maxIdx > 180 && maxIdx < 187) {
			currentCount++;
		}
		else {
			currentCount = 0;
		}

		if (currentCount > DETECT_THRESHOLD) {
			sig = new FFTSignal("1000Hz Tone");
		}

		return sig;
	}
}

package com.map.fft;

import android.util.Log;

public class FFTSignalDatabase {
	// How many hits in a row before detection
	static final int DETECT_THRESHOLD = 7;
	static final int MAX_COUNT = 12;

	// Frequency range to search
	static final int MIN_FREQ = 1200;
	static final int MAX_FREQ = 2200;

	// Minimum amplitude to trigger a 'hit'
	static final int MIN_AMP = 150000;

	// Maximum allowable change in frequency between two samples
	// This is used to detect only relatively "smooth", continuous signals
	static final int MAX_FREQ_DELTA = 600;

	private int currentCount = 0;
	private int curFreq;
	private final int sampleRate;
	private final int bufferSize;
	private final int minIndex;
	private final int maxIndex;
	private FFTSignal curSig = null;

	public FFTSignalDatabase(int sampleRate, int bufferSize) {
		this.sampleRate = sampleRate;
		this.bufferSize = bufferSize;
		minIndex = freqToIndex(MIN_FREQ);
		maxIndex = freqToIndex(MAX_FREQ);
	}

	public FFTSignal searchChunk(int vals[]) {
		FFTSignal sig = null;
		int maxVal = -1;
		int maxIdx = -1;

		// Search for the loudest frequency in the detection range
		for (int i = minIndex; i < maxIndex; i++) {
			int mag = Math.abs(vals[i]);
			if (mag > maxVal) {
				maxVal = mag;
				maxIdx = i;
			}
		}

		int maxFreq = indexToFreq(maxIdx);

		if (maxVal >= MIN_AMP) {
			// Signal strength was sufficient, check continuity
			if (currentCount > 0) {
				// We have a previous hit to compare the signal against
				if (Math.abs(maxFreq - curFreq) < MAX_FREQ_DELTA) {
					// Frequency is within expected range, accept
					Log.i("FFTSignalDatabase", "Frequency Delta: "
							+ (maxFreq - curFreq) + "HZ");
					currentCount++;
				} else {
					// Frequency is outside of expected range, reset
					currentCount--;
				}
			} else {
				// This is the first signal, just accept it
				currentCount = 1;
			}
			curFreq = maxFreq;
		} else {
			// Signal strength insufficient, reset regardless
			currentCount--;
		}

		if (curSig != null) {
			if (currentCount > 0) {
				sig = curSig;
			} else {
				curSig = null;
			}
		} else if (currentCount >= DETECT_THRESHOLD) {
			// Enough hits detected in a row, signal detected
			curSig = new FFTSignal("Probable Siren");
			sig = curSig;
			Log.i("FFTSignalDatabase", "Signal Detected at " + maxFreq + "HZ");

			if (currentCount > MAX_COUNT) {
				currentCount = MAX_COUNT;
			}
		}

		return sig;
	}

	public int indexToFreq(int index) {
		return (index * sampleRate) / bufferSize;
	}

	public int freqToIndex(int freq) {
		return (freq * bufferSize) / sampleRate;
	}
}

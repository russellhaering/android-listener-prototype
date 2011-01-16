package com.map.fft;

public class FFT {

	public static int[] realFFT(short datain[], int start, int len) {
		// TODO: This whole thing is horribly inefficient
		int i;
		int data[] = new int[len];
		float fdata[] = new float[len * 2];

		// Build an array of complex floats
		for (i = 0; i < len; i++) {
			fdata[2 * i] = datain[start + i];
			// TODO: This may not be necessary
			fdata[2 * i + 1] = 0;
		}

		fdata = doFFT(fdata);

		for (i = 0; i < len; i++) {
			data[i] = (int) fdata[i * 2];
		}

		return data;
	}

	/**
	 * Performs a discrete Fourier transform.
	 *
	 * Replaces data[1..2*nn] by its discrete Fourier transform, if isign is
	 * input as 1; or replaces data[1..2*nn] by nn times its inverse discrete
	 * Fourier transform, if isign is input as \u22121. data is a complex array
	 * of length nn or, equivalently, a real array of length 2*nn. nn MUST be an
	 * integer power of 2 (this is not checked for!).
	 *
	 * @param datain
	 *            The data input array
	 * @return Returns the resulting array
	 */
	private static float[] doFFT(float datain[]) {
		int n = datain.length;
		float data[] = new float[n];
		int mmax, m, j, istep, i;
		double wtemp, wr, wpr, wpi, wi, theta;
		double tempr, tempi;

		j = 0;

		// This is the bit-reversal section of the routine.
		for (i = 0; i < n - 1; i += 2) {

			// Swap the real parts. The complex parts are both 0 and needn't
			// be swapped
			if (j >= i) {
				data[j] = datain[i];
				data[i] = datain[j];
			}
			m = n / 2;
			while (m >= 2 && j >= m) {
				j -= m;
				m = m / 2;
			}

			j += m;
		}

		// Here begins the Danielson - Lanczos section of the routine.
		mmax = 2;

		// Outer loop executed log2 nn times.
		while (n > mmax) {
			istep = mmax * 2;
			// Initialize the trigonometric recurrence.
			theta = (2 * Math.PI / mmax);
			wpr = Math.cos(theta);

			wpi = Math.sin(theta);
			wr = 1.0;
			wi = 0.0;
			// Here are the two nested inner loops.
			for (m = 0; m < mmax - 1; m += 2) {
				for (i = m; i < n - 2; i += istep) {
					// This is the Danielson-Lanczos formula:
					j = i + mmax;
					tempr = wr * data[j] - wi * data[j + 1];
					tempi = wr * data[j + 1] + wi * data[j];
					data[j] = data[i] - (float) tempr;
					data[j + 1] = data[i + 1] - (float) tempi;
					data[i] += tempr;
					data[i + 1] += tempi;
				}
				// Trigonometric recurrence.
				wr = (wtemp = wr) * wpr - wi * wpi;
				wi = wi * wpr + wtemp * wpi;
			}
			mmax = istep;
		}

		return data;
	}
};
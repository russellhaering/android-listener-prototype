package com.map.fft;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;

public class FFTApplication extends Application {
	private FFTServiceStatus fftServiceStatus = FFTServiceStatus.STOPPED;
	private List<ServiceStatusListener> fftServiceStatusListeners = new ArrayList<ServiceStatusListener>();

	public void setFFTServiceStatus(FFTServiceStatus status) {
		this.fftServiceStatus = status;
		for (ServiceStatusListener l: fftServiceStatusListeners) {
			l.onServiceStatusUpdate(status);
		}
	}

	public FFTServiceStatus getFFTServiceStatus() {
		return fftServiceStatus;
	}

	public void addFFTServiceStatusListener(ServiceStatusListener l) {
		fftServiceStatusListeners.add(l);
	}

	public void removeFFTServiceStatusListener(ServiceStatusListener l) {
		fftServiceStatusListeners.remove(l);
	}
}

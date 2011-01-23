package com.map.fft;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;

public class FFTApplication extends Application {
	private Status fftServiceStatus = Status.STOPPED;
	private Status fftAlertStatus = Status.STOPPED;
	private List<StatusListener> fftServiceStatusListeners = new ArrayList<StatusListener>();
	private List<StatusListener> fftAlertStatusListeners = new ArrayList<StatusListener>();

	public void setFFTServiceStatus(Status status) {
		this.fftServiceStatus = status;
		for (StatusListener l: fftServiceStatusListeners) {
			l.onServiceStatusUpdate(status);
		}
	}

	public Status getFFTServiceStatus() {
		return fftServiceStatus;
	}

	public void addFFTServiceStatusListener(StatusListener l) {
		fftServiceStatusListeners.add(l);
	}

	public void removeFFTServiceStatusListener(StatusListener l) {
		fftServiceStatusListeners.remove(l);
	}

	public Status getFFTAlertStatus() {
		return fftAlertStatus;
	}

	public void addFFTAlertStatusListener(StatusListener l) {
		fftAlertStatusListeners.add(l);
	}

	public void removeFFTAlertStatusListener(StatusListener l) {
		fftAlertStatusListeners.remove(l);
	}

	public void setFFTAlertStatus(Status status) {
		this.fftAlertStatus = status;
		for (StatusListener l: fftAlertStatusListeners) {
			l.onServiceStatusUpdate(status);
		}
	}
}

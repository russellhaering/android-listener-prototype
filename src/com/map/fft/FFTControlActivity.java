package com.map.fft;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class FFTControlActivity extends Activity {
	private Button ctlButton;

	private String BUTTON_START_STRING = "Start Service";
	private String BUTTON_STOP_STRING = "Stop Service";

	private Intent listenerServiceIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Set up the control button
		ctlButton = (Button) findViewById(R.id.ctlButton);
		ctlButton.setOnClickListener(ctlButtonListener);
		fftStatusListener
				.onServiceStatusUpdate((((FFTApplication) getApplicationContext())
						.getFFTServiceStatus()));
		((FFTApplication) getApplicationContext())
				.addFFTServiceStatusListener(fftStatusListener);

		listenerServiceIntent = new Intent(getApplicationContext(),
				FFTListenerService.class);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		((FFTApplication) getApplicationContext())
				.removeFFTServiceStatusListener(fftStatusListener);
	}

	private OnClickListener ctlButtonListener = new OnClickListener() {
		public void onClick(View v) {
			Status s = ((FFTApplication) getApplicationContext())
					.getFFTServiceStatus();
			switch (s) {
			case STOPPED:
				startService(listenerServiceIntent);
				break;
			case STARTED:
				stopService(listenerServiceIntent);
				break;
			}
		}
	};

	private StatusListener fftStatusListener = new StatusListener() {
		public void onServiceStatusUpdate(Status status) {
			switch (status) {
			case STOPPED:
				ctlButton.setText(BUTTON_START_STRING);
				break;
			case STARTED:
				ctlButton.setText(BUTTON_STOP_STRING);
				break;
			}
		}
	};
}
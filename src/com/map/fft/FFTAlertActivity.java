package com.map.fft;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;

public class FFTAlertActivity extends Activity {
	private TextView topArea;
	private View bottomArea;
	private Handler viewFlipHandler = new Handler();
	private int viewState = 0;

	private long[] vibratePattern = new long[] {0, 500, 500};
	private int vibrateIndex = 1;
	private Vibrator vib;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alert);
		((FFTApplication) getApplicationContext())
				.setFFTAlertStatus(Status.STARTED);

		topArea = (TextView) findViewById(R.id.alertTopArea);
		bottomArea = findViewById(R.id.alertBottomArea);
		vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
	}

	@Override
	public void onResume() {
		super.onResume();
		viewFlipHandler.post(viewColorFlipper);
		vib.vibrate(vibratePattern, vibrateIndex);
	}

	@Override
	public void onPause() {
		super.onPause();
		viewFlipHandler.removeCallbacks(viewColorFlipper);
		vib.cancel();
	}

	private Runnable viewColorFlipper = new Runnable() {
		@Override
		public void run() {
			if (viewState == 0) {
				topArea.setBackgroundColor(Color.RED);
				topArea.setTextColor(Color.WHITE);
				bottomArea.setBackgroundColor(Color.WHITE);
				viewState = 1;
			}
			else {
				topArea.setBackgroundColor(Color.WHITE);
				topArea.setTextColor(Color.RED);
				bottomArea.setBackgroundColor(Color.RED);
				viewState = 0;
			}

			viewFlipHandler.postDelayed(viewColorFlipper, 1000);
		}
	};
}
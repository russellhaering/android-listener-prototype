package com.map.fft;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FFTAlertActivity extends Activity {
	private TextView topArea;
	private View bottomArea;
	private Handler viewFlipHandler = new Handler();
	private int viewState = 0;

	private long[] vibratePattern = new long[] {0, 500, 500};
	private int vibrateIndex = 1;
	private Vibrator vib;
	private BroadcastReceiver endReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alert);
		((FFTApplication) getApplicationContext())
				.setFFTAlertStatus(Status.STARTED);

		topArea = (TextView) findViewById(R.id.alertTopArea);
		bottomArea = findViewById(R.id.alertBottomArea);
		vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		
		endReceiver = new SirenEndReceiver();
		IntentFilter endFilter = new IntentFilter(FFTListenerService.SIREN_END);
		this.registerReceiver(endReceiver, endFilter);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
							| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
							| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
							| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onResume() {
		super.onResume();
		viewFlipHandler.post(viewColorFlipper);
		vib.vibrate(vibratePattern, vibrateIndex);
	}

	@Override
	public void onPause() {
		// TODO: Throw something up in the notification area?
		super.onPause();
		viewFlipHandler.removeCallbacks(viewColorFlipper);
		vib.cancel();
	}

	private Runnable viewColorFlipper = new Runnable() {
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
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(endReceiver);
	}
	
	private class SirenEndReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			FFTAlertActivity.this.finish();
		}
	}
}
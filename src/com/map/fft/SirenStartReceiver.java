package com.map.fft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SirenStartReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent alertIntent = new Intent(context, FFTAlertActivity.class);
		alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(alertIntent);
	}

}

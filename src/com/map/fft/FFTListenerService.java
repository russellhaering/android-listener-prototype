package com.map.fft;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class FFTListenerService extends Service {
	// Tag for log messages from this class
	private static String TAG = "FFTListenerService";

	// Text to be used in the notification
	private static String LISTENER_NOTIFICATION_TITLE = "Listening...";
	private static String LISTENER_NOTIFICATION_BODY = "Listener is running";

	// The ID of the service notification
	private static int LISTENER_FOREGROUND_ID = 0;

	private Notification foregroundNotification;
	private NotificationManager notificationManager;

	@Override
	public void onCreate() {
		// TODO
		Log.d("FFTListenerService", "Service Created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Construct the notification to be shown (without a time displayed)
		foregroundNotification = new Notification();
		foregroundNotification.icon = R.drawable.icon;
		foregroundNotification.when = 0;

		// The notification is ongoing and may not be cleared
		foregroundNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		foregroundNotification.flags |= Notification.FLAG_NO_CLEAR;

		// Initialize the status to a 'listener is running' message, and set it
		// to open the service manager when clicked
		Intent managerIntent = new Intent(this, FFTControlActivity.class);
		foregroundNotification.setLatestEventInfo(this,
				LISTENER_NOTIFICATION_TITLE,
				LISTENER_NOTIFICATION_BODY,
				PendingIntent.getActivity(this, 0, managerIntent, 0));

		// Set that notification
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(LISTENER_FOREGROUND_ID, foregroundNotification);
		startForeground(LISTENER_FOREGROUND_ID, foregroundNotification);

		// Notify any interested UI features
		((FFTApplication) getApplicationContext()).setFFTServiceStatus(FFTServiceStatus.STARTED);
		Log.i(TAG, "Service Started");

		// This service is "sticky" and should not be stopped until we say so
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		((FFTApplication) getApplicationContext()).setFFTServiceStatus(FFTServiceStatus.STOPPED);
		stopForeground(true);
		notificationManager.cancel(LISTENER_FOREGROUND_ID);
		Log.i(TAG, "Service Stopped");
	}
}

package com.map.fft;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class FFTListenerService extends Service {
	// Broadcast identifiers
	public static final String SIREN_BEGIN = "com.map.fft.intent.action.SIREN_BEGIN";
	public static final String SIREN_END = "com.map.fft.intent.action.SIREN_END";

	// Tag for log messages from this class
	private static String TAG = "FFTListenerService";

	// Text to be used in the notification
	private static String LISTENER_NOTIFICATION_TITLE = "Listening...";
	private static String LISTENER_NOTIFICATION_BODY = "Listener is running";

	// The ID of the service notification
	private static int LISTENER_FOREGROUND_ID = 0;

	// The notification and notification manager
	private Notification foregroundNotification;
	private NotificationManager notificationManager;

	// Siren alert timings
	private final long BEGIN_DELAY = 5 * 1000;
	private final long END_DELAY = 5 * 1000;

	// The Handler and AudioRecord for recording
	private Handler micHandler;

	@Override
	public void onCreate() {
		Log.i(TAG, "Service Created");
	}

	/**
	 * Called on service start.
	 * 
	 * This method is called when the service is started by the Control
	 * Activity. When the service is started we must first construct a new
	 * notification and register it with the notification manager, then update
	 * the service status to STARTED in the Application (so that the Control
	 * Activity can update its UI) and finally start the background thread that
	 * reads data from the microphone.
	 */
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
				LISTENER_NOTIFICATION_TITLE, LISTENER_NOTIFICATION_BODY,
				PendingIntent.getActivity(this, 0, managerIntent, 0));

		// Set that notification
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(LISTENER_FOREGROUND_ID,
				foregroundNotification);
		startForeground(LISTENER_FOREGROUND_ID, foregroundNotification);

		// Notify any interested UI features
		((FFTApplication) getApplicationContext())
				.setFFTServiceStatus(Status.STARTED);

		// Set up the microphone buffering
		micThread.start();

		// This service is "sticky" and should not be stopped until we say so
		Log.i(TAG, "Service Started");
		return START_STICKY;
	}

	/**
	 * Reject bind attempts.
	 * 
	 * This is called when someone attempts to bind to this service. We don't
	 * currently implement any controllable behavior sufficiently complex to
	 * justify providing such an interface, so we simply reject these attempts.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Handle destruction of the service.
	 * 
	 * When someone wants to destroy the service we must reverse the actions
	 * taken when the service was started, so we pass the background thread a
	 * runnable that will cause it to stop, set the service status to STOPPED
	 * then cancel the services running notification.
	 */
	@Override
	public void onDestroy() {
		micHandler.postAtFrontOfQueue(micStopper);

		((FFTApplication) getApplicationContext())
				.setFFTServiceStatus(Status.STOPPED);
		stopForeground(true);
		notificationManager.cancel(LISTENER_FOREGROUND_ID);
		Log.i(TAG, "Service Stopped");
	}

	/**
	 * A Runnable used to stop the microphone thread.
	 * 
	 * This runnable can be queued in the handler for the microphone thread in
	 * order to cleanly stop the thread. It stops the microphone listener,
	 * releases its system resources, then causes the looper to exit.
	 */
	private Runnable micStopper = new Runnable() {
		public void run() {
			Looper.myLooper().quit();
		}
	};

	private Runnable startAlert = new Runnable() {
		public void run() {
			sendBroadcast(new Intent(SIREN_BEGIN));
			micHandler.postDelayed(stopAlert, END_DELAY);
		}
	};

	private Runnable stopAlert = new Runnable() {
		public void run() {
			sendBroadcast(new Intent(SIREN_END));
			micHandler.postDelayed(startAlert, BEGIN_DELAY);
		}
	};

	private Thread micThread = new Thread() {
		@Override
		public void run() {
			Looper.prepare();
			micHandler = new Handler();
			micHandler.postDelayed(startAlert, BEGIN_DELAY);
			Looper.loop();
		}
	};
}

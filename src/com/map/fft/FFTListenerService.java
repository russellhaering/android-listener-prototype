package com.map.fft;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class FFTListenerService extends Service {
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

	private int AUDIO_SOURCE = AudioSource.MIC;
	private int SAMPLE_RATE = 22050;
	private int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private int BUFFER_SIZE_FACTOR = 32;
	private int bufSize;
	private AudioRecord micRecord;
	private Handler micHandler;

	@Override
	public void onCreate() {
		Log.i(TAG, "Service Created");
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

		// Set up the microphone buffering
		micThread.start();

		// This service is "sticky" and should not be stopped until we say so
		Log.i(TAG, "Service Started");
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		micHandler.postAtFrontOfQueue(micStopper);

		((FFTApplication) getApplicationContext()).setFFTServiceStatus(FFTServiceStatus.STOPPED);
		stopForeground(true);
		notificationManager.cancel(LISTENER_FOREGROUND_ID);
		Log.i(TAG, "Service Stopped");
	}

	private Runnable micStopper = new Runnable() {
		@Override
		public void run() {
			synchronized (micRecord) {
				micRecord.stop();
				micRecord.release();
				micRecord = null;
				Looper.myLooper().quit();
			}
		}
	};

	private Thread micThread = new Thread() {
		@Override
		public void run() {
			Looper.prepare();

			micHandler = new Handler();

			bufSize = BUFFER_SIZE_FACTOR *
				AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

			micRecord = new AudioRecord(AUDIO_SOURCE,
				SAMPLE_RATE,
				CHANNEL_CONFIG,
				AUDIO_FORMAT,
				bufSize);

			synchronized (micRecord) {
				micRecord.setRecordPositionUpdateListener(micBufferListener, micHandler);
				micRecord.setPositionNotificationPeriod(bufSize / 2);
				micRecord.startRecording();
				micRecord.read(new short[bufSize], 0, bufSize);
			}

			Looper.loop();
		}

		private OnRecordPositionUpdateListener micBufferListener = new OnRecordPositionUpdateListener() {
			@Override
			public void onMarkerReached(AudioRecord recorder) {
				// This method is required to exist
			}

			@Override
			public void onPeriodicNotification(AudioRecord recorder) {
				Log.i(TAG, "Period Callback Fired");
				short audioBuffer[] = new short[bufSize];
				synchronized (micRecord) {
					micRecord.read(audioBuffer, 0, bufSize);
					Log.i(TAG, "Completed Read");
				}
			}
		};
	};
}

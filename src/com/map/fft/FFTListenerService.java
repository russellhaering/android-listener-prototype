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

	private int AUDIO_SOURCE = AudioSource.MIC;
	private int SAMPLE_RATE = 22050;
	private int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private int BUFFER_SIZE = 4096 * 4;
	private AudioRecord micRecord;
	private Handler micHandler;
	private int CHUNK_SIZE = 4096;

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
		((FFTApplication) getApplicationContext()).setFFTServiceStatus(Status.STARTED);

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

		((FFTApplication) getApplicationContext()).setFFTServiceStatus(Status.STOPPED);
		stopForeground(true);
		notificationManager.cancel(LISTENER_FOREGROUND_ID);
		Log.i(TAG, "Service Stopped");
	}

	private Runnable micStopper = new Runnable() {
		public void run() {
			micRecord.stop();
			micRecord.release();
			micRecord = null;
			Looper.myLooper().quit();
		}
	};

	private Thread micThread = new Thread() {
		private short audioBuffer[];
		private FFTSignalDatabase db;
		private int sirenTimeout = 0;

		@Override
		public void run() {
			Looper.prepare();

			db = new FFTSignalDatabase(SAMPLE_RATE, CHUNK_SIZE);

			micHandler = new Handler();

			Log.i(TAG, "Buffer Size = " + BUFFER_SIZE);

			audioBuffer = new short[BUFFER_SIZE];

			micRecord = new AudioRecord(AUDIO_SOURCE,
				SAMPLE_RATE,
				CHANNEL_CONFIG,
				AUDIO_FORMAT,
				BUFFER_SIZE * 2);

			micRecord.setRecordPositionUpdateListener(micBufferListener, micHandler);
			micRecord.setPositionNotificationPeriod(BUFFER_SIZE);
			micRecord.startRecording();
			micRecord.read(audioBuffer, 0, BUFFER_SIZE);

			Looper.loop();
		}

		private OnRecordPositionUpdateListener micBufferListener = new OnRecordPositionUpdateListener() {
			public void onMarkerReached(AudioRecord recorder) {
				// This method is required to exist
			}

			public void onPeriodicNotification(AudioRecord recorder) {
				micRecord.read(audioBuffer, 0, BUFFER_SIZE);

				for (int i = 0; i < BUFFER_SIZE; i += CHUNK_SIZE) {
					int[] vals = FFT.realFFT(audioBuffer, i, CHUNK_SIZE);
					FFTSignal sig = db.searchChunk(vals);

					if (sig != null) {
						if (sirenTimeout == 0) {
							sendBroadcast(new Intent(SIREN_BEGIN));
						}
						sirenTimeout = 25;
					} else {
						if (sirenTimeout == 1) {
							sendBroadcast(new Intent(SIREN_END));
						}
						if (sirenTimeout != 0) {
							sirenTimeout--;
						}
					}
				}
			}
		};
	};
}

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

	// Recording parameters
	private int AUDIO_SOURCE = AudioSource.MIC;
	private int SAMPLE_RATE = 22050;
	private int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private int CHUNK_SIZE = 4096;
	private int BUFFER_SIZE = CHUNK_SIZE * 4;
	private int SIREN_TIMEOUT = 25;

	// The Handler and AudioRecord for recording
	private AudioRecord micRecord;
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
			micRecord.stop();
			micRecord.release();
			micRecord = null;
			Looper.myLooper().quit();
		}
	};

	/**
	 * A thread that gathers and analyzes data from the microphone.
	 * 
	 * Because calling read() to gather data from the microphone is a blocking
	 * operation, we perform it in a background thread so that the main service
	 * thread doesn't lock.
	 */
	private Thread micThread = new Thread() {
		// Audio recording and analysis state
		private short audioBuffer[];
		private FFTSignalDatabase db;
		private int sirenTimeout = 0;

		/**
		 * Run the microphone thread.
		 * 
		 * The microphone threads job is to gather data from the microphone,
		 * pass it to the signal database for analysis, then respond to any
		 * signals (that is, Sirens) reported by the database. The thread
		 * handles jobs using a Handler that actually belongs to the
		 * encapsulating FFTListenerService - the advantage of this being that
		 * the listener service can post a job to the Handler that causes the
		 * microphone thread to exit cleanly.
		 * 
		 * To prepare for all of this we instantiate the signal database and
		 * handler, then instantiate a new AudioRecord to read data from the
		 * microphone. We instruct the AudioRecord to trigger a listener after
		 * each segment of audio is recorded, then kick off the first such
		 * recording. Finally, we start the thread's Looper which will allow the
		 * Handler to begin handling jobs.
		 */
		@Override
		public void run() {
			Looper.prepare();

			db = new FFTSignalDatabase(SAMPLE_RATE, CHUNK_SIZE);

			micHandler = new Handler();

			Log.i(TAG, "Buffer Size = " + BUFFER_SIZE);

			audioBuffer = new short[BUFFER_SIZE];

			micRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE,
					CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE * 2);

			micRecord.setRecordPositionUpdateListener(micBufferListener,
					micHandler);
			micRecord.setPositionNotificationPeriod(BUFFER_SIZE);
			micRecord.startRecording();
			micRecord.read(audioBuffer, 0, BUFFER_SIZE);

			Looper.loop();
		}

		/**
		 * Handle data from the microphone.
		 * 
		 * The onPeriodNotification method of this class will be fired each time
		 * a BUFFER_SIZE sized segment of audio has been recorded. We pass this
		 * buffer to the signal database, and get back an FFTSignal. If the
		 * signal is non-null, we broadcast a SIREN_BEGIN then set a counter to
		 * SIREN_TIMEOUT. Every subsequent period that no signal is detected,
		 * this counter is decremented, but any time a signal is detected it is
		 * reset to SIREN_TIMEOUT. When the timer reaches 0 we broadcast a
		 * SIREN_END.
		 */
		private OnRecordPositionUpdateListener micBufferListener = new OnRecordPositionUpdateListener() {
			// This method is required to exist but we don't use it
			public void onMarkerReached(AudioRecord recorder) {
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
						sirenTimeout = SIREN_TIMEOUT;
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

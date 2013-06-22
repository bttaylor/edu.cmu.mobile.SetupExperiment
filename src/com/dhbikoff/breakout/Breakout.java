package com.dhbikoff.breakout;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Activity for the running game. Holds the game's graphics thread. Saves and
 * restores game data when paused or resumed.
 * 
 */
public class Breakout extends Activity implements SensorEventListener {

	private boolean sound;
	private GameView gameView;
	//private int level;
	private int seq_num;

	// parameters received from SetupExperiment.java
	private String username;
//	private int latency;
	private int mode;
	
	// recon-mod sensor references
    private SensorManager sm;
    private Sensor accelerometer;
//    private Sensor magnetometer;
//    private Sensor gyroscope;
//    private Sensor altimeter;
//    private Sensor recon_free_fall;
//    private Sensor recon_sync;
//    private Sensor gravity_sensor;
//    private Sensor linear_sensor;
//    private Sensor rotation_vector;
    


	/**
	 * Activity constructor. 
	 * Receives an intent from SetupExperiment.
	 * 
	 * @param savedInstanceState
	 *            saved data from a previous run of this Activity
	 * 
	 *            {@inheritDoc}
	 * */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i("Breakout", "onCreate()");
		
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		Intent intent = getIntent();

		// Currently NOT set by SetupExperiment
		// but required for GameView object (using default values)
		int newGame = intent.getIntExtra("NEW_GAME", 1);
		sound = intent.getBooleanExtra("SOUND_ON_OFF", true);
		seq_num = intent.getIntExtra("SEQ_NUM", 0);

		// intent from SetupExperiment
		username = intent.getStringExtra(this.getString(R.string.USERNAME));
		mode = intent.getIntExtra(this.getString(R.string.EXPMODE),0);
		
		// Verify key params are recieved 
		Log.d(this.getString(R.string.TAG), "\t ...Received preferences");
		Log.d(this.getString(R.string.TAG), "\t\t username: " + username);
		
		// init sensor handlers
		setupSensors();
		
		// initialize graphics and game thread
		gameView = new GameView(this, newGame, sound, seq_num, username, mode);
		Log.i("Breakout","prior to setContent(gameView)");
		setContentView(gameView);
		Log.i("Breakout","after setContent");
	}
	/*
	public void onCreate(Bundle savedInstanceState) {
		Log.i("Breakout", "onCreate()");
		
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		Intent intent = getIntent();

		// Currently NOT set by SetupExperiment
		// but required for GameView object (using default values)
		int newGame = intent.getIntExtra("NEW_GAME", 1);
		sound = intent.getBooleanExtra("SOUND_ON_OFF", true);
		level = intent.getIntExtra("GAME_LEVEL", 1);

		// intent from SetupExperiment
		username = intent.getStringExtra(this.getString(R.string.USERNAME));
		latency = Integer.parseInt(intent.getStringExtra(this
				.getString(R.string.LATENCY_IN_MS)));
		
		// Verify key params are recieved 
		Log.d(this.getString(R.string.TAG), "\t ...Received preferences");
		Log.d(this.getString(R.string.TAG), "\t\t username: " + username);
		Log.d(this.getString(R.string.TAG), "\t\t latency: " + latency);
		
		// init sensor handlers
		setupSensors();
		
		// initialize graphics and game thread
		gameView = new GameView(this, newGame, sound, level, username, latency);
		Log.i("Breakout","prior to setContent(gameView)");
		setContentView(gameView);
		Log.i("Breakout","after setContent");
	}*/
	
	/**
	 * Called when the system resumes this Activity.
	 * Sets up writer and starts sensor listener
	 * 
	 * {@inheritDoc}
	 * */
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.i("Breakout", "onResume()");
    	startSensors();
		gameView.resume();
	}

	/**
	 * Called when the system pauses this Activity. 
	 * (when another activity comes into the foreground)
	 * 
	 * Stops the game's thread from running.
	 * Stops the hardware sensors from running.
	 * 
	 * @see http://developer.android.com/reference/android/app/Activity.html
	 * for activity Life-Cycle
	 * 
	 * {@inheritDoc}
	 * */
	@Override
	protected void onPause() {
		Log.i("Breakout", "onPause()");
		
		gameView.pause();
		stopSensors();
		
		super.onStop();
		finish();
	}
	
	/**
	 * Called when the Activity is no longer visible
	 * Writes experiment data and 
	 * 
	 * @see http://developer.android.com/reference/android/app/Activity.html
	 * for activity Life-Cycle
	 * 
	 * {@inheritDoc}
	 * */
    @Override
    protected void onStop() {
    	Log.i("Breakout", "onStop(): Writing Experiment Data");		
    	super.onStop();
    	
    	// this activity is finished, return to the SetupExperiment
    	finish();
    }
	
	
	/**
	 * Wrapper function for passing KeyEvent to gameView
	 * Workaround since SurfaceView not receiving view events 
	 * 
	 * @Override
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		// let gameView use left and right dpad for game control
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
			return gameView.onKeyDown(keyCode, event);
		}
		
		// let system handle all other key events
		return super.onKeyDown(keyCode, event);
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Wrapper function for passing SensorEvent to gameView
	 * Workaround since SurfaceView not receiving key event 
	 * 
	 * @Override
	 */
	public void onSensorChanged(SensorEvent e) {
			if ((e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
					|| (e.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) ) {
				gameView.onAccelerationChanged(e.values[0], e.values[1], e.values[2]);
            }
	}
	
	/**
	 * Initializes handles for hardware sensors
	 * 
	 * @see http://developer.android.com/reference/android/hardware/SensorManager.html
	 * for details on using the SensorManager class
	 * 
	 * @see http://developer.android.com/reference/android/hardware/SensorEvent.html
	 * for details on each sensor types
	 */
	private void setupSensors() {
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);    
        
    	// Verify Sensor Access: Display all available sensors
        List<Sensor> sensor_list = sm.getSensorList(Sensor.TYPE_ALL); 
        Log.i("setupSensors()", "Sensors Available" + ": " + sensor_list.size());
        for(int i=0; i<sensor_list.size(); i++)
        	Log.i("setupSensors", "Sensor (" + i + "): " +  sensor_list.get(i).getName());
        
	}	
	
    /**
     * sensor registration
     */
    private void startSensors() {
      sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }
    
    /**
     * unregisters all sensors 
     */
    private void stopSensors() {
    	sm.unregisterListener(this);
    }

}

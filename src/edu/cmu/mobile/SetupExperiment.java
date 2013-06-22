package edu.cmu.mobile;



import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.dhbikoff.breakout.Breakout;
import com.dhbikoff.breakout.R;

/**
 * the SetupExperiment activity executes 
 * before the main application activity
 * 
 * it displays a dialog prompting the investigator
 * to provide a description of the test run. 
 * 
 * The description is used for setting up experiment
 * parameters and annotating the experiment log
 * 
 * @author Emmanuel Owusu
 *
 */
public class SetupExperiment extends Activity {
	
	// handles for view elements
	EditText input_username;
	//EditText input_latency;
	RadioGroup ModeGroup;
	RadioButton freeMode_button;
	RadioButton speedMode_button;
	RadioButton sizeMode_button;
	Button startButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(this.getString(R.string.TAG), "SetupExperiment.onCreate");
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.setup_modes);
		setup();	
	}
	
	/**
	 * Called when this Activity is resumed. 
	 * {@inheritDoc}
	 * */
	@Override
	protected void onResume() {
		Log.i(this.getString(R.string.TAG), "SetupExperiment.onResume");
		super.onResume();
		restoreState();
	}
	
	/**
	 * Called when the system pauses this Activity. Saves the sound button state
	 * and high score value.
	 * {@inheritDoc}
	 * */
	@Override
	protected void onPause() {
		Log.i(this.getString(R.string.TAG), "SetupExperiment.onPause");
		super.onPause();
		saveState(); 
	}
	
    /**
     * initialize setup screen
     */
    private void setup() {
       
        // link handles to view elements
		input_username = (EditText)findViewById(R.id.username_textview);     
		//input_latency = (EditText)findViewById(R.id.latency_textview);
        startButton = (Button)findViewById(R.id.start_button);

        ModeGroup = (RadioGroup)findViewById(R.id.expModeradioGroup);

        // Define behavior of the "Start New Game" button
        startButton.setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v) {
				
				// Save entered data
				saveState();
				
				// Pass user input to main activity
				Intent intent = createIntent();
				startActivity(intent);
			}
    	});
    }
    
    /**
     * Saves user inputs from UI as a shared preferences
     */
    private void saveState() {
	    Log.i("SetupExperiment", "saveState()");
	    
		// Get inputs from UI
		String username = input_username.getText().toString().replace(' ', '_');
		//String latency = input_latency.getText().toString();
		int mode = getMode();
		
		// Save Preferences
		SharedPreferences settings = getSharedPreferences(this.getString(R.string.PREFS), 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString(this.getString(R.string.USERNAME), username);
	    editor.putInt(this.getString(R.string.EXPMODE), mode);
	    //editor.putString(this.getString(R.string.LATENCY_IN_MS), latency);
	    editor.commit();
	    

		Log.i(this.getString(R.string.TAG), "\t\t username: " + username);
		Log.i(this.getString(R.string.TAG), "\t\t mode: " + mode);
    
    }
    
    /**
     *  Restores UI with user inputs from previous session
     */
    private void restoreState() {
    	Log.i(this.getString(R.string.TAG), "\t ...Restoring preferences");
    	
    	// Restore Preferences (if a saved instance is available)
        SharedPreferences settings = getSharedPreferences(this.getString(R.string.PREFS), 0);
        String username_previous = settings.getString(this.getString(R.string.USERNAME), "");
//        String latency_previous = settings.getString(this.getString(R.string.LATENCY_IN_MS), "0");
    	
        input_username.setText(username_previous); // restore previously used username
  //      input_latency.setText(latency_previous); // restore previously used latency value
        
        Log.i(this.getString(R.string.TAG), "\t\t username: " + username_previous);
	//	Log.i(this.getString(R.string.TAG), "\t\t latency: " + latency_previous);
        
    }
    
    /**
     * creates an intent for passing to main activity 
     * 
     * @param username: string containing a user ID
     * @param latency: string containing the latency value
     * @return intent: bundle containing preferences to used in main activity
     */
    private Intent createIntent() {		
		// Get inputs from UI
		String username = input_username.getText().toString().replace(' ', '_');
		//String latency = input_latency.getText().toString();
		int mode = getMode();
    	
    	Intent intent = new Intent(this, Breakout.class);
		intent.putExtra(this.getString(R.string.USERNAME), username); 
		intent.putExtra(this.getString(R.string.EXPMODE), mode);
		//intent.putExtra(this.getString(R.string.LATENCY_IN_MS), latency); 
		return intent;
    }

    private int getMode(){
    	int mode = 0;
		switch(ModeGroup.getCheckedRadioButtonId()){
		case R.id.freeRadioBtn:
			mode = 0;
			break;
		case R.id.speedRadioBtn:
			mode = 1;
			break;
		case R.id.sizeRadioBtn:
			mode = 2;
			break;
		}
		return mode;
    }
}

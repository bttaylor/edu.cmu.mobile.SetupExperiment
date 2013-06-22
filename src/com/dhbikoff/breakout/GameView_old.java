package com.dhbikoff.breakout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import edu.cmu.mobile.ExperimentData;
import edu.cmu.mobile.SetupExperiment;
import android.app.AlertDialog;

/**
 * Creates and draws the graphics for the game. Runs a Thread for animation and
 * game physics. Saves and restores game data when paused or restored.
 * 
 */
public class GameView_old extends SurfaceView implements Runnable{

	private boolean showGameOverBanner = false;
	private int playerLevel = 0;
	//private int PLAYER_TURNS_NUM = 1;
	private Paint levelPaint;
	private String playerLevelText = "LEVEL = ";
	private boolean soundToggle;
	private int startNewGame;
	private ObjectOutputStream oos;
	private final String FILE_PATH = "data/data/com.dhbikoff.breakout/data.dat";
	private final int frameRate = 33;
	private final int startTimer = 66;
	private boolean touched = false;
	private float eventX;
	private SurfaceHolder holder;
	private Thread gameThread = null;
	private boolean running = false;
	private Canvas canvas;
	private boolean checkSize = true;
	private boolean newGame = true;
	private int waitCount = 0;
	private Ball ball;
	private Paddle paddle;
	private ArrayList<Block> blocksList;
	private String getReady = "GET READY...";
	private Paint getReadyPaint;
	private int points = 0;
	private Paint scorePaint;
	private String score = "SCORE = ";
	//private int gameLevel = 0;
	private int totalErrors = 0; 
	private Paint errorPaint;
	//private CountDownTimer countDown;
	private boolean timeUp = false;
	//private Handler m_handler;
	private boolean timerActive = false;         
	private Timer GameTimer;
	
	private float dpadStep; // how much to move paddle for onKey event
	private int screenWidth; // save screen width in pixels
	

	private int MAX_LEVEL = 2; // controls how many levels per experiment
	private int timerDuration = 12000; //120000; // length of each level in mili-seconds
	private long gameStartTime;
	private String username;
	private int latency;

    
    // Storing experiment data
	protected LinkedList<ExperimentData> experimentData;


	/**
	 * Constructor. Sets sound state and new game signal depending on the
	 * incoming intent from the Breakout class. Instantiates the ball, blocks,
	 * and paddle. Sets up the Paint parameters for drawing text to the screen.
	 * 
	 * @param context
	 *            Android Context
	 * @param launchNewGame
	 *            start new game or load save game
	 * @param sound
	 *            sound on/off
	 * */
	public GameView_old(Context context, int launchNewGame, boolean sound, int level, 
			String username, int latency) {
		super(context);
		startNewGame = launchNewGame; // new game or continue
		soundToggle = sound;
		holder = getHolder();
		ball = new Ball(this.getContext(), soundToggle);
		paddle = new Paddle();
		blocksList = new ArrayList<Block>();
		//gameLevel = level;
		
		scorePaint = new Paint();
		scorePaint.setColor(Color.WHITE);
		scorePaint.setTextSize(25);

		errorPaint = new Paint();
		errorPaint.setColor(Color.WHITE);
		errorPaint.setTextSize(25);

		levelPaint = new Paint();
		levelPaint.setTextAlign(Paint.Align.RIGHT);
		levelPaint.setColor(Color.WHITE);
		levelPaint.setTextSize(25);

		getReadyPaint = new Paint();
		getReadyPaint.setTextAlign(Paint.Align.CENTER);
		getReadyPaint.setColor(Color.WHITE);
		getReadyPaint.setTextSize(45);
		
		GameTimer = new Timer();
		
		experimentData= new LinkedList<ExperimentData>();
		
		delayBuffer = new LinkedList<Float>();

		this.latency = latency;
		
		delaybufferSize	= (int)(latency/ACC_FREQUENCY)/1000+1;
		Log.i("GameView", "Buffersize: " + delaybufferSize);
				
		// Format file name
		Date now = new Date();
		this.username = username;
		filename =  username + "_" + "ms_" + now.getTime() +".csv";
	}

	/**
	 * Runs the game thread. Sets the frame rate for drawing graphics. Acquires
	 * a canvas for drawing. If no blocks exist, initialize game objects. Moves
	 * the paddle according to touch events. Checks for collisions as the ball's
	 * moves. Keeps track of player turns and ends the game when turns run out.
	 * Awards the player an extra turn when a level is completed. Draws text to
	 * the screen showing player score and turns remaining. Draws text to
	 * announce when the game begins or ends.
	 * 
	 * */
	public void run() {
		
		while (running) {
			try {
				Thread.sleep(frameRate);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (holder.getSurface().isValid()) {
				canvas = holder.lockCanvas();
				canvas.drawColor(Color.BLACK);

				if ((blocksList.size() == 0) || timeUp) {
			
			//		if (!timeUp && timerActive) GameTimer.cancel();
			
					timeUp = false;
					checkSize = true;
					newGame = true;
				}

				if (checkSize) {
					initObjects(canvas);
					checkSize = false;
				}

		
				if (touched) {
					paddle.movePaddle((int) eventX);
				}
				drawToCanvas(canvas);

				// pause screen on new game
				if (newGame) {
					waitCount = 0;
					newGame = false;
				}
				waitCount++;
				
				if(running){
				engine(canvas, waitCount);
				String printScore = score + points;
				canvas.drawText(printScore, 0, 25, scorePaint);
				String levels = playerLevelText + playerLevel;
				canvas.drawText(levels, canvas.getWidth(), 25, levelPaint);
				holder.unlockCanvasAndPost(canvas); // release canvas
				}
				
			}
		} // end while(running)
		Log.i("GameView","running = false");
		Intent intent = new Intent(this.getContext(), SetupExperiment.class);
		this.getContext().startActivity(intent);
		return;
	}

	/**
	 * Draws graphics to the screen.
	 * 
	 * @param canvas
	 *            graphics canvas
	 * */
	private void drawToCanvas(Canvas canvas) {
		drawBlocks(canvas);
		paddle.drawPaddle(canvas);
		ball.drawBall(canvas);
	}

	/**
	 * Pauses the animation until the wait counter is satisfied. Sets the
	 * velocity and coordinates of the ball. Checks for collisions. Checks if
	 * the game is over. Draws text to alert the user if the ball restarts or
	 * the game is over.
	 * 
	 * @param canvas
	 *            graphics canvas
	 * @param waitCt
	 *            number of frames to pause the game
	 * */
	private void engine(Canvas canvas, int waitCt) {
		if (waitCount > startTimer) {
			showGameOverBanner = false;
			//playerLevel -= ball.setVelocity();
			 int curError = ball.setVelocity();
			 
			// TODO store previous rounds data; set here? or in initObjects???
			 if (curError == 1) {
				 ExperimentData er = new ExperimentData(playerLevel, totalErrors, blocksList.size(), points, (System.currentTimeMillis()- gameStartTime));
				 experimentData.add(er);
				 totalErrors += curError;
			 }
			
//			if (playerLevel > MAX_LEVEL) {
//				showGameOverBanner = true;
//				gameOver(canvas);
//				GameTimer.cancel();
//				
//				getReadyPaint.setColor(Color.WHITE);
//				canvas.drawText("Complete", canvas.getWidth() / 2,
//						(canvas.getHeight() / 2) - (ball.getBounds().height())
//								- 50, getReadyPaint);
//				
//				if (showGameOverBanner) {
//					try {
//						Thread.sleep(3000);
//						
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//				
//				return;
//			}
			
			// paddle collision
			ball.checkPaddleCollision(paddle);
			// block collision and points tally
			points += ball.checkBlocksCollision(blocksList);
		}

		else {
			if (showGameOverBanner) {
				getReadyPaint.setColor(Color.RED);
				canvas.drawText("GAME OVER!!!", canvas.getWidth() / 2,
						(canvas.getHeight() / 2) - (ball.getBounds().height())
								- 50, getReadyPaint);
			}
			getReadyPaint.setColor(Color.WHITE);
			canvas.drawText(getReady, canvas.getWidth() / 2,
					(canvas.getHeight() / 2) - (ball.getBounds().height()),
					getReadyPaint);

		}
	}

	/**
	 * Resets variables to signal a new game. Deletes the remaining blocks list.
	 * When the run function sees the blocks list is empty, it will initialize a
	 * new game board.
	 * 
	 * @param canvas
	 * 
	 *            graphics canvas
	 * */
	private void gameOver(Canvas canvas) {
		 Log.i("GameView.gameOver()", "GAME OVER");
		
		points = 0;
		blocksList.clear();
		
		// Save all game data        
        setupWriteToSDCard();
    	writeStatsToSDCard();
		stopWriteToSDCard();

	}

	/**
	 * Initializes graphical objects. Restores game state if an existing game is
	 * continued.
	 * 
	 * @param canvas
	 *            graphical canvas
	 * */
	private void initObjects(Canvas canvas) {

		if (playerLevel > 0) {
			
			// TODO store previous rounds data
			ExperimentData er = new ExperimentData(playerLevel, totalErrors, blocksList.size(), points, 0);
			experimentData.add(er);		
			
			// TODO Verify that levels are running for the correct duration of time
			long duration = System.currentTimeMillis() - gameStartTime;
			Log.i("Breakout", "GameView.initObjects()" + "Level " + playerLevel + " duration: " + duration/1000 +"(secs)");
		}
		
		
		playerLevel++; // update player level
		
		// store the start time of current level
		gameStartTime = System.currentTimeMillis();
		
		// reset paddle and ball location; change ball speed based on the level
		touched = false;
		ball.initCoords(canvas.getWidth(), canvas.getHeight(), playerLevel);
		paddle.initCoords(canvas.getWidth(), canvas.getHeight());
		
		initBlocks(canvas);
		
			
		// Complete experiment if last level reached
        // TODO need to terminate this thread and return to Breakout activity; still hangs
		if (playerLevel > MAX_LEVEL) {
			showGameOverBanner = true;
			
			//GameTimer.cancel();
			
			getReadyPaint.setColor(Color.WHITE);
			canvas.drawText("Complete", canvas.getWidth() / 2,
					(canvas.getHeight() / 2) - (ball.getBounds().height())
							- 50, getReadyPaint);
			holder.unlockCanvasAndPost(canvas); // release canvas
			
			gameOver(canvas);
			
			// Experiment is over; close this thread
			try {
				Log.i("GameOver", "Go to sleep");
				Thread.sleep(2000);
				Log.i("GameOver", "Awake");
				running = false;
		//		pause();
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else { // set a new timer
			
			timerActive = true;
			GameTimer.schedule(new TimerTask() {
			     public void run() {
			    	 Log.v("GameView.initObjects()", "Level " + playerLevel + " TIME UP (" + timerDuration/1000 +"secs)");
			         timeUp = true;
			     }
			}, timerDuration);		
		}
	}
	
	/**
	 * Restores a saved ArrayList of blocks. Reads through an ArrayList of
	 * integer Arrays. Passes the values to construct a block and adds the block
	 * to an ArrayList.
	 * 
	 * @param arr
	 *            ArrayList of integer arrays containing the coordinates and
	 *            color of the saved blocks.
	 * */
	private void restoreBlocks(ArrayList<int[]> arr) {
		for (int i = 0; i < arr.size(); i++) {
			Rect r = new Rect();
			int[] blockNums = arr.get(i);
			r.set(blockNums[0], blockNums[1], blockNums[2], blockNums[3]);
			Block b = new Block(r, blockNums[4]);
			blocksList.add(b);
		}
	}

	/**
	 * Opens a saved game file and reads in data to restore saved game state.
	 * */
	private void restoreGameData() {
		try {
			FileInputStream fis = new FileInputStream(FILE_PATH);
			ObjectInputStream ois = new ObjectInputStream(fis);
			points = ois.readInt(); // restore player points
			playerLevel = ois.readInt(); // restore player turns
			@SuppressWarnings("unchecked")
			ArrayList<int[]> arr = (ArrayList<int[]>) ois.readObject();
			restoreBlocks(arr); // restore blocks
			ois.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		startNewGame = 1; // only restore once
	}

	/**
	 * Initializes blocks. Measures the width and height of the canvas for the
	 * dimensions and coordinates of the blocks. Sets the color depending on the
	 * block's row. Adds the block to an ArrayList.
	 * 
	 * @param graphics canvas
	 *             
	 * */
	private void initBlocks(Canvas canvas) {
		int blockHeight = canvas.getWidth() / 36;
		int spacing = canvas.getWidth() / 144;
		int topOffset = canvas.getHeight() / 10;
		int blockWidth = (canvas.getWidth() / 10) - spacing;
		
		int num_rows = 4;
		
		// Reset state variables needed for control
		screenWidth = canvas.getWidth();
		dpadStep = (float) screenWidth/10;
		eventX = (float) screenWidth/2;
		currentVelocity = 0;
		
		
		for (int i = 0; i < num_rows; i++) {
			for (int j = 0; j < 10; j++) {
				int y_coordinate = (i * (blockHeight + spacing)) + topOffset;
				int x_coordinate = j * (blockWidth + spacing);

				Rect r = new Rect();
				r.set(x_coordinate, y_coordinate, x_coordinate + blockWidth,
						y_coordinate + blockHeight);

				int color;

				if (i < 2)
					color = Color.RED;
				else if (i < 4)
					color = Color.YELLOW;
				else if (i < 6)
					color = Color.GREEN;
				else if (i < 8)
					color = Color.MAGENTA;
				else
					color = Color.LTGRAY;

				Block block = new Block(r, color);

				blocksList.add(block);
			}
		}
	}

	/**
	 * Draws blocks to screen
	 * 
	 * @param canvas
	 *            graphical canvas
	 * */
	private void drawBlocks(Canvas canvas) {
		for (int i = 0; i < blocksList.size(); i++) {
			blocksList.get(i).drawBlock(canvas);
		}
	}

	/**
	 * Saves game state. Reads block color and coordinates into an ArrayList.
	 * Saves blocks, player points, and player turns into a data file.
	 * */
	private void saveGameData() {
		ArrayList<int[]> arr = new ArrayList<int[]>();

		for (int i = 0; i < blocksList.size(); i++) {
			arr.add(blocksList.get(i).toIntArray());
		}

		try {
			FileOutputStream fos = new FileOutputStream(FILE_PATH);
			oos = new ObjectOutputStream(fos);
			oos.writeInt(points);
			//oos.writeInt(playerLevel);
			oos.writeObject(arr);
			oos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves game data and destroys Thread.
	 * */
	public void pause() {
		Log.i("Breakout", "Gameview pause()");
		
		//saveGameData();
		running = false;
		while (true) {
			if(gameThread.equals(null))
				Log.i("Breakout", "sitting in a loop b/c gameThread is null");
		/*	try {
			//	gameThread.join();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}*/
			break;
		}
		//gameThread = null;
		ball.close();
		Log.i("Breakout", "exiting gameView.pause(), running = false now");
	}

	/**
	 * Resumes the game. Starts a new game Thread.
	 * */
	public void resume() {
		Log.i("GameView","gameThread.start calling");
		running = true;
		gameThread = new Thread(this);
		gameThread.start();
	}

	/**
	 * Overridden Touch event listener. Reads screen touches to move the paddle.
	 * {@inheritDoc}
	 * 
	 * @param event screen touch event
	 * @return true
	 * */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN
				|| event.getAction() == MotionEvent.ACTION_MOVE) {
			eventX = event.getX();
			//Log.i("onTouchEvent", "eventX: " + eventX);
			touched = true;
			
			try {
				Thread.sleep(latency);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return touched;
	}

	/**
	 * Reads directional-pad to move the paddle
	 * @see android.view.View.OnKeyListener
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT: {
				Log.v("onKeyDown","move left");
								
				// Move paddle left
				eventX = move(0.0f-dpadStep);								
				touched = true;
				return touched;
			}
			case KeyEvent.KEYCODE_DPAD_RIGHT: {
				Log.v("onKeyDown","move right");
				
				// Move paddle right
				eventX = move(dpadStep);				
				touched = true;
				return touched;
			}

		}
		
		return false;
	}
	
	/**
	 * 
	 * @param dist
	 * @return next position of eventX
	 */
	private float move(float dist) {
		Log.d("move", "dist: " + dist);
		
		int newPosition = (int) Math.round(eventX+dist);
		Log.d("move","newPosition (before): " + newPosition);
		
		if (newPosition > screenWidth) {
			newPosition = screenWidth;
		}

		if (newPosition < 0) {
			newPosition = 0;
		}
		
		Log.d("move","newPosition (after): " + newPosition);
		return (float)newPosition;	
	}
	
	/**
	 * Convert the magnitude of accelerations 
	 * along the z-axis (left, right motion) 
	 * to corresponding number of pixels to move paddle
	 * 
	 * With respect to a person wearing the goggles:
	 * x-axis: up(+), 		down(-)
	 * y-axis: forward(+), 	backward(-)
	 * z-axis: right(+), 	left(-) 
	 * 
	 * Note:  All values are in SI units (m/s^2) and measure the acceleration 
	 * applied to the phone minus the force of gravity. For more details...
	 * 
	 * All movement detection algorithms assume linear acc; device acc 
	 * Note: Linear Acceleration = Acceleration - Gravity
	 * 
	 * @see http://developer.android.com/reference/android/hardware/SensorEvent.html
	 * 
	 */
	public void onAccelerationChanged(float x, float y, float z) {
		trackVelocity(x,y,z);
	}
	
	
	// Helper Params for trackVelocity()
	private float currentVelocity;
	private final float ACC_FREQUENCY = 1.0f/44.0f;
	private final float scale_velocity_to_pixel = 7.0f;


	private final int z_window_size = 6;
	private final float z_threshold =  -0.1f;
	private LinkedList<Float> z_window = new LinkedList<Float>();
	private float z_sum = 0.0f;
	
	LinkedList<Float> delayBuffer;
	int delaybufferSize;

	private void trackVelocity(float x, float y, float z){
		
		currentVelocity =  currentVelocity + z*ACC_FREQUENCY;
		Log.d("trackVelocity", "z: " + z);
		Log.d("trackVelocity", "ACC_FREQUENCY: " + ACC_FREQUENCY);
		Log.d("trackVelocity", "currentVelocity: " + currentVelocity);
		Log.d("trackVelocity", "scale_velocity_to_pixel: " + scale_velocity_to_pixel);
		
		// buffer movements
		delayBuffer.add((float)currentVelocity*scale_velocity_to_pixel);
		
		// Replay buffered move		
		if(delayBuffer.size() == delaybufferSize) {
			touched = true;
			eventX = move(delayBuffer.remove());
		}
		
		

		// save a sliding-window of observed accelerations
		z_window.add(z);
		z_sum+=z;
		if(z_window.size() == z_window_size) {
			z_sum-=z_window.remove();
		}
		
		// reset currentVelocity to zero; if device has remained relatively still
		// during the current window of observations
		float z_average = (z_sum/z_window.size());
		if( z_average > z_threshold && z_average < Math.abs(z_threshold)) {
			Log.d("trackVelocity", "RESET Velocity");
			currentVelocity = 0.0f;
		}	
	}
	
	/****************WRITER FUNCTIONS**********************************/
	
	// external storage references
	private File folder;
	private File file;
	private FileWriter writer;
	protected static BufferedWriter out;
	private String filename;
	
	// manage file lines before write
	protected static List<String> storeLines;
	protected static StringBuilder keySequence;
	protected static StringBuilder timestamps;
	
    /**
     * Setup directory and file for writing
     */
    private void setupWriteToSDCard() {
    	try {
    		folder = new File(Environment.getExternalStorageDirectory(), 
    				getResources().getString(R.string.APP_DIR));
    		
    		Log.i("setupWriteToSDCard()", "Storage directory: " + folder);
    		
    		if(!folder.exists()) folder.mkdirs();
    	    if(folder.canWrite()) {
    	    	file = new File(folder, filename);
    	    	writer = new FileWriter(file);
    	    	out = new BufferedWriter(writer);
    	    } else throw new IOException();
    	} catch (IOException e) {
    	    Log.e("setupWriteToSDCard()", getResources().getString(R.string.NO_EXT_STO_ACCESS) + ": " + e.getMessage());
        	//new AlertDialog.Builder(this)
        	//.setMessage(getResources().getString(R.string.NO_EXT_STO_ACCESS))
        	//.show();
    	}
    }
	
    /**
     * Write experiment statistics to file
     */
    private void writeStatsToSDCard() {
		try {
			Log.i("Breakout", "writeStatsToSDCard : Writing Experiment Statistics to File...");
			
	        Iterator<ExperimentData> iterator = experimentData.iterator();  
	        while (iterator.hasNext()) {  
	            //Log.i("experimentData", iterator.next().toString());
	            out.write(iterator.next().toString());
	            out.newLine();
	        }
			
		} catch (IOException e) {
			Log.e("writeStatsToSDCard()", e.getMessage());
		}
    	
    }
    
    /**
     * close buffered file writer 
     */
    private void stopWriteToSDCard() {
    	try {
			out.close();
		} catch (IOException e) {
			Log.e("stopWriteToSDCard()", e.getMessage());
		}
    	
    }
	
}

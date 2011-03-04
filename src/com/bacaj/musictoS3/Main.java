package com.bacaj.musictoS3;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * @author <a href="http://www.jamesbaca.net">James Baca</a>
 */

public class Main extends Activity implements OnCompletionListener{

	private Handler mHandler = new Handler();
	private Spinner mBucketSpinner;
	private Button mPlayButton;
	private Button mRecordButton;
	private Button mStopButton;
	private Button mPushButton;
	private boolean mCredentialsFound;
	private final RecordMusic mRecorder = new RecordMusic();
	private final PlayMusic mPlayer = new PlayMusic();
	private final String mDirectory = "/MusicToS3/";
	private List<String> mBuckets = null;
	private ProgressDialog mLoadingDialog;



	/**
	 * Runnable to be called from a different thread without running into ui errors
	 * http://android-developers.blogspot.com/2009/05/painless-threading.html
	 */
	private final Runnable mResultsRunnable = new Runnable() {
		@Override
		public void run(){
			setupUI();
		}
	};

	/**
	 * Used for monitoring for a filename in the EditText field
	 */
	private final TextWatcher mFileNameWatcher = new TextWatcher() {

		@Override
        public void afterTextChanged (Editable s){

                if(s.length() != 0){
                	// We have a name for a file

                	String lName = s.toString(); // filename
                	String lPath = Environment.getExternalStorageDirectory()
    				.getAbsolutePath(); // absolutePath to external storage


                	RecordMusic.createDirectoryIfNotExist(lPath + mDirectory);

                	// add a 3gp extension if the user did not
                	if (!lName.contains(".3gp"))
                		lName += ".3gp";

                	// Create a file object for the Player/Recorder objects
                	File lFile = new File(lPath +  mDirectory  + lName);


                	mRecorder.setPath(mDirectory, lName);
					mPlayer.setPath(mDirectory, lName);
					Main.this.mRecordButton.setEnabled(true);


                	if(lFile.isFile() && lFile.canRead()){
                		// this file exists we can play it

                		Main.this.mPlayButton.setEnabled(true);
                		Main.this.mPushButton.setEnabled(true);
                	}
                }
        }

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub

		}

	};

	// The onClickListeners are kind of like finite state machines for the ui
	/**
	 *  OnClickListener for the record button
	 */
	private final OnClickListener mRecordListener = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			Main.this.mStopButton.setEnabled(true);
			Main.this.mPlayButton.setEnabled(false);
			Main.this.mPushButton.setEnabled(false);
			Main.this.mRecorder.startRecording();
		}

	};

	/**
	 * The OnClickListener for the play button
	 */
	private final OnClickListener mPlayListener = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			Main.this.mStopButton.setEnabled(true);
			Main.this.mRecordButton.setEnabled(false);
			Main.this.mPushButton.setEnabled(false);

			// we set a callback to update the buttons when the song is over
			Main.this.mPlayer.setOnCompletetionListener(Main.this);
			Main.this.mPlayer.startPlaying();
		}

	};

	/**
	 * The OnClickListener for the stop button
	 */
	private final OnClickListener mStopListener = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			Main.this.mRecordButton.setEnabled(true);
			Main.this.mPlayButton.setEnabled(true);
			Main.this.mPushButton.setEnabled(true);

			// Just stop both since only one is valid at a time and
			// calling stop on the wrong one is harmless
			Main.this.mRecorder.stopRecording();
			Main.this.mPlayer.stopPlaying();
		}

	};

	/**
	 * The OnClickListener for the push to S3 button happens
	 */
	private final OnClickListener mPushListener = new OnClickListener(){

		@Override
		public void onClick(View arg0) {

			Main.this.mRecordButton.setEnabled(false);
			Main.this.mPlayButton.setEnabled(false);
			Main.this.mStopButton.setEnabled(false);
			Main.this.mPushButton.setEnabled(false);

			// get the filename and path
			String lName = Main.this.mRecorder.getAbsolutePath();
			File lFile = new File(lName);

			String lBucket = Main.this.mBucketSpinner.getSelectedItem().toString();

			if(lFile.isFile() && lFile.canRead()){
				// The file exists and we have read perms to it

				// push it to the cloud
				S3.createObjectForBucket(lBucket, lFile.getName(), lFile);
			}
			Main.this.mRecordButton.setEnabled(true);
			Main.this.mPlayButton.setEnabled(true);
			Main.this.mPushButton.setEnabled(true);
		}

	};


    /** Called when the activity is first created. */
	private EditText mFileName;    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLoadingDialog = ProgressDialog.show(this, "",
        		this.getString(R.string.loading_message), true);

        // load the S3 credentials
        loadCredentials();
    }

	/**
	 * Attempt to load the credentials
	 */
    private void loadCredentials() {
    	Thread lThread = new Thread() {

			@Override
    		public void run(){
    	        try {
    	        	InputStream lInStream = getClass().getResourceAsStream( "AwsCredentials.properties" );
    	        	if( S3.loadCredentials(lInStream)){
    	        		mCredentialsFound = true;
    	        		loadBuckets();
    	        	} else{
    	        		mCredentialsFound = false;
    	        	}

    	        }
    	        catch ( Exception exception ) {
    	            Log.e( "Loading AWS Credentials", exception.getMessage() );
    	            mCredentialsFound = false;
    	        }
    	        Main.this.mHandler.post(mResultsRunnable);
    		}
    	};
    	lThread.start();
    }

    /**
     * Display alert dialog about credentials problem and exit after
     */
    protected void displayCredentialsIssueAndExit() {
        AlertDialog.Builder confirm = new AlertDialog.Builder( this );
        confirm.setTitle(R.string.loading_error);
        confirm.setMessage(R.string.credential_error_message);
        confirm.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick( DialogInterface dialog, int which ) {
                	Main.this.finish();
                }
        } );
        confirm.show().show();
    }

    /**
     * Method setsUp the UI by making buttons visible
     * and sets up OnClick Events
     */
    private void setupUI(){
    	mLoadingDialog.dismiss();

    	if(mCredentialsFound == false){
    		displayCredentialsIssueAndExit();
    		return;
    	}
    	// Credentials were valid display the rest of the items

    	mFileName = (EditText) findViewById(R.id.file_name);
    	mFileName.setVisibility(View.VISIBLE);
    	mFileName.addTextChangedListener(mFileNameWatcher);

    	mPlayButton = (Button) findViewById(R.id.play_button);
    	mPlayButton.setVisibility(View.VISIBLE);
    	mPlayButton.setOnClickListener(mPlayListener);

    	mRecordButton = (Button) findViewById(R.id.record_button);
    	mRecordButton.setVisibility(View.VISIBLE);
    	mRecordButton.setOnClickListener(mRecordListener);


    	mStopButton = (Button) findViewById(R.id.stop_button);
    	mStopButton.setVisibility(View.VISIBLE);
    	mStopButton.setOnClickListener(mStopListener);


    	mPushButton = (Button) findViewById(R.id.push_button);
    	mPushButton.setVisibility(View.VISIBLE);
    	mPushButton.setOnClickListener(mPushListener);


    	mBucketSpinner = (Spinner) findViewById(R.id.bucket_spinner);
    	mBucketSpinner.setVisibility(View.VISIBLE);
    	ArrayAdapter <String>lAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mBuckets);
    	mBucketSpinner.setAdapter(lAdapter);
    }

    /**
     * Method gets a list of the buckets for the account listed
     */
    private void loadBuckets(){
    	mBuckets = S3.getBucketNames();
    }

	@Override
	public void onCompletion(MediaPlayer arg0) {
		Main.this.mStopListener.onClick(null);
	}
}
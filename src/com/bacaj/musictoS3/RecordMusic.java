package com.bacaj.musictoS3;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

/**
 * @author <a href="http://www.jamesbaca.net">James Baca</a>
 */

public class RecordMusic {
	private MediaRecorder mRecorder;
	private String mPath;
	static final String LOG_TAG = "RECORDMUSIC";

	public RecordMusic(){

	}

	public RecordMusic(String aDirectoryName, String aName) {
		setPath(aDirectoryName, aName);
	}

	/**
	 * Stores what file to record to
	 * @param aDirectoryName the path of the directory to record
	 * @param aName the actual file name
	 */
	public void setPath(String aDirectoryName, String aName) {
		String lPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath();
		if (!aName.contains(".3gp")) {
			aName += ".3gp";
		}

		mPath = lPath + "/" + aDirectoryName + "/" + aName;
	}

	public void startRecording() {
		stopRecording();

		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(mPath);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}

		mRecorder.start();
	}

	public void stopRecording() {
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
	}

	public String getAbsolutePath() {
		return mPath;
	}

	public String getFileName() {
		File lFile = new File(mPath);
		return lFile.getName();
	}

	static public void createDirectoryIfNotExist(String aDirectory){
		File lDirectory = new File(aDirectory);
    	if(!lDirectory.exists()){
    		lDirectory.mkdir();
    	}
	}

}

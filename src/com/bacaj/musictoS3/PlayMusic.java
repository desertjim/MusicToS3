package com.bacaj.musictoS3;

import java.io.IOException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.util.Log;

/**
 * @author <a href="http://www.jamesbaca.net">James Baca</a>
 */

public class PlayMusic {

	private String mPath;
	private MediaPlayer mPlayer;
	static private String LOG_TAG = "PLAYMUSIC";
	private OnCompletionListener mOnCompletionListener;

	public PlayMusic(){

	}

	public PlayMusic(String aDirectoryName, String aName) {
		setPath(aDirectoryName, aName);

	}

	public void setPath(String aDirectoryName, String aName) {
		String lPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath();
		if (!aName.contains(".3gp")) {
			aName += ".3gp";
		}

		mPath = lPath + "/" + aDirectoryName + "/" + aName;
	}

	public void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
        	if(mOnCompletionListener != null){
                mPlayer.setOnCompletionListener(mOnCompletionListener);
        	}
            mPlayer.setDataSource(mPath);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    public void stopPlaying() {
    	if(mPlayer != null){
    		mPlayer.release();
    		mPlayer = null;
    	}
    }

    public void setOnCompletetionListener(OnCompletionListener aOnCompletionListener){
    	mOnCompletionListener = aOnCompletionListener;

    }

}

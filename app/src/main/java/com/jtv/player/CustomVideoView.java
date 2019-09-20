package com.jtv.player;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jtv.player.R;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Displays a video file. The VideoView class can load images from various
 * sources (such as resources or content providers), takes care of computing its
 * measurement from the video so that it can be used in any layout manager, and
 * provides various display options such as scaling and tinting.
 */

public class CustomVideoView extends SurfaceView implements MediaPlayerControl,OnVideoSizeChangedListener {

	private int videoWidth=0,videoHeight=0 ;
	private int screenWidth ,screenHeight ;
	private boolean isPauseState ;
	public OnAudioFocusChangeListener afChangeListener ;
	private AudioManager audioManager ;
	private LinearLayout splashScreen ;
	public Timer timer;
	private boolean isFirstPlayPause = false ; 
	public static final int FF_REW_JUMP_VALUE = 30000;
	public static final String TAG = "CustomVideoView";
	// settable by the client
	private Uri mUri;

	private VideoPlayerScreen vScreen;
	// All the stuff we need for playing and showing a video
	private SurfaceHolder mSurfaceHolder;
	public MediaPlayer mMediaPlayer = null;
	private boolean mIsPrepared;
	private int mVideoWidth;
	private int mVideoHeight;
	private OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private TextView logText;
	private ProgressBar mProgressBar;
	private ImageView playPause ;

	private LinearLayout topNavBar ;

	private int mCurrentBufferPercentage;
	private OnErrorListener mOnErrorListener;
	private boolean mStartWhenPrepared;
	private int mSeekWhenPrepared;

	private final Context mContext;
	private FileDescriptor mFd;

	private IPlayPauseListener mListener;
	private ISeekListener mSeekListener;
	private IBufferingListener mBufferingListener;
	private static long lastTimeSeeked;
	private int lastPressedKey = 0;
	private int lastSeekPos = 0;

	boolean wasVideoPaused = false;

	private Handler buffDialogHandler ;

	private int m_interval = 2000; // 5 seconds by default, can be changed later

	Runnable m_statusChecker = new Runnable() {
		@Override
		public void run() {
			CustomVideoView.this.getCurrentPosition();
			try {
				CustomVideoView.this.buffDialogHandler.postDelayed(
						CustomVideoView.this.m_statusChecker,
						CustomVideoView.this.m_interval);
			} catch (Throwable e) {
			}

		}
	};

	MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mp) {
			int videoWidth = mp.getVideoWidth();
		    int videoHeight = mp.getVideoHeight();
		    Log.i(TAG, "onPrepared ");
		    Log.i(TAG, "onPrepared videoWidth "+videoWidth);
		    Log.i(TAG, "onPrepared videoHeight "+videoHeight);

			CustomVideoView.this.mIsPrepared = true;
			if (CustomVideoView.this.mOnPreparedListener != null) {
				CustomVideoView.this.mOnPreparedListener
						.onPrepared(CustomVideoView.this.mMediaPlayer);
			}

			if (CustomVideoView.this.mStartWhenPrepared) {

			} else if (!CustomVideoView.this.isPlaying()
					&& ((CustomVideoView.this.mSeekWhenPrepared != 0) || (CustomVideoView.this
							.getCurrentPosition() > 0))) {

			}

		}
	};

	private final OnCompletionListener mCompletionListener = new OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {

			if (CustomVideoView.this.mOnCompletionListener != null) {
				CustomVideoView.this.mOnCompletionListener
						.onCompletion(CustomVideoView.this.mMediaPlayer);
			}
		}
	};

	private final OnErrorListener mErrorListener = new OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mp, int a, int b) {
			Log.d("VideoView", "Error: " + a + "," + b);

			/* If an error handler has been supplied, use it and finish. */
			if (CustomVideoView.this.mOnErrorListener != null) {
				if (CustomVideoView.this.mOnErrorListener.onError(
						CustomVideoView.this.mMediaPlayer, a, b)) {
					return true;
				}
			}

			/*
			 * Otherwise, pop up an error dialog so the user knows that
			 * something bad has happened. Only try and pop up the dialog if
			 * we're attached to a window. When we're going away and no longer
			 * have a window, don't bother showing the user an error.
			 */
			if (CustomVideoView.this.getWindowToken() != null) {
				//Resources r = CustomVideoView.this.mContext.getResources();
				new AlertDialog.Builder(CustomVideoView.this.mContext)
						.setTitle("Error")
						.setMessage("Video can not play!")
						.setPositiveButton("Ok",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										/*
										 * If we get here, there is no onError
										 * listener, so at least inform them
										 * that the video is over.
										 */
										if (CustomVideoView.this.mOnCompletionListener != null) {
											CustomVideoView.this.mOnCompletionListener
													.onCompletion(CustomVideoView.this.mMediaPlayer);
										}
									}
								}).setCancelable(false).show();
			}
			return true;
		}
	};

	private final MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			if (mp == null) {
				return;
			}
			CustomVideoView.this.mCurrentBufferPercentage = percent;

		}
	};

	private final MediaPlayer.OnInfoListener mOnInfoListerner = new MediaPlayer.OnInfoListener() {
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			if (CustomVideoView.this.mProgressBar != null) {
				if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
					CustomVideoView.this.mProgressBar
							.setVisibility(View.VISIBLE);
				} else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
					CustomVideoView.this.mProgressBar
							.setVisibility(View.INVISIBLE);
				}
			}
			if (what != MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
                Log.i(TAG, "deve Info (" + what + "," + extra + ")");
                int vWidth = mp.getVideoWidth();
                int vHeight = mp.getVideoHeight();
                int boxWidth = screenWidth ;
                int boxHeight = screenHeight ;

                Log.i(TAG, "deve Info ");
        	    Log.i(TAG, "deve Info videoWidth "+vWidth);
        	    Log.i(TAG, "deve Info videoHeight "+vHeight);
        	    Log.i(TAG, "deve Info boxWidth "+boxWidth);
        	    Log.i(TAG, "deve Info boxHeight "+boxHeight);
        	    Log.i(TAG, "deve Info screenWidth "+screenWidth);
        	    Log.i(TAG, "deve Info screenHeight "+screenHeight);

//        	    int ar = 1 ;
//        	    if(vWidth>0 && vHeight>0){
//        	    	int wr = boxWidth / vWidth;
//        	    	int hr = boxHeight / vHeight;
//        	    	ar = vWidth / vHeight;
//
//        	    	//if (wr > hr)
//             	    //	videoWidth = (int) (vHeight / ar);
//         	        //else
//         	        //	videoHeight = (int) (vWidth * ar);
//
//        	    	if(vWidth>vHeight){
//        	    		videoWidth = vWidth * wr ;
//        	    	}
//        	    	else{
//
//        	    	}
//        	    	Log.i(TAG, "deve Info final videoWidth "+videoWidth);
//             	    Log.i(TAG, "deve Info final videoHeight "+videoHeight);
//
//        	    }
        	    int ar = 1 ;
        	    if(vWidth>0 && vHeight>0){
        	    	int wr = boxWidth / vWidth;
        	    	int hr = boxHeight / vHeight ;
        	    	ar = vWidth / vHeight;

        	    	if(vWidth>vHeight){
        	    		if(hr<1){
        	    			Log.i(TAG, "deve Info inside hr < 1 ");
	        	    		if(wr>1){
	        	    			videoWidth = vWidth * wr ;
	        	    			/*int ak = vWidth * boxHeight ;
	        	    			videoWidth = ak/vHeight ;
	        	    			Log.i(TAG, "deve Info else 2 screenWidth "+videoWidth);
	        	    			if(videoWidth>boxWidth){
	        	    				int bk = videoHeight * boxWidth ;
	        	    				videoHeight = bk/videoWidth ;
	        	    				videoWidth = boxWidth ;
	        	    			}*/
	        	    		}
	        	    		else{
	        	    			Log.i(TAG, "deve Info else con ");
	        	    			int ak = vWidth * boxHeight ;
	        	    			videoWidth = ak/vHeight ;
	        	    			Log.i(TAG, "deve Info else 2 screenWidth "+videoWidth);
	        	    			if(videoWidth>boxWidth){
	        	    				int bk = videoHeight * boxWidth ;
	        	    				videoHeight = bk/videoWidth ;
	        	    				videoWidth = boxWidth ;
	        	    			}
	        	    		}
        	    		}
        	    		else{
        	    			Log.i(TAG, "deve Info inside hr > 1 ");
        	    			if(wr>1){
        	    				Log.i(TAG, "deve Info inside wr > 1 ");
        	    				int ak = vWidth * boxHeight ;
	        	    			videoWidth = ak/vHeight ;

	        	    			Log.i(TAG, "deve Info if 3 screenWidth "+videoWidth);
	        	    			if(videoWidth>boxWidth){
	        	    				int bk = videoHeight * boxWidth ;
	        	    				videoHeight = bk/videoWidth ;
	        	    				videoWidth = boxWidth ;
	        	    			}
	        	    		}
        	    			else{
        	    				Log.i(TAG, "deve Info inside wr < 1 ");
        	    				int ak = vWidth * boxHeight ;
	        	    			videoWidth = ak/vHeight ;

	        	    			Log.i(TAG, "deve Info if 3 screenWidth "+videoWidth);
	        	    			if(videoWidth>boxWidth){
	        	    				int bk = videoHeight * boxWidth ;
	        	    				videoHeight = bk/videoWidth ;
	        	    				videoWidth = boxWidth ;
	        	    			}
        	    			}
        	    		}
        	    	}
        	    	else{

        	    	}
        	    	Log.i(TAG, "deve Info final videoWidth "+videoWidth);
             	    Log.i(TAG, "deve Info final videoHeight "+videoHeight);
        	    }
        	    LayoutParams lp = (LayoutParams) CustomVideoView.this.getLayoutParams();
				lp.width = videoWidth ;
				lp.height = videoHeight ;;

				CustomVideoView.this.getHolder().setFixedSize(videoWidth, videoHeight) ;
				CustomVideoView.this.setLayoutParams (lp);

				//////////////////////////////////////////////////////////////
                if(getSplashScreen()!=null){
                	getSplashScreen().setVisibility(View.INVISIBLE);
                }
                if(getAudioManager()!=null){
                	getAudioManager().setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
                Log.i(TAG, "deve Info ( isPauseState " +isPauseState + ")");
                if(isPauseState){
                	//mMediaPlayer.pause();
                	isPauseState = false ;
                	getAudioManager().setStreamMute(AudioManager.STREAM_MUSIC, true);
                	TimerTask task = new PauseTimer();
            		Timer tm = new Timer();
            		tm.schedule(task, 700, 700);
                }
			}
			return false;
		}
	};

	private final MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			Log.i(CustomVideoView.TAG,
					"Calling seek complete Listener...");
			// isSeeking = false;
			CustomVideoView.this.mSeekListener.onSeekComplete();
		}

	};


	SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
			if (!CustomVideoView.this.wasVideoPaused) {
				CustomVideoView.this.getHolder().setFixedSize(
						CustomVideoView.this.mVideoWidth,
						CustomVideoView.this.mVideoHeight);
				if (CustomVideoView.this.mIsPrepared
						&& (CustomVideoView.this.mVideoWidth == w)
						&& (CustomVideoView.this.mVideoHeight == h)) {
					if (CustomVideoView.this.mMediaPlayer != null) {
						CustomVideoView.this.mMediaPlayer.start();
					}
				}
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i(CustomVideoView.TAG, "Surface created...");
			CustomVideoView.this.mSurfaceHolder = holder;

			CustomVideoView.this.openVideo();

		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// after we return from this we can't use the surface any more
			Log.i(CustomVideoView.TAG, "surfaceDestroyed ....");

			try {
				//int x = audioManager.abandonAudioFocus (afChangeListener);
				//Log.i(TAG, " ... x = " +x);
				//CustomVideoView.this.stopPlayback();
				CustomVideoView.this.mSurfaceHolder = null;
				if (CustomVideoView.this.mMediaPlayer != null) {
					CustomVideoView.this.mMediaPlayer.reset();
					CustomVideoView.this.mMediaPlayer.release();
					CustomVideoView.this.mMediaPlayer = null;
				}
			} catch (Throwable e) {
			}
		}

	};

	private int mCurrentPos = 0;

	public CustomVideoView(Context context) {
		this(context, null, 0);
	}

	public CustomVideoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CustomVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.mContext = context;
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		screenWidth = display.getWidth() ;
		screenHeight = display.getHeight() ;
		videoWidth = display.getWidth() ;
		videoHeight = display.getHeight() ;
		CustomVideoView.lastTimeSeeked = 0;
		this.initVideoView();
	}


	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getBufferPercentage() {
		if (this.mMediaPlayer != null) {
			return this.mCurrentBufferPercentage;
		}
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		if ((this.mMediaPlayer != null) && this.mIsPrepared) {
			int currentPosition = this.mMediaPlayer.getCurrentPosition();

			if (this.mCurrentPos == currentPosition) {

				this.mBufferingListener.onBuffering(true);
			} else {
				this.mBufferingListener.onBuffering(false);
			}
			this.mCurrentPos = currentPosition;

			if (currentPosition < this.lastSeekPos) {
				return this.lastSeekPos;
			}
			return currentPosition;
		}
		return 0;
	}

	@Override
	public int getDuration() {
		if ((this.mMediaPlayer != null) && this.mIsPrepared) {
			return this.mMediaPlayer.getDuration();
		}
		return -1;
	}

	public int getLastPressedKey() {
		return this.lastPressedKey;
	}

	public TextView getLogText() {
		return this.logText;
	}

	public ProgressBar getmProgressBar() {
		return this.mProgressBar;
	}

	public VideoPlayerScreen getvScreen() {
		return this.vScreen;
	}

	private void initVideoView() {
		
		this.mVideoWidth = 0;
		this.mVideoHeight = 0;
		this.getHolder().addCallback(this.mSHCallback);
		this.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		this.setFocusable(true);
		this.setFocusableInTouchMode(true);
		this.requestFocus();
	}

	@Override
	public boolean isPlaying() {
		if ((this.mMediaPlayer != null) && this.mIsPrepared) {
			return this.mMediaPlayer.isPlaying();
		}
		return false;
	}

	
	// Kindle patch...
	public void onControllerHide() {
		if(this.playPause != null){
			this.playPause.setVisibility(View.VISIBLE);
			this.playPause.setEnabled(true);
		}
		if(this.getTopNavBar()!=null){
			this.getTopNavBar().setVisibility(View.VISIBLE);
		}
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		boolean res = true;
		// howManyTimes = 0;
		// isKeyDown = false;
		try {
			switch (keyCode) {
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				case KeyEvent.KEYCODE_MEDIA_REWIND:
				case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
					break;
				default:
					res = super.onKeyUp(keyCode, event);
					break;
			}
		} catch (Throwable e) {
		}
		return res;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

//		WindowManager wm = (WindowManager) VideoPlayerScreen.context.getSystemService(Context.WINDOW_SERVICE);
//		Display display = wm.getDefaultDisplay();
//		if(videoWidth==0){
//			videoWidth = display.getWidth() ;
//			videoHeight = display.getHeight() ;
//		}
//		int videoWidth = mMediaPlayer.getVideoWidth();
//	    int videoHeight = mMediaPlayer.getVideoHeight();
//	    Log.i(TAG, "inside onMeasure ");
//	    Log.i(TAG, "inside videoWidth "+videoWidth);
//	    Log.i(TAG, "inside videoHeight "+videoHeight);
	    
		//this.setMeasuredDimension(
		//		display.getWidth(),
		//		display.getHeight());
		//this.setMeasuredDimension(100,100);
		this.setMeasuredDimension(videoWidth,videoHeight);

	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		//this.toggleMediaControlsVisiblity();
		
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		this.toggleMediaControlsVisiblity();
		return false;
	}

	public void openVideo() {
		this.mStartWhenPrepared = false;
		this.mSeekWhenPrepared = 0;
		this.lastPressedKey = 0;
		if (this.mProgressBar != null) {
			this.mProgressBar.setVisibility(View.VISIBLE);
		}
		if (((this.mUri == null) && (this.mFd == null))
				|| (this.mSurfaceHolder == null)) {
			// not ready for playback just yet, will try again later
			return;
		}
		
		Intent i = new Intent("com.android.music.musicservicecommand");
		i.putExtra("command", "pause");
		this.mContext.sendBroadcast(i);

		if (this.mMediaPlayer != null) {
			
			this.mMediaPlayer.reset();
			this.mMediaPlayer.release();
			this.mMediaPlayer = null;
		}
		try {
			this.mMediaPlayer = new MediaPlayer();
			
			this.mMediaPlayer.setOnPreparedListener(this.mPreparedListener);
			this.mIsPrepared = false;
			this.mMediaPlayer.setOnCompletionListener(this.mCompletionListener);
			this.mMediaPlayer.setOnErrorListener(this.mErrorListener);
			this.mMediaPlayer
					.setOnBufferingUpdateListener(this.mBufferingUpdateListener);
			this.mMediaPlayer.setOnInfoListener(this.mOnInfoListerner);
			this.mMediaPlayer
					.setOnSeekCompleteListener(this.mOnSeekCompleteListener);
			this.mCurrentBufferPercentage = 0;
			if (this.mUri != null) {
				this.mMediaPlayer.setDataSource(this.mContext, this.mUri);
			} else {
				this.mMediaPlayer.setDataSource(this.mFd);
			}
			this.mMediaPlayer.setDisplay(this.mSurfaceHolder);
			this.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			this.mMediaPlayer.setScreenOnWhilePlaying(true);
			this.mMediaPlayer.prepareAsync();
			this.buffDialogHandler = new Handler();

			// This will control the loading progressbar while buffering
			this.startRepeatingTask();
			this.requestLayout();
			this.invalidate();

		} catch (IOException ex) {
			Log.w("VideoView", "Unable to open content: " + this.mUri, ex);
			return;
		} catch (IllegalArgumentException ex) {
			Log.w("VideoView", "Unable to open content: " + this.mUri, ex);
			return;
		} finally {
		}
	}

	@Override
	public void pause() {
		Log.i(CustomVideoView.TAG, "Paused....");
		if ((this.mMediaPlayer != null) && this.mIsPrepared) {
			if (this.mMediaPlayer.isPlaying()) {

				this.mMediaPlayer.pause();
				this.wasVideoPaused = true;
			}
			if (this.mListener != null) {
				this.mListener.onPause();
			}
		}
		this.mStartWhenPrepared = false;
	}

	public int resolveAdjustedSize(int desiredSize, int measureSpec) {
		int result = desiredSize;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		switch (specMode) {
			case MeasureSpec.UNSPECIFIED:
				/*
				 * don't be larger than max size imposed on ourselves.
				 */
				result = desiredSize;
				break;

			case MeasureSpec.AT_MOST:
				/*
				 * Don't be larger than specSize, and don't be larger than the max
				 * size imposed on ourselves.
				 */
				result = Math.min(desiredSize, specSize);
				break;

			case MeasureSpec.EXACTLY:
				// No choice. Do what is provided.
				result = specSize;
				break;
		}
		return result;
	}

	@Override
	public synchronized void seekTo(int msec) {
		try {
			if (msec == this.mMediaPlayer.getCurrentPosition()) {
				return;
			}
			long now = (new Date()).getTime();
			if ((now - CustomVideoView.lastTimeSeeked) < 200) {
				return;
			}
			CustomVideoView.lastTimeSeeked = now;
			if (msec < 0) {
				msec = 0;
			}
			if ((this.mMediaPlayer != null) && this.mIsPrepared) {

				if (msec > this.mMediaPlayer.getDuration()) {
					msec = this.mMediaPlayer.getDuration();
				}

				this.mMediaPlayer.seekTo(msec);
				this.lastSeekPos = msec;
			} else {
				this.mSeekWhenPrepared = msec;
			}
		} catch (Throwable e) {
		}

	}
	public boolean isPauseState() {
		return isPauseState;
	}

	public void setPauseState(boolean isPauseState) {
		this.isPauseState = isPauseState;
	}
	public LinearLayout getSplashScreen() {
		return splashScreen ;
	}

	public void setSplashScreen(LinearLayout splashScreen) {
		this.splashScreen = splashScreen;
	}
	public void setLastPressedKey(int lastPressedKey) {
		this.lastPressedKey = lastPressedKey;
	}

	public void setLogText(TextView logText) {
		this.logText = logText;
	}

	public void setmProgressBar(ProgressBar mProgressBar) {
		this.mProgressBar = mProgressBar;
	}

	public void setOnBufferingListener(IBufferingListener mBufferingListener) {
		this.mBufferingListener = mBufferingListener;
	}

	public ImageView getPlayPause() {
		return playPause;
	}

	public void setPlayPause(ImageView playPause) {
		this.playPause = playPause;
		isFirstPlayPause = true ;
		TimerTask task = new MyTimer();
		timer = new Timer();
		timer.schedule(task, 1000, 1000);
		
	}
	public LinearLayout getTopNavBar() {
		return topNavBar;
	}

	public void setTopNavBar(LinearLayout topNavBar) {
		this.topNavBar = topNavBar;
		
	}
	public AudioManager getAudioManager() {
		return audioManager;
	}

	public void setAudioManager(AudioManager audioManager) {
		this.audioManager = audioManager;
	}
	/**
	 * Register a callback to be invoked when the end of a media file has been
	 * reached during playback.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnCompletionListener(OnCompletionListener l) {
		this.mOnCompletionListener = l;
	}

	/**
	 * Register a callback to be invoked when an error occurs during playback or
	 * setup. If no listener is specified, or if the listener returned false,
	 * VideoView will inform the user of any errors.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnErrorListener(OnErrorListener l) {
		this.mOnErrorListener = l;
	}

	/**
	 * Register a callback to be invoked when the media file is loaded and ready
	 * to go.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
		this.mOnPreparedListener = l;
	}

	public void setOnSeekListener(ISeekListener mSeekListener) {
		this.mSeekListener = mSeekListener;
	}

	public void setPlayPauseListener(IPlayPauseListener listener) {
		this.mListener = listener;
	}

	public void setVideoFD(FileDescriptor fd) {
		this.mFd = fd;
		this.mUri = null;
		// this.openVideo();
	}

	public void setVideoPath(String path) {
		this.setVideoURI(Uri.parse(path));
	}

	public void setVideoURI(Uri uri) {
		// isSeeking = false;
		this.mUri = uri;
		this.mFd = null;
		// this.openVideo();
	}

	public void setvScreen(VideoPlayerScreen vScreen) {
		this.vScreen = vScreen;
	}

	@Override
	public void start() {
		if ((this.mMediaPlayer != null) && this.mIsPrepared) {
			
			this.mMediaPlayer.start();
			this.wasVideoPaused = false;
			this.mStartWhenPrepared = false;
		} else {
			this.mStartWhenPrepared = true;
		}
		if (this.mListener != null) {
			this.mListener.onPlay();
		}
	}

	void startRepeatingTask() {
		this.m_statusChecker.run();
	}

	public void stopPlayback() {
		Log.i(CustomVideoView.TAG, "stopPlayback ... ");
		if (this.mMediaPlayer != null) {
			this.mMediaPlayer.stop();
			this.mMediaPlayer.release();
			this.mMediaPlayer = null;

		}
		this.stopRepeatingTask();
		this.buffDialogHandler = null;
	}

	void stopRepeatingTask() {
		if (this.buffDialogHandler != null) {
			this.buffDialogHandler.removeCallbacks(this.m_statusChecker);
		}
	}

	public void toggleMediaControlsVisiblity() {
		
		Log.i(TAG, "inside toggleMediaControlsVisiblity");
		
		if(this.playPause != null){
			if(this.playPause.getVisibility() == View.INVISIBLE){
				this.playPause.setVisibility(View.VISIBLE);
				this.playPause.setEnabled(true);
				if(this.getTopNavBar()!=null){
					this.getTopNavBar().setVisibility(View.VISIBLE);
				}
				if(isFirstPlayPause){
					isFirstPlayPause = false ;
					this.playPause.setImageResource(R.drawable.ic_media_pause);
				}
				Log.i(CustomVideoView.TAG,
						"toggleMediaControlsVisiblity isShowing() = set visible if cond") ;
				
				TimerTask task1 = new MyTimer();
				timer = new Timer();
				timer.schedule(task1, 1000, 1000);
				
				Log.i(CustomVideoView.TAG,
						"toggleMediaControlsVisiblity timer started if cond") ;
			}
			else{
				this.playPause.setVisibility(View.INVISIBLE);
				this.playPause.setEnabled(false);
				if(this.topNavBar!=null){
					this.topNavBar.setVisibility(View.INVISIBLE);
				}
				if(isFirstPlayPause){
					isFirstPlayPause = false ;
					this.playPause.setImageResource(R.drawable.ic_media_pause);
					this.playPause.setVisibility(View.VISIBLE);
					this.playPause.setEnabled(true);
					if(this.topNavBar!=null){
						this.topNavBar.setVisibility(View.VISIBLE);
					}
					TimerTask task = new MyTimer();
					timer = new Timer();
					timer.schedule(task, 1000, 1000);
				}
				else{
					if(timer!=null){
						timer.cancel();
					}
				}
				Log.i(CustomVideoView.TAG,
						"toggleMediaControlsVisiblity isShowing() = set invisible else cond") ;
				
			}
			
		}
		
	}

//	@Override
//	public int getAudioSessionId() {
//		// TODO Auto-generated method stub
//		return 0;
//	}

	public class MyTimer extends TimerTask {
		private int time = 0;

		@Override
		public void run() {
			// TODO Auto-generated method stub
			time = time + 1 ;
			Log.i(TAG, "inside timer time "+time);
			if(time>5){
				//playPause.setVisibility(View.INVISIBLE);
				playPause.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if(vScreen.mVideoView.isPlaying()){
						 playPause.setVisibility(View.INVISIBLE);
						 playPause.setEnabled(false);
						}
					}
				});
				
				if(getTopNavBar()!=null){
					getTopNavBar().post(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							if(vScreen.mVideoView.isPlaying()){
								getTopNavBar().setVisibility(View.INVISIBLE);
							}
						}
					});
				}
				Log.i(TAG, "inside timer task "+time);
				this.cancel();
			}
		}
		
	}
	public class PauseTimer extends TimerTask {
		private int time;

		@Override
		public void run() {
			// TODO Auto-generated method stub
			time = time + 1 ;
			Log.i(TAG, "inside timer time "+time);
			if(time>0){
				mMediaPlayer.pause();
				getAudioManager().setStreamMute(AudioManager.STREAM_MUSIC, false);
				this.cancel();
			}
		}
		
	}
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		// TODO Auto-generated method stub
		int videoWidth = mp.getVideoWidth();
	    int videoHeight = mp.getVideoHeight();
	    Log.i(TAG, "onVideoSizeChanged ");
	    Log.i(TAG, "onVideoSizeChanged videoWidth "+videoWidth);
	    Log.i(TAG, "onVideoSizeChanged videoHeight "+videoHeight);
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}
	

}

package com.jtv.player;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.fragment.app.FragmentActivity;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaRouter;

import com.jtv.player.R;

import java.util.Timer;
import java.util.TimerTask;


public class VideoPlayerScreen extends FragmentActivity implements OnClickListener{
	
	private boolean pauseState = false ;
	private int focusChangeId = 0 ;
	private AudioManager am ;
	private final static String TAG = "VideoPlayerScreen";

	private String httpLiveUrl = "MEDIA_URL";
	public static Context context ;
	/** Called when the activity is first created. */
	private long mLastPosition = 0 ;
	private boolean isBuffering;
	private boolean mPlayStarted;
	private boolean isFirstTimeStart ;
	private boolean isResume;
	public CustomVideoView mVideoView ;
	//public VideoView adVideoView ;

	private String videoLink = "";
	private long startTimeOfAnEvent;
	private boolean isComplete;
	private boolean hasAlreadyExecuted = false ;
	private boolean isVideoPaused;
	private TextView logText;
	private ProgressBar mProgressBar ;
	private boolean isDialogOpen = false  ;
	private ImageView playPause ;
	private boolean isVideoPlaying ;
	private boolean isAdVideoPlaying ;
	private ChromeCastInteraction chromeCastInteraction ;
	private MediaRouteButton mediaRouteButton ;
	private LinearLayout posterLayer ;
	private LinearLayout topNavBar ;
	private LinearLayout splashImage ;
	private LinearLayout videoViewParent ;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.custom_video);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		context = this ;
		Log.i(TAG, "inside VideoPlayerScreen ");
		am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		am.setStreamMute(AudioManager.STREAM_MUSIC, true);
		//getMediaResolution();
		//VideoPlayerScreen.this.initListener();
    	//VideoPlayerScreen.this.initCasting();
		this.mProgressBar = (ProgressBar) this.findViewById(R.id.progressBar1);
		this.mProgressBar.setVisibility(View.INVISIBLE) ;
		VideoPlayerScreen.this.initListener();
		VideoPlayerScreen.this.initCasting();
		this.splashScreenHandle();

	}

	private void splashScreenHandle(){
		
		  new Handler().postDelayed(new Runnable() {
              
            // Using handler with postDelayed called runnable run method
  
            @Override
            public void run() {
            	VideoPlayerScreen.this.mProgressBar.setVisibility(View.VISIBLE);
            	//VideoPlayerScreen.this.initListener();
            	VideoPlayerScreen.this.initVideoPlaying();
            	VideoPlayerScreen.this.initCasting();
            }
         }, 3000); // wait for 3 seconds
		  
	}
	IPlayPauseListener playPauseListener = new IPlayPauseListener() {

		@Override
		public void onBuffer(long timeBufferingStarted, int currentDuration) {

			if (VideoPlayerScreen.this.mLastPosition == currentDuration) {
				// Player is probably buffering...
				VideoPlayerScreen.this.isBuffering = true;
				if (!VideoPlayerScreen.this.hasAlreadyExecuted
						&& VideoPlayerScreen.this.mPlayStarted) {
					VideoPlayerScreen.this.isVideoPaused = true;
					VideoPlayerScreen.this.hasAlreadyExecuted = true;
					VideoPlayerScreen.this.isFirstTimeStart = false;
					VideoPlayerScreen.this.startTimeOfAnEvent = System
							.currentTimeMillis();
				}
				/**
				 * buffering started while the video was playing..
				 */
				if (VideoPlayerScreen.this.mPlayStarted
						&& !VideoPlayerScreen.this.isVideoPaused) {
					VideoPlayerScreen.this.mProgressBar
					.setVisibility(View.VISIBLE);
				}

			} else {
				//splashImage.setVisibility(View.GONE);
				VideoPlayerScreen.this.mProgressBar
				.setVisibility(View.INVISIBLE);
				VideoPlayerScreen.this.mLastPosition = currentDuration;
				if (VideoPlayerScreen.this.isBuffering
						&& VideoPlayerScreen.this.mPlayStarted) {

					if (!VideoPlayerScreen.this.isFirstTimeStart) {
						Log.i(VideoPlayerScreen.TAG,
								"Inside isFirstTimeStart -- ");
						VideoPlayerScreen.this.isVideoPaused = false;

						VideoPlayerScreen.this.startTimeOfAnEvent = System
								.currentTimeMillis();

					}
				}
				VideoPlayerScreen.this.hasAlreadyExecuted = false;
				VideoPlayerScreen.this.isBuffering = false;
			}
		}

		@Override
		public void onPause() {

			VideoPlayerScreen.this.mPlayStarted = false ;
			VideoPlayerScreen.this.isFirstTimeStart = false ;
			VideoPlayerScreen.this.isVideoPaused = true ;

			Log.i(
					VideoPlayerScreen.TAG,
					"Log Time"
							+ String.valueOf(System.currentTimeMillis()
									- VideoPlayerScreen.this.startTimeOfAnEvent));

			Log.i(VideoPlayerScreen.TAG, "startTimeOfAnEvent"
					+ VideoPlayerScreen.this.startTimeOfAnEvent);

			VideoPlayerScreen.this.startTimeOfAnEvent = System
					.currentTimeMillis();

			VideoPlayerScreen.this.isResume = false;

		}

		@Override
		public void onPlay() {

			VideoPlayerScreen.this.mPlayStarted = true;
			VideoPlayerScreen.this.isComplete = false;
			if (!VideoPlayerScreen.this.isFirstTimeStart) {

				Log.i(VideoPlayerScreen.TAG, "startTimeOfAnEvent"
						+ VideoPlayerScreen.this.startTimeOfAnEvent);
				Log.i(
						VideoPlayerScreen.TAG,
						"Log Time"
								+ String.valueOf(System.currentTimeMillis()
										- VideoPlayerScreen.this.startTimeOfAnEvent));


				VideoPlayerScreen.this.startTimeOfAnEvent = System
						.currentTimeMillis();

				VideoPlayerScreen.this.isResume = true;

			}

			Log.i(VideoPlayerScreen.TAG, "Play after buffer...");
			VideoPlayerScreen.this.isVideoPaused = false;
		}
	};

	/**
	 * user has completed 'seeking' the video. At this point the video is
	 * expected to start buffering and will only start playing once it is ready
	 * to play the part of the video where the user had seek'd to.
	 */
	ISeekListener mSeekListener = new ISeekListener() {

		@Override
		public void onSeekComplete() {
			VideoPlayerScreen.this.startTimeOfAnEvent = System
					.currentTimeMillis();

		}
	};

	IBufferingListener mBufferingListener = new IBufferingListener() {

		@Override
		public void onBuffering(boolean isBuffering) {
			if (isBuffering) {
				if (VideoPlayerScreen.this.mVideoView.isPlaying()) {
					VideoPlayerScreen.this.mHandler.sendEmptyMessage(1);
				}
			} else {
				VideoPlayerScreen.this.mHandler.sendEmptyMessage(2);
			}
		}
	};
	Handler mHandler = new Handler() {
		@Override
		public void dispatchMessage(Message msg) {
			if (msg.what == 1) {
				if (VideoPlayerScreen.this.mProgressBar.getVisibility() != View.VISIBLE) {
					VideoPlayerScreen.this.mProgressBar
					.setVisibility(View.VISIBLE);
				}

			} else {
				//splashImage.setVisibility(View.GONE);
				VideoPlayerScreen.this.mProgressBar
				.setVisibility(View.INVISIBLE);

			}
			super.dispatchMessage(msg);
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		Log.i("key  preshed 222 ", " button presed "+keyCode);
		boolean returnStatus = false ;
		try {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_POWER:
				Log.i(VideoPlayerScreen.TAG, "POWER BTN PRESSED...") ;
				if (this.mVideoView != null) {
					this.mVideoView.pause();
				}
				returnStatus = true ;
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				if(this.mVideoView.isPlaying()){
					this.mVideoView.pause();
					pauseState = true ;
					VideoPlayerScreen.this.playPause.setImageResource(R.drawable.ic_media_play);
					VideoPlayerScreen.this.playPause.setVisibility(View.VISIBLE);
					VideoPlayerScreen.this.playPause.setEnabled(true);
					Log.i(TAG, " button CustomVideoView.isPauseState "+pauseState);
				}
				else{
					this.mVideoView.start();
					pauseState = false ;
					VideoPlayerScreen.this.playPause.setImageResource(R.drawable.ic_media_pause);
					VideoPlayerScreen.this.playPause.setVisibility(View.VISIBLE);
					VideoPlayerScreen.this.playPause.setEnabled(true);
					Log.i(TAG, " button CustomVideoView.isPauseState "+pauseState);
					TimerTask task = mVideoView.new MyTimer();
					mVideoView.timer = new Timer();
					mVideoView.timer.schedule(task, 1000, 1000);
				}
				returnStatus = true ;
				break;
			case KeyEvent.KEYCODE_BACK:
				Log.i("key back preshed ", "back button presed");
				if(!isDialogOpen){
					isVideoPlaying = VideoPlayerScreen.this.mVideoView.isPlaying() ;
					createAlertDialog("Do you want to exit the application?");
				}
				if(VideoPlayerScreen.this.mVideoView.isPlaying()){
					VideoPlayerScreen.this.mVideoView.pause();
				}
				else{
					//VideoPlayerScreen.this.adVideoView.pause();
				}
				returnStatus = true ;
				break;
			
			default:

				break;
			}
		} 
		catch (Throwable e) {

		}

		//return super.onKeyDown(keyCode, event);
		return returnStatus ;
	}
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false ;
	}
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return false ;
	}
	
	@Override
	public void finish() {
		Log.i(VideoPlayerScreen.TAG, "Finishing....") ;
		try {
			if (this.mVideoView != null) {
				Log.i(VideoPlayerScreen.TAG, "Finishing...."
						+ VideoPlayerScreen.this.mVideoView.getDuration());

				this.mVideoView.stopRepeatingTask();
				this.mVideoView.stopPlayback();
			}
			this.mVideoView = null;

			super.finish();
		} catch (Throwable e) {

		}
	}

	private void initListener() {

		this.posterLayer = (LinearLayout)this.findViewById(R.id.posterLayer);
		this.logText = (TextView) this.findViewById(R.id.log_text);
		this.mProgressBar = (ProgressBar) this.findViewById(R.id.progressBar1);
		// IMPORTANT **********************
		this.mVideoView = (CustomVideoView) this.findViewById(R.id.VideoView01);
		this.splashImage = (LinearLayout)findViewById(R.id.splashImage);
		this.videoViewParent = (LinearLayout)findViewById(R.id.videoViewParent); 
		
		this.mVideoView.setSplashScreen(this.splashImage);
		this.playPause = (ImageView)this.findViewById(R.id.playPause);
		this.topNavBar = (LinearLayout)this.findViewById(R.id.topNavBar);
		//this.layerTop = (LinearLayout)this.findViewById(R.id.layerTop);
		
		String isMcRequired = "yes";

		Log.d("VideoPlayer", "isMcRequired = " + isMcRequired);
		if (isMcRequired != null) {
			Log.d("VideoPlayer", "Setting MC");
		}

		this.mVideoView.setPlayPauseListener(this.playPauseListener);
		this.mVideoView.setOnSeekListener(this.mSeekListener);
		this.mVideoView.setOnBufferingListener(this.mBufferingListener);

		this.mVideoView.setLogText(this.logText);
		this.mVideoView.setvScreen(this);
		Log.i(VideoPlayerScreen.TAG, "1...");

		this.mVideoView.setmProgressBar(this.mProgressBar);
		this.mVideoView.setTopNavBar(this.topNavBar);
		this.mVideoView.setPlayPause(this.playPause);
		
		this.videoViewParent.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mVideoView.toggleMediaControlsVisiblity();
			}
		});
		
		initVideoPlaying();
		
	}

	private void initVideoPlaying(){
		this.videoLink = httpLiveUrl ;
		int result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.d(TAG, "Successfull to request audioFocus listener");

			mVideoView.setAudioManager(am);
			this.playVideo(this.videoLink) ;

			this.playPause.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(mVideoView.isPlaying()){
						mVideoView.pause();
						playPause.setImageResource(R.drawable.ic_media_play) ;
						pauseState = true ;
					}
					else{
						playPause.setImageResource(R.drawable.ic_media_pause);
						pauseState = false ;
						mVideoView.start();
						TimerTask task = mVideoView.new MyTimer();
						mVideoView.timer = new Timer();
						mVideoView.timer.schedule(task, 1000, 1000);
					}

				}
			});

			Log.i(TAG, "video file link "+this.videoLink) ;
			Log.i(VideoPlayerScreen.TAG, "4...");
		} else {
			Log.e(TAG, "Failure to request focus listener");
			AlertDialog alertDialog = new AlertDialog.Builder(
					VideoPlayerScreen.this).create();
			alertDialog.setTitle("Alert Dialog");
			alertDialog.setMessage("Audio not found");
			//alertDialog.setIcon(R.drawable.tick);
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
					System.exit(0);
				}
			});
			
			alertDialog.show();
		}
	}
	private void initCasting(){

		//mediaRouteButton = (MediaRouteButton) findViewById(R.id.media_route_button);
		//chromeCastInteraction = new ChromeCastInteraction(VideoPlayerScreen.this, mediaRouteButton,am);
		//chromeCastInteraction.getCastComponents("", "",this.posterLayer,this.playPause,mVideoView);
	}
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// event...
		if (!this.isComplete) {
			// send stop report because player is existing while the movie is
			// playing..
			// LogUtil.log(TAG,"On Destroy====");

			if (this.isResume) {
			} else {
			}

		}


	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onPause() {
		//Log.i(TAG, "CustomVideoView.isPauseState  "+CustomVideoView.isPauseState );
		Log.i(TAG, "pauseState "+pauseState );
		if(mVideoView!=null && mVideoView.mMediaPlayer!=null){
			am.setStreamMute(AudioManager.STREAM_MUSIC, true);
		}
		Log.i(TAG, "inside onPause ");
		if (isFinishing()) {
			if(chromeCastInteraction!=null && chromeCastInteraction.mMediaRouter!=null && chromeCastInteraction.mMediaRouterCallback!=null){
				chromeCastInteraction.mMediaRouter.removeCallback(chromeCastInteraction.mMediaRouterCallback);
			}
		}
		
		super.onPause();

	}
	
	@Override
	protected void onResume() {
		super.onResume() ;
		Log.i(TAG, "inside onResume ") ;
		Log.i(TAG, "pauseState  "+pauseState ) ;
		
		focusChangeId = 0 ;
		//int result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		if(mVideoView!=null && mVideoView.mMediaPlayer!=null){
			//Log.i(TAG, "pauseState " +CustomVideoView.isPauseState );
			mVideoView.setPauseState(pauseState);
			mVideoView.afChangeListener = afChangeListener ;
			am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			if(pauseState){
				mVideoView.pause() ;
			}
			pauseState = false ;
		}
		
		if(chromeCastInteraction!=null && chromeCastInteraction.mMediaRouter!=null && 
				chromeCastInteraction.mMediaRouterCallback!=null && chromeCastInteraction.mMediaRouteSelector!=null){
			chromeCastInteraction.mMediaRouter.addCallback(chromeCastInteraction.mMediaRouteSelector, chromeCastInteraction.mMediaRouterCallback,
					MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
		}
		
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.i(TAG, "inside onStart ");
	}
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.i(TAG, "inside onStop ");
		System.exit(0);
		
	}
	@Override
	protected void onUserLeaveHint() {
		// TODO Auto-generated method stub
		Log.i(TAG, "inside onUserLeaveHint ");
		Log.i(TAG, "inside onUserLeaveHint focusChangeId "+focusChangeId);
		if(mVideoView.isPlaying()){
			Log.i(TAG, "inside onUserLeaveHint video playing");
		}
		else{
			Log.i(TAG, "inside onUserLeaveHint stop");
		}
		super.onUserLeaveHint();
		
	}
	
	private void playVideo(final String movieUrl) {

		this.mVideoView.cancelLongPress();
		this.mVideoView.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				VideoPlayerScreen.this.mProgressBar
				.setVisibility(View.INVISIBLE);
				topNavBar.setVisibility(View.INVISIBLE);
				VideoPlayerScreen.this.isComplete = false;
				VideoPlayerScreen.this.isFirstTimeStart = true ;
				VideoPlayerScreen.this.isResume = true;
				VideoPlayerScreen.this.mPlayStarted = true;

				VideoPlayerScreen.this.mVideoView.start();
				
			}

		});
		this.mVideoView.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {

				return true;
			}

		});

		this.mVideoView.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.i("VideoPlayerScreen", "video ad complete ");
			}
		});

		VideoPlayerScreen.this.mVideoView.setVideoURI(Uri.parse(movieUrl));

		Log.i(TAG,"3...");

	}


	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		//super.onBackPressed();
		Log.i("inside on back 1", "inside on back ");
	}

	public void createAlertDialog(String msg){

		isDialogOpen = true ;
		AlertDialog.Builder dl = new AlertDialog.Builder(this)
		.setTitle(
				"Player")
				.setMessage(msg)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {

					@Override
					public void onClick(
							DialogInterface dialog,
							int whichButton) {
						isDialogOpen = false ;			
						pauseState = false ;
						VideoPlayerScreen.this.finish();
						System.exit(0);

					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if(isVideoPlaying){
							VideoPlayerScreen.this.mVideoView.start();
						}

//						if(isAdVideoPlaying){
//							VideoPlayerScreen.this.adVideoView.start();
//						}
						isDialogOpen = false ;
					}
				});

		dl.show();
	}

	
	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			Log.i(TAG, "audioManager ===== " + focusChange);
			focusChangeId = focusChange ;
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
				// Pause playback
				Log.i(TAG, "audioManager.AUDIOFOCUS_LOSS_TRANSIENT");
				am.abandonAudioFocus(afChangeListener);
			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
				Log.i(TAG, "audioManager.AUDIOFOCUS_GAIN");
				// Resume playback 
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				//am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
				am.abandonAudioFocus(afChangeListener);
				// Stop playback
				Log.i(TAG, "audioManager.AUDIOFOCUS_LOSS");
			}
		}
	};

//	private void getMediaResolution(){
//		MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
//		metaRetriever.setDataSource(adUrl);
//		String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
//		String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
//		Log.i(TAG, "video width "+width+" height "+height);
//	}

}
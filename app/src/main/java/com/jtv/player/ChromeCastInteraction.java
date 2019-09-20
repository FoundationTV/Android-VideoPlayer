package com.jtv.player;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.Listener;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.cast.RemoteMediaPlayer.OnStatusUpdatedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;
import com.jtv.player.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class ChromeCastInteraction {
	
	private final static String TAG = "ChromeCastInteraction" ;
	/////////////////////////////////////////////////////////////////////////////////////////////
	public MediaRouter mMediaRouter ;
	public MediaRouteSelector mMediaRouteSelector ;
	private CastDevice mSelectedDevice ;
	public MyMediaRouterCallback  mMediaRouterCallback ;
	private GoogleApiClient mApiClient ;
	private ConnectionFailedListener mConnectionFailedListener ;
	private ConnectionCallbacks mConnectionCallbacks ;
	private Listener mCastClientListener ;
	private String APP_ID = "######" ;
	private boolean mApplicationStarted ;
	private HelloWorldChannel mHelloWorldChannel ;
	private RemoteMediaPlayer mRemoteMediaPlayer ;
	private MediaRouteButton mMediaRouteButton ;
	private String sessionId ;
	private AudioManager audioManager ;

	private String httpLiveUrl = "######";

	private String videoImg = "" ;
	
	private String adUrl = "######" ;
	/////////////////////////////////////////////////////////////////////////////////////////////

	private Context context ;
	private CustomVideoView mVideoView ;
	private ImageView playPause ;
	private LinearLayout posterLayer ;
	
	private MediaMetadata mediaMetadata ;
	
	public ChromeCastInteraction(Context context,MediaRouteButton mMediaRouteButton,AudioManager am){
		this.context = context ;
		this.mMediaRouteButton = mMediaRouteButton ;
		audioManager = am ;
	}
	
	public void getCastComponents(String videoUrl, String poster, LinearLayout posterLayer, ImageView playPause, CustomVideoView mVideoView){
		 
		this.posterLayer = posterLayer ;
		this.playPause = playPause ;
		this.mVideoView = mVideoView ;
		
		 mMediaRouter = MediaRouter.getInstance(this.context) ;
		 mMediaRouteSelector = new MediaRouteSelector.Builder()
		    .addControlCategory(CastMediaControlIntent.categoryForCast(APP_ID))
		    .build();
		 mMediaRouterCallback = new MyMediaRouterCallback();
		 mConnectionFailedListener = new ConnectionFailedListener();
		 mConnectionCallbacks = new ConnectionCallbacks();
		 
		// Set the MediaRouteButton selector for device discovery.
		//mMediaRouteButton = routeButton ;
		 if(mMediaRouteButton!=null){
			 mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
			 
		 }
		 
	}
	
	private class MyMediaRouterCallback extends MediaRouter.Callback {

		  @Override
		  public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
			// TODO Auto-generated method stub
			super.onRouteAdded(router, route);
			mMediaRouteButton.setVisibility(View.VISIBLE);
		  }
		  @Override
		  public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
			// TODO Auto-generated method stub
			super.onRouteRemoved(router, route);
			mMediaRouteButton.setVisibility(View.GONE);
		  }
		  @Override
		  public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
		    mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
		    //String routeId = info.getId();
		    Log.i(TAG, "route id "+info.getId());
		    Log.i(TAG, "mSelectedDevice \n"+
		    "\n getDeviceId "+mSelectedDevice.getDeviceId()+
		    "\n getDeviceVersion "+mSelectedDevice.getDeviceVersion()+
		    "\n getFriendlyName "+mSelectedDevice.getFriendlyName()+
		    "\n getModelName "+mSelectedDevice.getModelName()+
		    "\n getServicePort "+mSelectedDevice.getServicePort()+
		    "\n mSelectedDevice "+mSelectedDevice.getIpAddress()+
		    "\n getServicePort "+mSelectedDevice.getServicePort());
		    mCastClientListener = new Listener() {
				  @Override
				  public void onApplicationStatusChanged() {
				    if (mApiClient != null) {
				      Log.d(TAG, "onApplicationStatusChanged: "
				       + Cast.CastApi.getApplicationStatus(mApiClient));
				    }
				  }

				  @Override
				  public void onVolumeChanged() {
				    if (mApiClient != null) {
				      Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
				    }
				  }

				  @Override
				  public void onApplicationDisconnected(int errorCode) {
				    teardown();
				  }
			};

			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
	                .builder(mSelectedDevice, mCastClientListener);

			mApiClient = new GoogleApiClient.Builder(ChromeCastInteraction.this.context)
			                        .addApi(Cast.API, apiOptionsBuilder.build())
			                        .addConnectionCallbacks(mConnectionCallbacks)
			                        .addOnConnectionFailedListener(mConnectionFailedListener)
			                        .build();

			mApiClient.connect();

		  }

		  @Override
		  public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
		    teardown();
		    mSelectedDevice = null;
		  }
	}

	private class ConnectionCallbacks implements
    GoogleApiClient.ConnectionCallbacks {

		@Override
		public void onConnected(Bundle connectionHint) {

		  try {
		      Cast.CastApi.launchApplication(mApiClient, APP_ID, false)
		        .setResultCallback(
		           new ResultCallback<Cast.ApplicationConnectionResult>() {

		          public void onResult(Cast.ApplicationConnectionResult result) {
		              Status status = result.getStatus();
		              if (status.isSuccess()) {
		                //ApplicationMetadata applicationMetadata =
		                //                                result.getApplicationMetadata();


		                sessionId = result.getSessionId();
		                String applicationStatus = result.getApplicationStatus();
		                boolean wasLaunched = result.getWasLaunched();
		                Log.e(TAG, "sessionId "+sessionId);
		                Log.e(TAG, "applicationStatus "+applicationStatus);
		                Log.e(TAG, "wasLaunched "+wasLaunched);
		                Log.e(TAG,"status "+ result.getStatus());
		                mApplicationStarted = true;

		                mRemoteMediaPlayer = new RemoteMediaPlayer();
		                mRemoteMediaPlayer.setOnStatusUpdatedListener(
		                                           new OnStatusUpdatedListener() {
		                  @Override
		                  public void onStatusUpdated() {
		                    MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
		                    boolean isPlaying = mediaStatus.getPlayerState() ==
		                            MediaStatus.PLAYER_STATE_PLAYING;
		                  }
		                });

		                mRemoteMediaPlayer.setOnMetadataUpdatedListener(
		                                           new RemoteMediaPlayer.OnMetadataUpdatedListener() {
		                  @Override
		                  public void onMetadataUpdated() {
		                    MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
		                    MediaMetadata metadata = mediaInfo.getMetadata();
		                  }
		                });

		                ChromeCastInteraction.this.castVideo();
	                    mHelloWorldChannel = new HelloWorldChannel();
	                    try {
	                     Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
	                          mHelloWorldChannel.getNamespace(),
	                          mHelloWorldChannel);
	                    } catch (IOException e) {
	                     Log.e(TAG, "Exception while creating channel", e);
	                    }

		              } else {
		                teardown();
		              }
		          }
		      });

		    } catch (Exception e) {
		      Log.e(TAG, "Failed to launch application", e);
		    }

	   }

		@Override
		public void onConnectionSuspended(int arg0) {
			// TODO Auto-generated method stub
			//mWaitingForReconnect = true;
		}
	}

	private class ConnectionFailedListener implements
	    GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult arg0) {
			// TODO Auto-generated method stub
			teardown();
		}
	}

	class HelloWorldChannel implements Cast.MessageReceivedCallback {
		  public String getNamespace() {
		    return "urn:x-cast:"+ ChromeCastInteraction.this.context.getPackageName();
		  }

		  @Override
		  public void onMessageReceived(CastDevice castDevice, String namespace,
		        String message) {
		    Log.d(TAG, "onMessageReceived: " + message);
		  }
	}

	private void castVideo(){

		///////////////////////////////////////////////////////////////////////////
		 if(ChromeCastInteraction.this.mVideoView!=null){
         	ChromeCastInteraction.this.mVideoView.pause();
			 //ChromeCastInteraction.this.mVideoView.stopPlayback();;
			//audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
         	if(ChromeCastInteraction.this.posterLayer!=null){
         		ChromeCastInteraction.this.posterLayer.setVisibility(View.VISIBLE);
         	}
         	if(ChromeCastInteraction.this.playPause!=null){
         		ChromeCastInteraction.this.playPause.setImageResource(R.drawable.ic_media_play);
         		ChromeCastInteraction.this.playPause.setClickable(false);
         		ChromeCastInteraction.this.playPause.setVisibility(View.GONE);
         	}
         }
		//////////////////////////////////////////////////////////////////////////
		mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
		mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My video");
		mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, "Video Description");


		Uri uri = Uri.parse(videoImg) ;
		WebImage img = new WebImage(uri, 165, 200);
		mediaMetadata.addImage(img);

//		MediaInfo mediaInfo = new MediaInfo.Builder(
//			httpLiveUrl)
//		    .setContentType("application/x-mpegURL")
//		    .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
//		    .setMetadata(mediaMetadata)
//		              .build();
		//.setContentType("application/x-mpegURL")

		MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
	    mediaMetadata.putString(MediaMetadata.KEY_TITLE, "LC");
	    mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, "Player");
	    mediaMetadata.putString(MediaMetadata.KEY_STUDIO, "Studio");
	    mediaMetadata.addImage(new WebImage(uri));
	    mediaMetadata.addImage(new WebImage(uri));//big image
	    MediaInfo mediaInfo = new MediaInfo.Builder(
	    		httpLiveUrl)
	    				.setContentType("video/mp4")
	    				//.setContentType("aapplication/vnd.apple.mpegurl")
	                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
	                    .setMetadata(mediaMetadata)
	                    .build();

	    //.setContentType("application/vnd.apple.mpegurl")
	    //mVideoCastManager.startCastControllerActivity(context, mediaInfo, 0, true);
	    //.setContentType("video/mp4")
	    // .setContentType("application/x-mpegURL")
		try {
		  mRemoteMediaPlayer.load(mApiClient, mediaInfo, true)
		     .setResultCallback(new ResultCallback<MediaChannelResult>() {
			    @Override
			    public void onResult(MediaChannelResult result) {
			      if (result.getStatus().isSuccess()) {
			        Log.d(TAG, "Media loaded successfully");
			      }
			    }
		     });

		} catch (IllegalStateException e) {
		  Log.e(TAG, "Problem occurred with media during loading", e);
		} catch (Exception e) {
		  Log.e(TAG, "Problem opening media during loading", e);
		}

		mRemoteMediaPlayer.setOnStatusUpdatedListener(new OnStatusUpdatedListener() {

			@Override
			public void onStatusUpdated() {
				// TODO Auto-generated method stub
				 Log.e(TAG, "status update"+ mRemoteMediaPlayer.getMediaStatus());
			}
		});

	}

	private void teardown() {

		 if(ChromeCastInteraction.this.mVideoView!=null){
         	ChromeCastInteraction.this.mVideoView.start();
			//audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
         	if(ChromeCastInteraction.this.posterLayer!=null){
         		ChromeCastInteraction.this.posterLayer.setVisibility(View.GONE);
         	}
         	if(ChromeCastInteraction.this.playPause!=null){
         		ChromeCastInteraction.this.playPause.setImageResource(R.drawable.ic_media_pause);
         		ChromeCastInteraction.this.playPause.setClickable(true);
         		ChromeCastInteraction.this.playPause.setVisibility(View.VISIBLE);
         	}
         }

	    Log.d(TAG, "teardown");
	    if (mApiClient != null) {
	      if (mApplicationStarted) {
	        //if (mApiClient.isConnected()) {
	          try {
	            Cast.CastApi.stopApplication(mApiClient, sessionId);
	            if (mHelloWorldChannel != null) {
	              Cast.CastApi.removeMessageReceivedCallbacks(
	                mApiClient,
	                mHelloWorldChannel.getNamespace());
	              mHelloWorldChannel = null;
	            }
	          } catch (IOException e) {
	                 Log.e(TAG, "Exception while removing channel", e);
	          }
	          mApiClient.disconnect();
	        //}
	        mApplicationStarted = false;
	      }
	      mApiClient = null;
	    }

	  mSelectedDevice = null;
	  //mWaitingForReconnect = false;
	  sessionId = null;
   }

   private void callAdOnDelay(){

		//////////////////////////////////////////////////////////////////////////
		mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My Ad"); // http://api.junctiontv.com/jtv/movie/12-YEARS-A-SLAVE.mp4
		mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, "Ad desc");

		Uri uri = Uri.parse(videoImg) ;
		WebImage img = new WebImage(uri, 165, 200);
		mediaMetadata.addImage(img);

		final Timer myTimer = new Timer();
		myTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				((Activity)context).runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub

						MediaInfo mediaInfoAd = new MediaInfo.Builder(
								adUrl)
							    .setContentType("video/mp4")
							    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
							    .setMetadata(mediaMetadata)
							              .build();

						try{
							 mRemoteMediaPlayer.load(mApiClient, mediaInfoAd, true)
						     .setResultCallback(new ResultCallback<MediaChannelResult>() {
							    @Override
							    public void onResult(MediaChannelResult result) {
							      if (result.getStatus().isSuccess()) {
							        Log.d(TAG, "Media loaded successfully");
							      }
							    }
						     });
							 
							 myTimer.cancel();
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
				});
			}
		}, 40000,40000);
		
	}
}

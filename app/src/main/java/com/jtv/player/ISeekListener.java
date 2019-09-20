package com.jtv.player;

/**  
 *  user has forwarded or rewinded the video using the seek bar. A new event has occurred
 *  and start time needs to be set to 0 again.
 */


public interface ISeekListener {
	void onSeekComplete();
}

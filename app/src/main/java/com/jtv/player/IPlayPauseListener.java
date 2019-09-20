package com.jtv.player;

public interface IPlayPauseListener {

	
		void onPlay();         
		void onPause(); 
		void onBuffer(long currentTime, int currentDuration);
	
	
}

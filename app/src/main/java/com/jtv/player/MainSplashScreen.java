package com.jtv.player;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.jtv.player.R;

public class MainSplashScreen extends Activity{

	//private Handler hl ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main) ;
		Log.i("MainSplashScreen", "inside MainSplashScreen ");
        new Handler().postDelayed(new Runnable() {
              
            @Override
            public void run() {
                Intent i = new Intent(MainSplashScreen.this, VideoPlayerScreen.class);
                startActivity(i);
                MainSplashScreen.this.finish();
                overridePendingTransition(0, 0);
            }
        }, 4000); // wait for 4 seconds
        
	        
	}
	@Override
	protected void onUserLeaveHint() {
		// TODO Auto-generated method stub
		super.onUserLeaveHint();
	}
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		//super.onBackPressed();
		MainSplashScreen.this.finish();
		System.exit(0);
	}
}

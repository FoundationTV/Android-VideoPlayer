package com.jtv.player;

import android.app.Activity;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;

public abstract class DisplayScreen extends Activity implements OnClickListener,
		OnFocusChangeListener{
	
		@Override
		public void onBackPressed() {
			// TODO Auto-generated method stub
			//super.onBackPressed();
			Log.i("inside on back 1", "inside on back ");
		}
		
		
}

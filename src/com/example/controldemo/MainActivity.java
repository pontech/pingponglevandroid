package com.example.controldemo;

import com.example.hallcontroller.R;

import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	public volatile static AccessorySerial AccessoryClass;
	public static Context usethis;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		usethis = this;
		if (AccessoryClass == null) {
			AccessoryClass = new AccessorySerial(usethis);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ConnectDisconect:
			//connect2Board();
			break;
		}
		return false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		AccessoryClass.disconnectFromAccessory();
	}

	@Override
	public void onBackPressed() { // prevent back button
		return;
	}

	@Override
	public void onResume() {
		super.onResume();
		AccessoryClass.resumeconnect();
	}

	@Override
	public void onPause() {
		//AccessoryClass.disconnectFromAccessory();
		super.onPause();
	}

	void TimerTask(int time) {
		final Handler handler = new Handler();
		final Runnable r;
		final int interval = time;
		r = new Runnable() {
			public void run() {
				if (AccessoryClass.connected) {
					// something to do periodically while connected
					handler.postDelayed(this, interval);
				}
			}
		};
		handler.postDelayed(r, interval);
	}
	
	public void onpress(View view) {
		Toast.makeText(this, AccessoryClass.sendCommand("on"), Toast.LENGTH_SHORT).show();
	}

	public void offpress(View view) {
		Toast.makeText(this, AccessoryClass.sendCommand("off"), Toast.LENGTH_SHORT).show();
	}


}

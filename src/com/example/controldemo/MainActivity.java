package com.example.controldemo;

import com.example.hallcontroller.R;

import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	public volatile static SerialClass Serial;
	
	int led = 81;
    String HIGH = " 1";
    String LOW = " 0";
    String INPUT = " 0";
    String OUTPUT = " 1";
    String OPEN = " 2";
    String True = " 1";
    String False = " 0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Serial = new SerialClass(
				(UsbManager) getSystemService(Context.USB_SERVICE), this);
		if (SerialClass.connected) {
			Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
			TimerTask(10000);
		}
		SetupHardware();
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
			connect2Board();
			break;
		}
		return false;
	}

	public void connect2Board() {
		if (SerialClass.connected) {
			Serial.connect(false);
			Toast.makeText(this, "disconnected", Toast.LENGTH_SHORT).show();
		} else {
			Serial.connect(true);
			if (SerialClass.connected) {
				Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();
				SetupHardware();
				TimerTask(10000);
			} else {
				Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	public void SetupHardware() {
		Serial.Write("pinmode " + String.valueOf(led) + OUTPUT);
	}

	public void LEDON(View view) {
		Serial.Write("digitalwrite " + String.valueOf(led) + HIGH);
	}

	public void LEDOFF(View view) {
		Serial.Write("digitalwrite " + String.valueOf(led) + LOW);
	}

	public void LEDREAD(View view) {
		Toast.makeText(this, Serial.WriteRead("digitalread " + String.valueOf(led), 250), Toast.LENGTH_SHORT).show(); 
	}

	void TimerTask(int time) {
		final Handler handler = new Handler();
		final Runnable r;
		final int interval = time;
		r = new Runnable() {
			public void run() {
				if (SerialClass.connected) {
					// something to do periodically while connected
					handler.postDelayed(this, interval);
				}
			}
		};
		handler.postDelayed(r, interval);
	}

}

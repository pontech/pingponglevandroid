package com.example.controldemo;

import java.util.ListIterator;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.hallcontroller.R;

public class MainActivity extends Activity {
	public volatile static AccessorySerial AccessoryClass;
	public static Context usethis;
	DisplayMetrics metrics;
	Integer[] xvalues = new Integer[100];
	Integer[] yvalues = new Integer[100];
	Integer[] setvalues = new Integer[100];
	Integer power = 0;
	int vectorLength;
	int axes;
	Paint paint;
	float maxx, maxy, minx, miny, locxAxis, locyAxis;

	float canvasHeight;
	float canvasWidth;
	float actualWidth;

	int[] xvaluesInPixels;
	int[] yvaluesInPixels;
	int[] setpointvaluesinpixels;
	ImgPoint on;
	ImgPoint off;
	ImgPoint setpointadd;
	ImgPoint setpointminus;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		usethis = this;
		if (AccessoryClass == null) {
			AccessoryClass = new AccessorySerial(usethis);
		}
		for (int i = 0; i<100; i++){
			xvalues[i]=i;
			yvalues[i]=0;
			setvalues[i]=0;
		}
		metrics = getBaseContext().getResources().getDisplayMetrics();
		int xlocationpixels = metrics.widthPixels - 65;
		Resources res = getResources();
		on = new ImgPoint(BitmapFactory.decodeResource(res,
				R.drawable.onbutton), xlocationpixels, 10);
		off = new ImgPoint(BitmapFactory.decodeResource(res,
				R.drawable.offbutton), xlocationpixels, 85);
		setpointadd = new ImgPoint(BitmapFactory.decodeResource(res,
				R.drawable.add_icon), 5, 10);
		setpointminus = new ImgPoint(BitmapFactory.decodeResource(res,
				R.drawable.delete_icon), 5, 85);
		Panel pan = new Panel(this);
		setContentView(pan);
		showGraph();
		TimerTask(100,pan);
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

	void TimerTask(int time, View view) {
		final Handler handler = new Handler();
		final Runnable r;
		final int interval = time;
		final View thisview = view;
		r = new Runnable() {
			public void run() {
				if (AccessoryClass.connected) {
					String res = AccessoryClass.sendCommand("readstatus");
					String[] parts = res.split(" ");
					//Toast.makeText(usethis, parts[0]+","+parts[1]+","+parts[2], Toast.LENGTH_SHORT).show();
					try{
						if(parts.length>=3) {
							for(int i = 0;i<99;i++){
								yvalues[i] = yvalues[i+1];
								setvalues[i] = setvalues[i+1];
							}
							yvalues[99] = (int) (Double.parseDouble(parts[0])*10);
							setvalues[99] = (int) (Double.parseDouble(parts[1])*10);
							power = (int) Double.parseDouble(parts[2]);
							//Toast.makeText(usethis, String.valueOf(yvalues[99])+","+String.valueOf(setvalues[99])+","+String.valueOf(power), Toast.LENGTH_SHORT).show();
							thisview.invalidate();
						}
					} catch (NumberFormatException e) {
						Toast.makeText(usethis, e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
				handler.postDelayed(this, interval);
			}
		};
		handler.postDelayed(r, interval);
	}
	
	public void showGraph() {
		vectorLength = xvalues.length;
		axes = 1;

		getAxes(xvalues, yvalues);

		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(1);
		paint.setTextSize(20);
		// Toast.makeText(this, fileName, Toast.LENGTH_SHORT).show();
		setContentView(new Panel(this));
	}

	class Panel extends View implements View.OnTouchListener {

		PointList pointList = new PointList();
		int pointID = 0;

		public Panel(Context context) {
			super(context);

			setOnTouchListener(this);
		}

		@Override
		public void onDraw(Canvas canvas) {

			// canvas.drawText(positionArray.toString(), 200, 200, paint);
			// canvas.drawText(pressureArray.toString(), 200, 230, paint);
			// canvas.drawText(tenthSecondArray.toString(), 200, 260, paint);

			canvasHeight = getHeight();
			canvasWidth = getWidth() - 75;
			actualWidth = getWidth();
			xvaluesInPixels = toPixel(canvasWidth, minx, maxx, xvalues);
			yvaluesInPixels = toPixel(canvasHeight, miny, maxy, yvalues);
			setpointvaluesinpixels = toPixel(canvasHeight, miny, maxy, setvalues);
			int locxAxisInPixels = toPixelInt(canvasHeight, miny, maxy,
					locxAxis);
			int locyAxisInPixels = toPixelInt(canvasWidth, minx, maxx, locyAxis);

			paint.setStrokeWidth(3);
			canvas.drawARGB(255, 255, 255, 255);
			canvas.drawColor(Color.BLACK);
			canvas.drawBitmap(on.img, on.X, on.Y, null);
			canvas.drawBitmap(off.img, off.X, off.Y, null);
			canvas.drawBitmap(setpointadd.img, setpointadd.X, setpointadd.Y, null);
			canvas.drawBitmap(setpointminus.img, setpointminus.X, setpointminus.Y, null);
			paint.setColor(Color.WHITE);
			canvas.drawLine(75, canvasHeight - locxAxisInPixels, canvasWidth,
					canvasHeight - locxAxisInPixels, paint);
			canvas.drawLine(locyAxisInPixels, 0, locyAxisInPixels,
					canvasHeight, paint);
			canvas.drawLine(canvasWidth, 0, canvasWidth, canvasHeight, paint);

			// Automatic axes markings, modify n to control the number of axes
			// labels
			if (axes != 0) {
				float temp = 0.0f;
				int n = 4;
				paint.setTextAlign(Paint.Align.CENTER);
				paint.setTextSize(20.0f);
				for (int i = 1; i <= n; i++) {
					temp = Math.round(10 * (minx + (i - 1) * (maxx - minx) / n)) / 10;
					canvas.drawText("" + temp / 10,
							(float) toPixelInt(canvasWidth, minx, maxx, temp),
							canvasHeight - locxAxisInPixels + 20, paint);
					temp = Math.round(10 * (miny + (i - 1) * (maxy - miny) / n)) / 10;
					canvas.drawText(
							String.valueOf((double)temp/10.0),
							locyAxisInPixels + 20,
							canvasHeight
									- (float) toPixelInt(canvasHeight, miny,
											maxy, temp), paint);
				}
				canvas.drawText("" + maxx / 10,
						(float) toPixelInt(canvasWidth, minx, maxx, maxx),
						canvasHeight - locxAxisInPixels + 20, paint);
				canvas.drawText("" + maxy, locyAxisInPixels + 20, canvasHeight
						- (float) toPixelInt(canvasHeight, miny, maxy, maxy),
						paint);
			}

			pointList = new PointList(); //setvals

			for (int i = 0; i < vectorLength; i++) {
				pointList.add(new Point((int) xvaluesInPixels[i],
						(int) setpointvaluesinpixels[i]));
			}

			ListIterator<Point> pointIterator = pointList.pointListIterator();

			Point previousPoint = null;
			Point currentPoint = null;
			paint.setStrokeWidth(2);
			paint.setColor(Color.RED);
			paint.setFlags(Paint.ANTI_ALIAS_FLAG);
			paint.setAntiAlias(true);

			int count = 0; // todo: 5 ugly find cause instead of covering it
							// (line from last point to first)
			while (pointIterator.hasNext()) {
				previousPoint = currentPoint;
				currentPoint = pointIterator.next();
				if (previousPoint != null && count < vectorLength) {

					canvas.drawLine(previousPoint.x,
							(canvasHeight - previousPoint.y), currentPoint.x,
							(canvasHeight - currentPoint.y), paint);

				}
				count++;
			}

			pointList = new PointList();

			for (int i = 0; i < vectorLength; i++) {
				pointList.add(new Point((int) xvaluesInPixels[i],
						(int) yvaluesInPixels[i]));
			}

			pointIterator = pointList.pointListIterator();

			previousPoint = null;
			currentPoint = null;
			paint.setStrokeWidth(2);
			paint.setColor(Color.BLUE);
			paint.setFlags(Paint.ANTI_ALIAS_FLAG);
			paint.setAntiAlias(true);

			count = 0; // todo: 5 ugly find cause instead of covering it
							// (line from last point to first)
			while (pointIterator.hasNext()) {
				previousPoint = currentPoint;
				currentPoint = pointIterator.next();
				if (previousPoint != null && count < vectorLength) {

					canvas.drawLine(previousPoint.x,
							(canvasHeight - previousPoint.y), currentPoint.x,
							(canvasHeight - currentPoint.y), paint);

				}
				count++;
			}
			paint.setStrokeWidth(5);
			paint.setColor(Color.YELLOW);
			int powertop = toPixelInt(canvasHeight, miny, maxy, (float) (power*0.055));
			canvas.drawLine(canvasWidth-10, canvasHeight -powertop, canvasWidth-10, canvasHeight - locxAxisInPixels, paint);
			
			invalidate();
			super.onDraw(canvas);
		}

		private void captureUp(MotionEvent event) {
			int index = event.getActionIndex();
			int id = event.getPointerId(index);
		}

		private void capturePointerMoves(MotionEvent event) {
		}

		private void captureDown(MotionEvent event) {

			int X = (int) event.getRawX();
			int Y = (int) event.getRawY() - 40;

			if (on.IsOn(X, Y)) {
				Toast.makeText(usethis, AccessoryClass.sendCommand("On"), Toast.LENGTH_SHORT).show();
			}
			if (off.IsOn(X, Y)) {
				Toast.makeText(usethis, AccessoryClass.sendCommand("off"), Toast.LENGTH_SHORT).show();
			}
			if (setpointadd.IsOn(X, Y)) {
				String res = AccessoryClass.sendCommand("readsetpoint");
				try{
					double newval = Double.parseDouble(res);
					newval = newval +.25;
					newval = newval> 5.5 ? 5.5 : newval;
					newval = newval< 2 ? 2 : newval;
					AccessoryClass.sendCommand("setsetpoint " + String.valueOf(newval));
				} catch (NumberFormatException e) {
					Toast.makeText(usethis, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
			if (setpointminus.IsOn(X, Y)) {
				String res = AccessoryClass.sendCommand("readsetpoint");
				try{
					double newval = Double.parseDouble(res);
					newval = newval -.25;
					newval = newval> 5.5 ? 5.5 : newval;
					newval = newval< 2 ? 2 : newval;
					AccessoryClass.sendCommand("setsetpoint " + String.valueOf(newval));
				} catch (NumberFormatException e) {
					Toast.makeText(usethis, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getActionMasked();
			switch (action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				captureDown(event);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				captureUp(event);
				break;
			case MotionEvent.ACTION_MOVE:
				capturePointerMoves(event);
				break;
			}
			invalidate();
			return true;
		}
	}
	private void getAxes(Integer[] xvalues, Integer[] yvalues) {

		minx = getMin(xvalues);
		miny = getMin(yvalues);
		maxx = getMax(xvalues);
		// maxy=getMax(yvalues);
		maxy = 60; // for presentation

		if (minx >= 0)
			locyAxis = minx;
		else if (minx < 0 && maxx >= 0)
			locyAxis = 0;
		else
			locyAxis = maxx;

		if (miny >= 0)
			locxAxis = miny;
		else if (miny < 0 && maxy >= 0)
			locxAxis = 0;
		else
			locxAxis = maxy;

	}
	private int[] toPixel(float pixels, float min, float max, Integer[] value) {

		double[] p = new double[value.length];
		int[] pint = new int[value.length];

		for (int i = 0; i < value.length; i++) {
			p[i] = .1 * pixels + ((value[i] - min) / (max - min)) * .8 * pixels;
			pint[i] = (int) p[i];
		}

		return (pint);
	}

	private int toPixelInt(float pixels, float min, float max, float value) {

		double p;
		int pint;
		p = .1 * pixels + ((value - min) / (max - min)) * .8 * pixels;
		pint = (int) p;
		return (pint);
	}

	private float getMax(Integer[] v) {
		float largest = v[0];
		for (int i = 0; i < v.length; i++)
			if (v[i] > largest)
				largest = v[i];
		return largest;
	}

	private float getMin(Integer[] v) {
		float smallest = v[0];
		for (int i = 0; i < v.length; i++)
			if (v[i] < smallest)
				smallest = v[i];
		return smallest;
	}
	public class ImgPoint {
		public int X = 0;
		public int Y = 0;
		public Bitmap img;

		ImgPoint(Bitmap imgin, int xin, int yin) {
			img = imgin;
			X = xin;
			Y = yin;
		}

		public boolean IsOn(int Xin, int Yin) {
			int xc = X + (img.getWidth() / 2); // center of image
			int yc = Y + (img.getHeight() / 2);
			double dist = Math.sqrt((double) Math.pow(xc - Xin, 2)
					+ Math.pow(yc - Yin, 2));
			if (dist < (img.getWidth() / 2)) {
				return true;
			} else {
				return false;
			}
		}
	}

}

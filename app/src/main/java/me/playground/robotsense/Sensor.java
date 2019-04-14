package me.playground.robotsense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.util.Log;

public class Sensor extends SurfaceView implements SurfaceHolder.Callback {
	private String TAG = "Sensor";
	private float centerX;
	private float centerY;
	private float radius;
	private final int quarterPiSteps = 15;
	private final int range = quarterPiSteps;
	private final int intStep = 3;
	private final int N = range / intStep;	// N points in two directions
	private final double Pi = 3.14159265359;
	private double maxAngle = range * Pi / (4 * quarterPiSteps);
	private GameLoop gameLoop;
	private Paint paint;
	private float width;
	private float height;
	private float[] posX;
	private float[] posY;
	private Path path;
	private NetLink net = null;
	private ControlResponse ctrlResp;
	private int index = 0;
	private double sonar_angle, chassis_angle, power;
	private static final int limit = 15;
	private static final int floatToInt = 100000000;

	public Sensor(Context context) {
		this(context, null);
	}

	public Sensor(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().setFormat(PixelFormat.TRANSLUCENT);
		getHolder().addCallback(this);
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(2);
		posX = new float[N * 2 + 1];
		posY = new float[N * 2 + 1];
		ctrlResp = new ControlResponse(-limit, 0, 1, -1, 0);
	}

	public void update(ControlResponse cr) {
		/* distance is in umeters from 0 to ~2m */
		int signedStep = cr.sonarAngle < 0 ? -intStep : intStep;
		double distance = radius * cr.distance / 2000000;
		double radians = (double)cr.sonarAngle / floatToInt;

		index = ((int)(radians * 4 * range / Pi) + signedStep / 2) / intStep + N;
		Log.i(TAG, "update() @ " + index);

		posX[index] = centerX + (float)(distance * Math.sin(radians));
		posY[index] = centerY - (float)(distance * Math.cos(radians));
		Log.i(TAG, "dist " + distance + ", angle " + radians +
		      " point: " + posX[index] + ":" + posY[index]);

		path.reset();
		for (int i = 0; i < 2 * N + 1; i++) {
			if (i == 0)
				path.moveTo(posX[i], posY[i]);
			else
				path.lineTo(posX[i], posY[i]);
		}
	}

	@Override
	public void draw(Canvas canvas) {
		if (canvas == null) return;

		net.getCurrent(ctrlResp);
		update(ctrlResp);

		canvas.drawColor(Color.BLACK);
		paint.setColor(Color.BLUE);
		canvas.drawPath(path, paint);

		paint.setColor(Color.RED);
		canvas.drawCircle(posX[index], posY[index], 4, paint);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) /*throws Exception */{
		gameLoop = new GameLoop(this);
		gameLoop.setRunning(true);

		if (net == null) {
			net = new NetLink();

			new Thread(net).start();
		}

		gameLoop.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		this.width = width;
		this.height = height;
		double step = maxAngle / N;
		double angle;

		path = new Path();

		Log.i(TAG, "Surface " + width + "x" + height);

		centerX = width / 2;
		centerY = 3 * height / 4;

		radius = Math.min(width, height) / 2;

		angle = -maxAngle;
		for (int i = 0; i < 2 * N + 1; i++) {
			posX[i] = centerX + radius * (float)Math.sin(angle);
			posY[i] = centerY - radius * (float)Math.cos(angle);
			if (i == 0)
				path.moveTo(posX[i], posY[i]);
			else
				path.lineTo(posX[i], posY[i]);
			angle += step;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		gameLoop.setRunning(false);
		gameLoop = null;
	}

	public void move(double angle, double power) {
		Log.i(TAG, "angle " + angle + " power " + power);
		// angle is changing from -pi to +pi as a clockwise angle from the left
		// 0 <= power <= 100

		ctrlResp.sonarAngle = (int)(this.sonar_angle * floatToInt);
		ctrlResp.turn = (int)(angle * floatToInt);
		ctrlResp.advance = (int)power;

		net.update(ctrlResp);

		this.chassis_angle = angle;
		this.power = power;
	}

	public void rotate(double angle) {
		Log.i(TAG, "angle " + angle);
		// angle is changing from -pi to +pi as a clockwise angle from the left

		if (angle < 0)
			return;

		/* Map the angle to -Pi/4 <= angle <= Pi/4 */
		angle = (angle - Pi / 2) / 2.;

		ctrlResp.sonarAngle = (int)(angle * floatToInt);
		ctrlResp.turn = (int)(this.chassis_angle * floatToInt);
		ctrlResp.advance = (int)this.power;

		net.update(ctrlResp);

		this.sonar_angle = angle;
	}
}

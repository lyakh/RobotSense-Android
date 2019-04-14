package me.playground.robotsense;

import java.lang.Thread;

import android.graphics.Canvas;
import android.view.SurfaceView;

import android.util.Log;

import android.os.Handler;
import android.os.Message;

/* Based on work of edgarramirez */
public class GameLoop extends Thread {
    private String TAG = "GameLoop";
    private static final long FPS = 24;
    private static final long ticksPS = 500 / FPS;
    private Sensor sensor;
    private SurfaceView view;
    private boolean running = false;
    private long startTime;
    private long sleepTime;
    private ControlResponse ctrlResp;

    public GameLoop(Sensor sns) {
        sensor = sns;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    @Override
    public void run() {
        Canvas canvas = null;
	int step = 3;

	while (running)
	{
            startTime = System.currentTimeMillis();

            try {
                canvas = sensor.getHolder().lockCanvas();
                synchronized (sensor.getHolder()) {
                    sensor.draw(canvas);
                }
            } finally {
                if (canvas != null) {
                    sensor.getHolder().unlockCanvasAndPost(canvas);
                }
            }

            sleepTime = ticksPS - (System.currentTimeMillis() - startTime);
            try {
                if (sleepTime > 0)
                    sleep(sleepTime);
                else
                    sleep(10);
            } catch (Exception ignore) {}
        }
    }
}

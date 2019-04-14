package me.playground.robotsense;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import com.erz.joysticklibrary.JoyStick;

/* Based on work of edgarramirez */
public class MainActivity extends Activity implements JoyStick.JoyStickListener {
    private String TAG = "RSense";
    private Sensor sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joysticknav);

        sensor = (Sensor) findViewById(R.id.sensor);

        JoyStick joy1 = (JoyStick) findViewById(R.id.joy1);
        joy1.setListener(this);
        joy1.setPadColor(Color.parseColor("#556fff6f"));
        joy1.setButtonColor(Color.parseColor("#55ff0000"));

        JoyStick joy2 = (JoyStick) findViewById(R.id.joy2);
        joy2.setListener(this);
        joy2.enableStayPut(true);
        joy2.setPadColor(Color.parseColor("#556fff6f"));
        joy2.setButtonColor(Color.parseColor("#55ff0000"));
    }

    @Override
    public void onMove(JoyStick joyStick, double angle, double power, int direction) {
        switch (joyStick.getId()) {
            case R.id.joy1:
                sensor.move(angle, power);
                break;
            case R.id.joy2:
                sensor.rotate(angle);
                break;
        }
    }

    @Override
    public void onTap() {}

    @Override
    public void onDoubleTap() {}
}

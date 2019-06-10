package me.playground.robotsense;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import me.playground.robotsense.R;

import com.erz.joysticklibrary.JoyStick;

/* Based on work of edgarramirez */
public class MainActivity extends AppCompatActivity implements JoyStick.JoyStickListener {
    private String TAG = "RSense";
    private Sensor sensor;
    private String ipAddress;
    private int port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joysticknav);

        PreferenceManager.setDefaultValues(this, R.xml.pref_network, false);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        ipAddress = pref.getString("ip_address", null);
        port = Integer.parseInt(pref.getString("port_number", null));

        sensor = (Sensor) findViewById(R.id.sensor);
        sensor.configure(ipAddress, port);

        JoyStick joy1 = (JoyStick) findViewById(R.id.joy1);
        joy1.setListener(this);
        joy1.setPadColor(Color.parseColor("#556fff6f"));
        joy1.setButtonColor(Color.parseColor("#55ff0000"));

        JoyStick joy2 = (JoyStick) findViewById(R.id.joy2);
        joy2.setListener(this);
        joy2.enableStayPut(true);
        joy2.setPadColor(Color.parseColor("#556fff6f"));
        joy2.setButtonColor(Color.parseColor("#55ff0000"));

        Toolbar tb = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(tb);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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

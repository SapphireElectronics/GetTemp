package ca.sapphire.gettemp;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;


public class WaveViewActivity extends AppCompatActivity {

    private int bufSize = 32768;
    private short[] buffer = new short[bufSize];
    private boolean mode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave_view);

        Bundle extras;
        if (savedInstanceState == null) {
            extras = getIntent().getExtras();
            if(extras == null) {
                buffer = null;
            } else {
                buffer = extras.getShortArray("wave");
                mode = extras.getBoolean("mode");
            }
        } else {
            buffer = (short[]) savedInstanceState.getSerializable("wave");
            mode = (boolean) savedInstanceState.getSerializable("mode");
        }

        WaveView waveView = new WaveView( this, buffer, 4096, 400, mode );
        setContentView(waveView);
        waveView.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wave_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

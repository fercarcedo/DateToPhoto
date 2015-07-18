package fergaral.datetophoto.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.view.MenuItem;
import android.view.Window;

import fergaral.datetophoto.R;
import fergaral.datetophoto.fragments.AboutFragment;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setEnterTransition(new Fade());
            getWindow().setExitTransition(new Fade());
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new AboutFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case android.R.id.home: {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    finishAfterTransition();
                else
                    finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}

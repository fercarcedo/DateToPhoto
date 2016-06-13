package fergaral.datetophoto.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.view.MenuItem;
import android.view.Window;

import fergaral.datetophoto.R;
import fergaral.datetophoto.fragments.SettingsFragment;

public class SettingsActivity extends PermissionActivity {

    public static boolean SHOULD_REFRESH = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setEnterTransition(new Fade());
            getWindow().setExitTransition(new Fade());
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        SettingsActivity.SHOULD_REFRESH = false;

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction().replace(R.id.main_settings_content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        /*if(!PhotosActivity.IS_PROCESSING && PhotosActivity.SHOULD_REFRESH_GRID) {
            Intent intent = new Intent(this, PhotosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }*/

        PhotosActivity.SHOULD_REFRESH_GRID = SettingsActivity.SHOULD_REFRESH;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        }else{
            finish();
        }
    }
}

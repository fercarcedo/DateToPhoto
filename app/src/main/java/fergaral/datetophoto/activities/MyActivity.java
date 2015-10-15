package fergaral.datetophoto.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;

import fergaral.datetophoto.R;
import fergaral.datetophoto.adapters.ViewPagerAdapter;
import fergaral.datetophoto.fragments.MainFragment;
import fergaral.datetophoto.utils.SlidingTabLayout;

/**
 * Created by Parejúa on 08/07/2014.
 */
public class MyActivity extends AppCompatActivity {

    private static final int NOTIFICATION_ID = 1;

    public static final String INTENT_ACTION = "fergaral.datetophoto.CANCEL_DIALOG_ACTION";
    public static final String INTENT_QUERY_ACTION = "fergaral.datetophoto.QUERY_SERVICE_ACTION";
    public static final String INTENT_RECEIVE_ACTION = "fergaral.datetophoto.RECEIVE_SERVICE_ACTION";

    public static final int READ_REQUEST_CODE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 2;
    private Uri[] uris;
    private Button btn1, btn2, btn3, btn4, btn5;
    private ImageView iv1;
//    private PhotosObserver instUploadObserver = new PhotosObserver();
    private String saved;
    private MaterialDialog dialog;
    private MediaScannerConnection msConn;
    private boolean dialogCancelled;

    private Toolbar toolbar;
    private ViewPager pager;
    private ViewPagerAdapter adapter;
    private SlidingTabLayout tabs;
    private CharSequence[] titles = {"Inicio", "Seleccionar fotos"};
    private int numberOfTabs = 2;

    protected void onCreate(Bundle savedInstanceState) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setEnterTransition(new Fade());
            getWindow().setExitTransition(new Fade());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);

        setSupportActionBar(toolbar);

        getSupportActionBar().hide();

        if(savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }
       /* // Creating The ViewPagerAdapter and Passing Fragment Manager, Titles fot the Tabs and Number Of Tabs.
        adapter =  new ViewPagerAdapter(getSupportFragmentManager(), titles, numberOfTabs);

        // Assigning ViewPager View and setting the adapter
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);

        // Assiging the Sliding Tab Layout View
        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true); // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width

        // Setting Custom Color for the Scroll bar indicator of the Tab View
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.tabsScrollColor);
            }
        });

        // Setting the ViewPager For the SlidingTabsLayout
        tabs.setViewPager(pager);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings : {
                startActivityCompat(new Intent(this, SettingsActivity.class));
                return true;
            }

            case R.id.action_about: {
                startActivityCompat(new Intent(this, AboutActivity.class));
                return true;
            }

            case R.id.action_diskspaceusage: {
                startActivityCompat(new Intent(this, DiskSpaceUsageActivity.class));
                return true;
            }

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

    private void startActivityCompat(Intent intent)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }
        else
        {
            startActivity(intent);
        }
    }
}

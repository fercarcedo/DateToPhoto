package fergaral.datetophoto.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.ArrayList;

import fergaral.datetophoto.R;
import fergaral.datetophoto.fragments.LoadPhotosFragment;
import fergaral.datetophoto.fragments.ProgressHeadlessFragment;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.receivers.ActionCancelReceiver;
import fergaral.datetophoto.utils.ProgressCircle;
import fergaral.datetophoto.utils.ProgressListener;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 20/09/15.
 */
public class ProgressActivity extends AppCompatActivity implements ProgressListener {

    private static final String TOTAL_KEY = "total";
    private static final String PROGRESS_KEY = "progress";
    private static final String ACTUAL_KEY = "actual";
    public static final String SEARCH_PHOTOS_KEY = "search_photos";
    private static final String PROGRESS_FRAGMENT_KEY = "progressFragment";
    public static final String SELECTED_PATHS_KEY = "selected_paths";
    private DonutProgress progressCircle;
    private TextView titleTv, progTv;
    private TextView cancelBtn;
    private ProgressHeadlessFragment progressHeadlessFragment;
    private int total, actual, progress;
    private boolean searchPhotos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        progressCircle = (DonutProgress) findViewById(R.id.progress_donut);
        titleTv = (TextView) findViewById(R.id.progress_title_tv);
        progTv = (TextView) findViewById(R.id.progress_circle_progtv);
        cancelBtn = (TextView) findViewById(R.id.cancel_btn);

        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(ProgressActivity.TOTAL_KEY))
                total = savedInstanceState.getInt(ProgressActivity.TOTAL_KEY);

            if(savedInstanceState.containsKey(ProgressActivity.PROGRESS_KEY))
                progress = savedInstanceState.getInt(ProgressActivity.PROGRESS_KEY);

            if(savedInstanceState.containsKey(ProgressActivity.ACTUAL_KEY))
                actual = savedInstanceState.getInt(ProgressActivity.ACTUAL_KEY);

            searchPhotos = savedInstanceState.getBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, false);
        }

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titleTv.setText("Cancelando...");
                sendBroadcast(new Intent(ProgressActivity.this, ActionCancelReceiver.class));
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        ArrayList<String> selectedPaths = intent.getStringArrayListExtra(ProgressActivity.SELECTED_PATHS_KEY);

        //Iniciamos la AsyncTask (en el Headless Fragment)
        progressHeadlessFragment = (ProgressHeadlessFragment) getSupportFragmentManager()
                .findFragmentByTag(ProgressActivity.PROGRESS_FRAGMENT_KEY);

        if(progressHeadlessFragment == null) {
            //Todavía no hemos creado el Fragment. Lo creamos e iniciamos la AsyncTask
            progressHeadlessFragment = new ProgressHeadlessFragment();
            Bundle arguments = new Bundle();
            arguments.putStringArrayList(ProgressHeadlessFragment.SELECTED_PATHS_KEY, selectedPaths);
            arguments.putBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, searchPhotos);
            progressHeadlessFragment.setArguments(arguments);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(progressHeadlessFragment, PROGRESS_FRAGMENT_KEY)
                    .commit();
        }
    }

    @Override
    public void reportTotal(int total) {
        showProgress(searchPhotos ? "Buscando fotos ya fechadas..." : null);
    }

    @Override
    public void onProgressChanged(int progress, int actual) {
        progressCircle.setProgress(progress);
        int total = (actual * 100) / progress;
        progTv.setText(actual + "/" + total);
    }

    @Override
    public void reportEnd(boolean fromActionShare) {
        hideProgress();
    }

    private void showProgress(String title) {
        //Establecemos el total de fotos a fechar
        if(title != null)
            titleTv.setText(title);

        progressCircle.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressCircle.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(TOTAL_KEY, total);
        outState.putInt(PROGRESS_KEY, progress);
        outState.putInt(ACTUAL_KEY, actual);
    }
}

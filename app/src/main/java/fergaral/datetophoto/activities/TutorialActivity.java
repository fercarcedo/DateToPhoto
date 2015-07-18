package fergaral.datetophoto.activities;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import fergaral.datetophoto.R;
import fergaral.datetophoto.fragments.TutorialFragment;
import fergaral.datetophoto.fragments.TutorialFragment1;
import fergaral.datetophoto.fragments.TutorialFragment2;
import fergaral.datetophoto.fragments.TutorialFragment3;
import fergaral.datetophoto.fragments.TutorialFragment4;

public class TutorialActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 5;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setEnterTransition(new Slide());
            getWindow().setExitTransition(new Slide());
            getWindow().setStatusBarColor(getResources().getColor(R.color.tutorialBackground));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.tutorialBackground));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        mViewPager = (ViewPager) findViewById(R.id.tutorial_viewpager);
        mPagerAdapter = new TutorialPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);


        final TextView nextBtn = (TextView) findViewById(R.id.tutorial_next_btn);
        TextView skipBtn = (TextView) findViewById(R.id.tutorial_skip_btn);

        nextBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mViewPager.getCurrentItem() != NUM_PAGES -1)
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                else
                    startActivityCompat(new Intent(TutorialActivity.this, PhotosActivity.class));
            }
        });

        skipBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivityCompat(new Intent(TutorialActivity.this, PhotosActivity.class));
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                /*if(position == 5) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TutorialActivity.this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("firstuse", false);
                    editor.apply();

                    Intent intent = new Intent(TutorialActivity.this, PhotosActivity.class);
                    intent.putExtra("fromfirstuse", true);
                    startActivityCompat(intent);
                }*/

                if(position == NUM_PAGES - 1)
                    nextBtn.setText("FINALIZAR");
                else if(nextBtn.getText().toString().equals("FINALIZAR"))
                    nextBtn.setText("SIGUIENTE");
            }

            @Override
            public void onPageSelected(int position) {
                if(position == NUM_PAGES - 1)
                    nextBtn.setText("FINALIZAR");
                else if(nextBtn.getText().toString().equals("FINALIZAR"))
                    nextBtn.setText("SIGUIENTE");
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    private void startActivityCompat(Intent intent) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }else{
            startActivity(intent);
        }
    }

    private class TutorialPagerAdapter extends FragmentStatePagerAdapter {

        public TutorialPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0 : default: return new TutorialFragment();
                case 1: return new TutorialFragment1();
                case 2: return new TutorialFragment2();
                case 3: return new TutorialFragment3();
                case 4: return new TutorialFragment4();
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}

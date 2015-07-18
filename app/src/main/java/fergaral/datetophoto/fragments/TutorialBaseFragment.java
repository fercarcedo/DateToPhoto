package fergaral.datetophoto.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.PhotosActivity;

public class TutorialBaseFragment extends Fragment {

    private static final int NUM_PAGES = 5;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_tutorial, container, false);

        mViewPager = (ViewPager) rootView.findViewById(R.id.tutorial_viewpager);
        mPagerAdapter = new TutorialPagerAdapter(getActivity().getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);


        final TextView nextBtn = (TextView) rootView.findViewById(R.id.tutorial_next_btn);
        TextView skipBtn = (TextView) rootView.findViewById(R.id.tutorial_skip_btn);

        nextBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mViewPager.getCurrentItem() != NUM_PAGES -1)
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                else {
                    startActivity(new Intent(getActivity(), PhotosActivity.class));
                }
            }
        });

        skipBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), PhotosActivity.class));
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 4) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("firstuse", false);
                    editor.apply();

                    startActivity(new Intent(getActivity(), PhotosActivity.class));
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 4) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("firstuse", false);
                    editor.apply();

                    startActivity(new Intent(getActivity(), PhotosActivity.class));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        return rootView;
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
                case 4: return new TutorialFragment();
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}

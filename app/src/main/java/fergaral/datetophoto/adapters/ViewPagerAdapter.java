package fergaral.datetophoto.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import fergaral.datetophoto.fragments.MainFragment;

/**
 * Created by Edwin on 15/02/2015.
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    CharSequence titles[]; // This will Store the Titles of the Tabs which are Going to be passed when ViewPagerAdapter is created
    int numberOfTabs; // Store the number of tabs, this will also be passed when the ViewPagerAdapter is created


    // Build a Constructor and assign the passed Values to appropriate values in the class
    public ViewPagerAdapter(FragmentManager fm,CharSequence titles[], int numberOfTabs) {
        super(fm);

        this.titles = titles;
        this.numberOfTabs = numberOfTabs;
    }

    //This method return the fragment for the every position in the View Pager
    @Override
    public Fragment getItem(int position) {

        if(position == 0) // if the position is 0 we are returning the First tab
        {
            return new MainFragment();
        }
        else             // As we are having 2 tabs if the position is now 0 it must be 1 so we are returning second tab
        {
            return new MainFragment();
        }

    }

    // This method return the titles for the Tabs in the Tab Strip

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position].toString().toUpperCase();
    }

    // This method return the Number of tabs for the tabs Strip

    @Override
    public int getCount() {
        return numberOfTabs;
    }
}

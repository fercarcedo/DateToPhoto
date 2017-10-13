package fergaral.datetophoto.fragments;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import fergaral.datetophoto.R;
import fergaral.datetophoto.utils.FoldersListPreference;
import fergaral.datetophoto.utils.PhotoUtils;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        FoldersListPreference listPreference = (FoldersListPreference) findPreference(getString(R.string.pref_folderstoprocess_key));
        ArrayList<String> folderNames = PhotoUtils.getFolders(getActivity());
        String[] entries = folderNames.toArray(new String[folderNames.size()]);

        Arrays.sort(entries, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.toLowerCase().compareTo(rhs.toLowerCase());
            }
        });

        if(entries.length > 0) {
            listPreference.setEntries(entries);
            listPreference.setEntryValues(entries);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean firstUse = sharedPreferences.getBoolean("firstuse", true);

        if(firstUse)
        {
            listPreference.selectAll();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstuse", false);
            editor.apply();
        }
    }
}
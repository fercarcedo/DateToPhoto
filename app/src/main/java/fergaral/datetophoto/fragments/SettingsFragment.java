package fergaral.datetophoto.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.utils.PhotoUtils;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        MultiSelectListPreference listPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_folderstoprocess_key));
        ArrayList<String> folderNames = PhotoUtils.getFolders(getActivity());
        String[] entries = folderNames.toArray(new String[folderNames.size()]);

        Arrays.sort(entries, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.toLowerCase().compareTo(rhs.toLowerCase());
            }
        });


        selectAllPhotosIfFirstUse(folderNames);

        listPreference.setEntries(entries);
        listPreference.setEntryValues(entries);
    }

    private void selectAllPhotosIfFirstUse(List<String> folderNames) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (prefs.getStringSet(getString(R.string.pref_folderstoprocess_key), null) == null) {
            // First use, no folders. Select all
            prefs.edit()
                    .putStringSet(getString(R.string.pref_folderstoprocess_key), new HashSet<>(folderNames))
                    .apply();
        }
    }
}
package fergaral.datetophoto.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.util.ArrayList;

import fergaral.datetophoto.R;
import fergaral.datetophoto.utils.FoldersListPreference;
import fergaral.datetophoto.utils.PhotoUtils;

/**
 * Created by fer on 16/06/15.
 */
public class TutorialFragment1 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tutorial1, container, false);

        final CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.tutorial_charging_enable_checkbox);
        checkBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = prefs.edit();

                editor.putBoolean(getString(R.string.pref_active_key), checkBox.isChecked());
                editor.apply();
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        checkBox.setChecked(prefs.getBoolean(getString(R.string.pref_active_key), true));

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        selectAllFoldersIfFirstUse(PhotoUtils.getFolders(getActivity()));
    }

    private void selectAllFolders(ArrayList<String> folderNames) {
        StringBuilder stringBuilder = new StringBuilder();

        for(String folderName : folderNames) {
            stringBuilder.append(folderName).append(FoldersListPreference.SEPARATOR);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.pref_folderstoprocess_key), stringBuilder.toString());
        editor.apply();
    }

    private void selectAllFoldersIfFirstUse(ArrayList<String> folderNames) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean firstUse = prefs.getBoolean("firstuse", true);

        if(firstUse) {
            selectAllFolders(folderNames);
        }
    }
}

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

import fergaral.datetophoto.R;

/**
 * Created by fer on 14/06/15.
 */
public class TutorialFragment2 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tutorial2, container, false);

        final CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.tutorial_overwrite_enable_checkbox);

        checkBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(getString(R.string.pref_overwrite_key), checkBox.isChecked());
                editor.apply();
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        checkBox.setChecked(prefs.getBoolean(getString(R.string.pref_overwrite_key), true));

        return rootView;
    }
}

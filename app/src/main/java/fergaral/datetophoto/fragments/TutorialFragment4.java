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
 * Created by fer on 4/07/15.
 */
public class TutorialFragment4 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tutorial4, container, false);
        CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.tutorial_keeplargephoto_checkbox);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        checkBox.setChecked(prefs.getBoolean(getString(R.string.pref_keeplargephoto_key), true));

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBx = (CheckBox) v;

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(getString(R.string.pref_keeplargephoto_key), checkBx.isChecked());
                editor.apply();
            }
        });

        return rootView;
    }
}

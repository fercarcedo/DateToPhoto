package fergaral.datetophoto.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fergaral.datetophoto.R;

/**
 * Created by Parejúa on 21/10/2014.
 */
public class AboutFragment extends Fragment {

    public AboutFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.my_toolbar);
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        appCompatActivity.setSupportActionBar(toolbar);

        if(appCompatActivity.getSupportActionBar() != null)
            appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        return rootView;
    }
}

package fergaral.datetophoto.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import fergaral.datetophoto.activities.PhotosActivity;
import fergaral.datetophoto.activities.ProgressActivity;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.utils.ProgressListener;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 21/09/15.
 */
public class ProgressHeadlessFragment extends Fragment implements ProgressChangedListener {

    public static final String SELECTED_PATHS_KEY = "selectedPaths";
    private ProgressListener mListener;
    private ArrayList<String> selectedPaths;
    private int total;
    private boolean searchPhotos;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //La Activity debe implementar ProgressChangedListener
        mListener = (ProgressListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Mantenemos la instancia del Fragment durante cambios de configuración
        setRetainInstance(true);

        Bundle arguments = getArguments();
        searchPhotos = arguments.getBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, false);

        if(arguments.containsKey(ProgressHeadlessFragment.SELECTED_PATHS_KEY))
            selectedPaths = arguments.getStringArrayList(ProgressHeadlessFragment.SELECTED_PATHS_KEY);

        if(!searchPhotos)
            Utils.startProcessPhotosService(getActivity(), this, selectedPaths);
        else
            Utils.searchForAlreadyProcessedPhotos(getActivity(), this);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        //Asignamos a la callback null, para evitar que el GC no
        //pueda recolectar la Activity en desuso (p.ej., durate una rotación)
        mListener = null;
    }

    @Override
    public void reportTotal(int total) {
        if(mListener != null)
            mListener.reportTotal(total);

        this.total = total;
    }

    @Override
    public void onProgressChanged(int actual) {
        double totalDouble = (double) total;
        double progress = (actual / totalDouble) * 100;

        if(mListener != null)
            mListener.onProgressChanged((int) progress, actual);
    }

    @Override
    public void reportEnd(boolean fromActionShare) {
        if(mListener != null)
            mListener.reportEnd(fromActionShare);
    }
}

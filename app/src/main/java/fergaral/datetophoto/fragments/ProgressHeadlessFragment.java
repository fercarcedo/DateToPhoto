package fergaral.datetophoto.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

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
    private boolean searchPhotos, shareAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Mantenemos la instancia del Fragment durante cambios de configuración
        setRetainInstance(true);

        Bundle arguments = getArguments();
        searchPhotos = arguments.getBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, false);

        if(arguments.containsKey(ProgressActivity.SELECTED_PATHS_KEY))
            selectedPaths = arguments.getStringArrayList(ProgressActivity.SELECTED_PATHS_KEY);

        if(arguments.containsKey(PhotosActivity.ACTION_SHARE_KEY))
            shareAction = arguments.getBoolean(PhotosActivity.ACTION_SHARE_KEY, false);

        Log.d("TAG", "containsSearch: " + arguments.containsKey(ProgressActivity.SEARCH_PHOTOS_KEY));
        Log.d("TAG", "containsSelectedPaths: " + arguments.containsKey(ProgressActivity.SELECTED_PATHS_KEY));
        Log.d("TAG", "selectedPaths!=null" + (selectedPaths != null));

        if(!searchPhotos) {
            if(!shareAction)
                Utils.startProcessPhotosService(getActivity(), this, selectedPaths);
            else
                Utils.startProcessPhotosURIService(getActivity(), this, selectedPaths);
        }else
            Utils.searchForAlreadyProcessedPhotos(getActivity(), this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        if(getTargetFragment() instanceof ProgressListener)
            mListener = (ProgressListener)getTargetFragment();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
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

        getFragmentManager()
                .beginTransaction()
                .remove(this)
                .commit();
    }
}
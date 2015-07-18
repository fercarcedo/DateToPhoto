package fergaral.datetophoto.fragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by fer on 11/06/15.
 */
public class MaterialProgressDialogFragment extends DialogFragment {

    private static final String TOTAL_KEY = "total";

    public static final String PROGRESS_KEY = "progress";
    public static final String PROGRESS_ACTION_QUERY = "fergaral.datetophoto.progress.get";
    public static final String PROGRESS_ACTION_SEND = "fergaral.datetophoto.progress.notify";

    private int currentProgress;

    public static MaterialProgressDialogFragment newInstance(int total) {
        Bundle arguments = new Bundle();
        arguments.putInt(TOTAL_KEY, total);
        MaterialProgressDialogFragment dialogFragment = new MaterialProgressDialogFragment();
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        currentProgress = getArguments().getInt(PROGRESS_KEY, 0);

        Log.d("PROGRESS", String.valueOf(currentProgress));

        Log.d("PROGRESS", "onCreateDialog");

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("Progreso")
                .content("Procesando fotos...")
                .progress(false, getTotal(), true)
                .showListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        //Recibimos el progreso del Service
                        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                int progress = intent.getIntExtra(PROGRESS_KEY, currentProgress);

                                if(getCurrentProgress() != -1)
                                    incrementProgress(progress - getCurrentProgress());
                                else
                                    incrementProgress(progress);

                                currentProgress = progress;
                            }
                        }, new IntentFilter(PROGRESS_ACTION_SEND));

                        //Pedimos al Service que nos diga el progreso actual
                        Intent intent = new Intent(PROGRESS_ACTION_QUERY);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                })
                .build();

        //dialog.incrementProgress(currentProgress);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("PROGRESS", "onCreate");
    }

    @Override
    public void onPause() {
        super.onPause();
        getArguments().putInt(PROGRESS_KEY, currentProgress);
    }

    public void incrementProgress(int by) {
        if(getDialog() != null) {
            ((MaterialDialog) getDialog()).incrementProgress(by);
            currentProgress = currentProgress + by;
        }
    }

    public int getCurrentProgress() {
        if(getDialog() != null)
            return ((MaterialDialog)getDialog()).getCurrentProgress();

        return -1;
    }

    public int getTotal() {
        return getArguments().getInt(TOTAL_KEY);
    }
}

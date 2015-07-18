package fergaral.datetophoto.utils;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.gc.materialdesign.views.ProgressBarDeterminate;

import fergaral.datetophoto.R;

/**
 * Created by fer on 17/05/15.
 */
public class AppCompatProgressDialog extends AlertDialog {
    public AppCompatProgressDialog(Context context) {
        super(context);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);

        setView(dialogView);

        setProgress(0);
    }

    public void setProgress(int progress) {
        ProgressBarDeterminate progressBar = (ProgressBarDeterminate) findViewById(R.id.progress);
        TextView progressPercent = (TextView) findViewById(R.id.progress_percent);
        TextView progressNumber = (TextView) findViewById(R.id.progress_number);

        progressBar.setProgress(progress);
        progressPercent.setText(Math.round(progress) + "%");
        progressNumber.setText(Math.round(progress) + "/" + 100);
    }
}

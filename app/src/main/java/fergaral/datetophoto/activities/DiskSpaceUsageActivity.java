package fergaral.datetophoto.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.view.MenuItem;
import android.view.Window;

import java.io.File;
import java.util.ArrayList;

import fergaral.datetophoto.R;
import fergaral.datetophoto.fragments.DiskSpaceUsageFragment;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.Utils;

public class DiskSpaceUsageActivity extends AppCompatActivity {

    private ProgressDialog dialog;
    private Context context = this;
    private boolean dialogCancelled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setEnterTransition(new Fade());
            getWindow().setExitTransition(new Fade());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disk_space_usage);

        dialog = new ProgressDialog(this);
        dialog.setTitle("Progreso");
        dialog.setMessage("Eliminando fotos con fecha...");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialogCancelled = true;
            }
        });
        //dialog.setIndeterminate(false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DiskSpaceUsageFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case android.R.id.home: {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    finishAfterTransition();
                else
                    finish();

                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public class DeleteImagesTask extends AsyncTask<ArrayList, Float, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setProgress(0);
            dialog.setMax(100);
            dialog.show();
        }

        @Override
        protected Void doInBackground(ArrayList... arrayLists) {

            ArrayList<String> galleryImages = arrayLists[0];

            if(galleryImages != null) {
                int total = 0, actual = 0;
                total = Utils.getNumberOfPhotosWithDate(galleryImages);
                dialog.setMax(total);

                for(String s : galleryImages)
                {
                    File file = new File(s);
                    String filePath = file.getAbsolutePath();

                    if(dialogCancelled)
                    {
                        dialogCancelled = false;
                        return null;
                    }

                    if(file.getName().contains("dtp-"))
                    {
                        PhotoUtils.deletePhoto(context, s);
                        actual++;
                        publishProgress((float) actual);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            super.onProgressUpdate(values);
            int p = Math.round(values[0]);
            dialog.setProgress(p);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
        }
    }

    public void deleteImagesWithDate()
    {
        new DeleteImagesTask().execute(new PhotoUtils(this).getCameraImages());
    }
}

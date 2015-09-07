package fergaral.datetophoto.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.Utils;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;

public class DiskSpaceUsageActivity extends AppCompatActivity {

    private ProgressDialog dialog;
    private boolean dialogCancelled, hasExecuted;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);

        if(toolbar != null)
            setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

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

        loadDiskSpaceUsage();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmación");
        builder.setMessage("¿Estás seguro de que quieres borrar todas las fotos con fecha de tu dispositivo? Esta acción no se puede deshacer");
        builder.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                deleteImagesWithDate();
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog alertDialog = builder.create();

        Button btn1 = (Button) findViewById(R.id.btndeletephotoswithdate);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.show();
            }
        });

        //dialog.setIndeterminate(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public class DeleteImagesTask extends AsyncTask<ArrayList, Float, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            hasExecuted = true;
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
                        PhotoUtils.deletePhoto(DiskSpaceUsageActivity.this, s);
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
            loadDiskSpaceUsage();
        }
    }

    public void deleteImagesWithDate()
    {
        new DeleteImagesTask().execute(new PhotoUtils(this).getCameraImages());
    }

    public void loadDiskSpaceUsage() {
        TextView tvphotoswithoutdate = (TextView) findViewById(R.id.tvphotoswithoutdateusage);
        TextView tvphotoswithdate = (TextView) findViewById(R.id.tvphotoswithdateusage);

        int[] diskspaceusage = Utils.getDiskSpaceUsage(this);
        tvphotoswithoutdate.setText("Photos without date: " + diskspaceusage[1] + " MB");
        tvphotoswithdate.setText("Photos with date: " + diskspaceusage[0] + " MB");

        PieChartView piechart = (PieChartView) findViewById(R.id.piechart);
        piechart.setChartRotationEnabled(false);

        PieChartData data = new PieChartData();

        List<SliceValue> sliceValues = new ArrayList<>();

        SliceValue photoswithdatevalue = new SliceValue(diskspaceusage[0], Color.rgb(76, 175, 80));
        photoswithdatevalue.setLabel("Fotos fechadas");
        SliceValue photoswithoutdatevalue = new SliceValue(diskspaceusage[1], Color.rgb(205, 220, 57));
        photoswithoutdatevalue.setLabel("Fotos sin fechar");

        sliceValues.add(photoswithdatevalue);
        sliceValues.add(photoswithoutdatevalue);

        data.setHasLabels(true);
        data.setValues(sliceValues);

        piechart.setPieChartData(data);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        PhotosActivity.SHOULD_REFRESH_GRID = hasExecuted;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            finishAfterTransition();
        else
            finish();
    }
}

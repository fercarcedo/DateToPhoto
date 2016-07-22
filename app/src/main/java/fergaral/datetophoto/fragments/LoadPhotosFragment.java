package fergaral.datetophoto.fragments;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;

import java.io.File;
import java.util.ArrayList;

import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.Utils;

/**
 * Headless fragment which takes care of loading the photos
 * which haven't already been datestamped
 * Created by fer on 10/08/15.
 */
public class LoadPhotosFragment extends Fragment {

    private TaskCallbacks mCallback;
    private ImagesToProcessTask loadPhotosTask;
    private String selectedFolder;
    private Context mContext;

    public interface TaskCallbacks {
        void onPreExecute();

        void onPostExecute(ArrayList<String> result);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //La Activity debe implementar TaskCallbacks
        mCallback = (TaskCallbacks) activity;

        if (activity != null && mContext == null) //Solo lo asignamos una vez
            mContext = activity.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Mantenemos la instancia del Fragment durante cambios de configuración
        setRetainInstance(true);

        loadPhotosTask = new ImagesToProcessTask();
        loadPhotosTask.execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        //Asignamos a la callback null, para evitar que el GC no
        //pueda recolectar la Activity en desuso (p.ej., durate una rotación)
        mCallback = null;
    }

    public class ImagesToProcessTask extends AsyncTask<Void, Void, ArrayList<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (mCallback != null)
                mCallback.onPreExecute();
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            SQLiteDatabase photosDb = new DatabaseHelper(mContext).getReadableDatabase();
            ArrayList<String> cameraImages = new PhotoUtils(mContext).getCameraImages();
            //Obtenemos las fotos sin fechar de entre las que hay que procesar, ya que es más rápido identificar las que hay
            //que procesar, que no las que están sin fechars
            ArrayList<String> imagesToProcess;

            long startTime = System.currentTimeMillis();
            String s = "";

            if (selectedFolder == null) {
                long startTimeImagProcess = System.currentTimeMillis();

                imagesToProcess = Utils.getImagesToProcess(mContext, cameraImages);

                long elapsedImagProcess = System.currentTimeMillis() - startTimeImagProcess;

                s += "getImagesToProcess: " + elapsedImagProcess + "\n";

                long startTimeWithoutDate = System.currentTimeMillis();

                imagesToProcess = Utils.getPhotosWithoutDate(mContext,
                        imagesToProcess,
                        photosDb);

                long elapedTimeWithoutDate = System.currentTimeMillis() - startTimeWithoutDate;

                s += "getPhotosWithoutDate: " + elapedTimeWithoutDate + "\n";
            } else {
                long startTimeImagProcess = System.currentTimeMillis();

                imagesToProcess = Utils.getImagesToProcess(mContext, cameraImages, selectedFolder);

                long elapsedImagProcess = System.currentTimeMillis() - startTimeImagProcess;

                s += "getImagesToProcess: " + elapsedImagProcess + "\n";

                long startTimeWithoutDate = System.currentTimeMillis();

                imagesToProcess = Utils.getPhotosWithoutDate(mContext,
                        imagesToProcess,
                        photosDb);

                long elapedTimeWithoutDate = System.currentTimeMillis() - startTimeWithoutDate;

                s += "getPhotosWithoutDate: " + elapedTimeWithoutDate + "\n";
            }

            long elapsedTime = System.currentTimeMillis() - startTime;

            s += "TOTAL: " + elapsedTime + "\n";

            Utils.write(Environment.getExternalStorageDirectory().getPath() + File.separator + "Download" + File.separator + "dtpload.txt",
                    s);

            photosDb.close();

            return imagesToProcess;
        }

        @Override
        protected void onPostExecute(ArrayList<String> imagesToProcess) {
            super.onPostExecute(imagesToProcess);

            if (mCallback != null)
                mCallback.onPostExecute(imagesToProcess);
        }
    }

    public void refresh() {
        load(null);
    }

    public void load(String folderName) {
        if (loadPhotosTask != null && !loadPhotosTask.isCancelled()) {
            //Cancelamos la AsyncTask (el onPostExecute no se ejecutará)
            loadPhotosTask.cancel(true);
        }

        loadPhotosTask = new ImagesToProcessTask();
        selectedFolder = folderName;
        loadPhotosTask.execute();
    }
}

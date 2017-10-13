package fergaral.datetophoto.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.media.ExifInterface;
import android.os.AsyncTask;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.listeners.ProgressChangedListener;

/**
 * Created by Fer on 13/10/2017.
 */


public class SearchForAlreadyProcessedPhotosTask extends AsyncTask<Void, Integer, Void> {

    private ProgressChangedListener listener;
    private List<String> imagesToProcess;
    private WeakReference<Context> contextRef;

    public SearchForAlreadyProcessedPhotosTask(ProgressChangedListener listener,
                                               List<String> imagesToProcess,
                                               Context context) {
        this.listener = listener;
        this.imagesToProcess = imagesToProcess;
        this.contextRef = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.reportTotal(imagesToProcess.size());
    }

    @Override
    protected Void doInBackground(Void... params) {
        Context context = contextRef.get();
        if (context == null) return null;
        SQLiteDatabase db = new DatabaseHelper(context).getWritableDatabase();

        int progress = 0;

        for(String path : imagesToProcess) {
            try {
                ExifInterface exifInterface = new ExifInterface(path);

                String makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE);

                if(makeExif != null && makeExif.startsWith("dtp-")) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.PATH_COLUMN, path);

                    db.insert(DatabaseHelper.TABLE_NAME, null, values);
                }

                progress++;
                publishProgress(progress);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        db.close();

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        listener.onProgressChanged(values[0]);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        listener.reportEnd(false);
    }
}
package fergaral.datetophoto.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

import fergaral.datetophoto.activities.PhotosActivity;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 22/07/15.
 */
public class RegisterPhotoURIIntoDBService extends IntentService {

    public RegisterPhotoURIIntoDBService() {
        super(RegisterPhotoURIIntoDBService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Vamos a intentar ver si existe un archivo local asociado con la URI
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(PhotosActivity.EXTRA_IMAGE_URI);

        for(Uri imageUri : imageUris) {
            try {
                String filePath = Utils.getRealPathFromURI(this, imageUri);

                if (filePath != null) {
                    //Hay ruta real
                    SQLiteDatabase db = new DatabaseHelper(this).getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.PATH_COLUMN, filePath);

                    db.insert(DatabaseHelper.TABLE_NAME, null, values);
                    db.close();
                } else {
                    Log.d("TAG", "No hay ruta real");
                }
            } catch (Exception e) {
                //No hay ruta real, pasamos
                Log.d("TAG", "No hay ruta real");
            }
        }
    }
}

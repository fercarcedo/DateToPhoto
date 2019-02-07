package fergaral.datetophoto.services

import android.app.IntentService
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log

import java.util.ArrayList

import fergaral.datetophoto.activities.PhotosActivity
import fergaral.datetophoto.db.DatabaseHelper
import fergaral.datetophoto.utils.Utils

/**
 * Created by fer on 22/07/15.
 */
class RegisterPhotoURIIntoDBService : IntentService(RegisterPhotoURIIntoDBService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        //Vamos a intentar ver si existe un archivo local asociado con la URI
        val imageUris = intent!!.getParcelableArrayListExtra<Uri>(PhotosActivity.EXTRA_IMAGE_URI)

        for (imageUri in imageUris) {
            try {
                val filePath = Utils.getRealPathFromURI(this, imageUri)

                if (filePath != null) {
                    //Hay ruta real
                    val db = DatabaseHelper(this).writableDatabase
                    val values = ContentValues()
                    values.put(DatabaseHelper.PATH_COLUMN, filePath)

                    db.insert(DatabaseHelper.TABLE_NAME, null, values)
                    db.close()
                } else {
                    Log.d("TAG", "No hay ruta real")
                }
            } catch (e: Exception) {
                //No hay ruta real, pasamos
                Log.d("TAG", "No hay ruta real")
            }

        }
    }
}

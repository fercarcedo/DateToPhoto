package fergaral.datetophoto.tasks

import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import androidx.exifinterface.media.ExifInterface

import java.io.IOException
import java.lang.ref.WeakReference

import fergaral.datetophoto.db.DatabaseHelper
import fergaral.datetophoto.listeners.ProgressChangedListener
import fergaral.datetophoto.utils.Image

/**
 * Created by Fer on 13/10/2017.
 */
class SearchForAlreadyProcessedPhotosTask(private val listener: ProgressChangedListener,
                                          private val imagesToProcess: List<Image>,
                                          context: Context) : AsyncTask<Void, Int, Void>() {
    private val contextRef: WeakReference<Context>

    init {
        this.contextRef = WeakReference(context)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        listener.reportTotal(imagesToProcess.size)
    }

    override fun doInBackground(vararg params: Void): Void? {
        val context = contextRef.get() ?: return null
        val db = DatabaseHelper(context).writableDatabase

        var progress = 0

        for (image in imagesToProcess) {
            try {
                context.contentResolver.openInputStream(image.uri)?.let { inputStream ->
                    val exifInterface = ExifInterface(inputStream)

                    val makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE)

                    if (makeExif != null && makeExif.startsWith("dtp-")) {
                        val values = ContentValues()
                        values.put(DatabaseHelper.PATH_COLUMN, image.toString())

                        db.insert(DatabaseHelper.TABLE_NAME, null, values)
                    }

                    progress++
                    publishProgress(progress)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        db.close()

        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        listener.onProgressChanged(values[0]!!)
    }

    override fun onPostExecute(aVoid: Void?) {
        listener.reportEnd(false)
    }
}
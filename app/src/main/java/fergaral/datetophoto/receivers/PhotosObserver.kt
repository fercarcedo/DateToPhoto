package fergaral.datetophoto.receivers

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.widget.Toast

import java.io.File

/**
 * Created by fer on 10/06/16.
 */
class PhotosObserver
/**
 * Creates a content observer.
 *
 * @param handler The handler to run [.onChange] on, or null if none.
 */
(private val mContext: Context, handler: Handler) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        this.onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        val media = readFromMediaStore(mContext,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if (media?.file != null)
            Toast.makeText(mContext, "Detected " + media.file.name, Toast.LENGTH_SHORT).show()
    }

    private fun readFromMediaStore(context: Context, uri: Uri): Media? {
        val cursor = context.contentResolver.query(uri, null, null, null,
                "date_added DESC")

        var media: Media? = null

        if (cursor != null && cursor.moveToNext()) {
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            media = Media(File(cursor.getString(dataColumn)),
                    cursor.getString(mimeTypeColumn))
        }

        cursor?.close()

        return media
    }

    private inner class Media(val file: File, val type: String)


}

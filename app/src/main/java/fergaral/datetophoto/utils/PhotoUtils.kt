package fergaral.datetophoto.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import fergaral.datetophoto.DateToPhoto
import fergaral.datetophoto.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by Parejúa on 02/11/2014.
 */
class PhotoUtils(private val context: Context) {
    private var msConn: MediaScannerConnection? = null

    val folders: Set<String>
        @SuppressLint("InlinedApi")
        get() {
            val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            return queryMediaStore<String>(projection, null, null) { cursorExternal, result ->
                processFoldersCursor(cursorExternal, result)
            }.toSet()
        }

    /**
     * Method that returns an ArrayList with the photos of the gallery
     * @return ArrayList<String> photos from gallery
     */
    @SuppressLint("InlinedApi")
    fun getCameraImages(foldersToProcess: Array<String>): ArrayList<Image> {
        val bucketName = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME, bucketName, MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
        } else {
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME, bucketName, MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.DATA)
        }

        val query = Array(foldersToProcess.size) { "?" }.joinToString(",")
        return queryMediaStore(projection, "$bucketName IN ($query)", foldersToProcess) { cursorExternal, result ->
            processPhotosCursor(cursorExternal, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, result)
        }
    }

    @SuppressLint("InlinedApi")
    fun getCameraImages(): ArrayList<Image> {
        val bucketName = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, bucketName, MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)

        return queryMediaStore(projection, null, null) { cursorExternal, result ->
            processPhotosCursor(cursorExternal, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, result)
        }
    }

    private fun <T> queryMediaStore(projection: Array<String>, selection: String?, selectionArgs: Array<String>?, callback: (Cursor?, MutableList<T>) -> Unit): ArrayList<T> {
        val cursorExternal = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs,
                MediaStore.MediaColumns.DATE_ADDED + " DESC")

        val result: ArrayList<T> = if (cursorExternal != null)
            ArrayList(cursorExternal.count)
        else
            ArrayList()

        callback(cursorExternal, result)

        return result
    }

    private fun processPhotosCursor(cursor: Cursor?, baseUri: Uri, photosUriList: MutableList<Image>) {
        processImagesCursor(cursor, baseUri, photosUriList)
    }

    @SuppressLint("InlinedApi")
    private fun processImagesCursor(cursor: Cursor?, baseUri: Uri, photosUriList: MutableList<Image>) {
        if (cursor == null)
            return

        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            do {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    photosUriList.add(Image(displayName, bucketName, dateAdded, ContentUris.withAppendedId(baseUri, id)))
                } else {
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    photosUriList.add(Image(displayName, bucketName, dateAdded, ContentUris.withAppendedId(baseUri, id), path))
                }
            } while (cursor.moveToNext())
        }

        cursor.close()
    }

    @SuppressLint("InlinedApi")
    private fun processFoldersCursor(cursor: Cursor?, foldersList: MutableList<String>) {
        if (cursor == null)
            return

        if (cursor.moveToFirst()) {
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            do {
                val bucketName = cursor.getString(bucketNameColumn)
                if ("Date To Photo" != bucketName) {
                    foldersList.add(bucketName)
                }
            } while (cursor.moveToNext())
        }

        cursor.close()
    }

    /**
     * Method that returns the date associated with a photo, which can be EXIF, or if it's not present, the file date
     * @param exif ExifInterface object
     * @param tag EXIF tag
     * @param imgFile File (used to get file date if EXIF date is not present)
     * @return String date of the photo
     */
    private fun getExifTag(exif: ExifInterface, tag: String, imgFile: File): String {
        //De la forma: 2014:09:21 13:53:58
        val attribute = exif.getAttribute(tag)

        if (attribute != null) {
            /*String date = attribute.substring(0, attribute.indexOf(" ")); //Ejemplo: 2014:09:21
            StringBuffer buffer = new StringBuffer(date);

            for (int i = 0; i < date.length(); i++) {
                if (date.charAt(i) == ':')
                    buffer.setCharAt(i, '/');
            }

            String dateString = "";

            try {
                Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(buffer.toString());
                dateString = new SimpleDateFormat("dd-MM-yyyy").format(date1);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return dateString;*/

            val year = attribute.substring(0, 4)
            val month = attribute.substring(5, 7)
            val day = attribute.substring(8, 10)

            return day + "/" + month + "/" + year
        }

        val lastModDate = Date(imgFile.lastModified())
        val cal = Calendar.getInstance()
        cal.time = lastModDate
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        return day.toString() + "/" + month + "/" + year
    }

    fun scanPhoto(imageFileName: String) {

        msConn = MediaScannerConnection(context, object : MediaScannerConnection.MediaScannerConnectionClient {
            override fun onMediaScannerConnected() {
                msConn!!.scanFile(imageFileName, null)
            }

            override fun onScanCompleted(path: String, uri: Uri) {
                msConn!!.disconnect()
            }
        })
        msConn!!.connect()
    }

    companion object {
        val FIRST_USE_KEY = "firstuse"

        fun getFolders(context: Context): Set<String> = PhotoUtils(context).folders

        fun getParentFolderName(path: String): String {
            val pathSegments = path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

            return pathSegments[pathSegments.size - 2]
        }

        fun getFileWithDate(fileWithoutDate: String): String {
            val pathSegments = fileWithoutDate.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val name = pathSegments[pathSegments.size - 1]

            return fileWithoutDate.substring(0, fileWithoutDate.length - name.length) + "dtp-" + name
        }

        fun deletePhoto(context: Context, s: String) {
            val file = File(s)
            val filePath = file.absolutePath

            file.delete()
            MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
        }

        fun selectAllFolders(context: Context): Set<String> {
            val folderNames = PhotoUtils.getFolders(context)

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putStringSet(context.getString(R.string.pref_folderstoprocess_key), HashSet(folderNames))
            editor.apply()

            return folderNames
        }

        fun selectAllFoldersOnFirstUse(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            if (prefs.getBoolean(FIRST_USE_KEY, true)) {
                selectAllFolders(context)

                val editor = prefs.edit()
                editor.putBoolean(FIRST_USE_KEY, false)
                editor.apply()
            }
        }

        fun incorrectFormat(image: Image): Boolean {
            return !(image.name.endsWith(".jpg") || image.name.endsWith(".JPG") || image.name.endsWith(".jpeg") ||
                    image.name.endsWith(".JPEG") || image.name.endsWith(".png") || image.name.endsWith(".PNG"))

            //Puede ser que la foto sea, por ejemplo, un JPEG pero que dentro haya texto, no una imagen
            /*BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //Solo queremos saber ancho y alto
        BitmapFactory.decodeFile(image, options);

        return options.outWidth == -1 || options.outHeight == -1;*/
        }

        fun getName(image: String): String {
            val pathSegments = image.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

            return pathSegments[pathSegments.size - 1]
        }

        @Throws(IOException::class)
        fun copy(src: File, dst: File) {
            var inStream: FileInputStream? = null
            var outStream: FileOutputStream? = null

            try {
                inStream = FileInputStream(src)
                outStream = FileOutputStream(dst)
                val inChannel = inStream.channel
                val outChannel = outStream.channel
                inChannel.transferTo(0, inChannel.size(), outChannel)
            } finally {
                if (inStream != null)
                    inStream.close()

                if (outStream != null)
                    outStream.close()
            }
        }

        fun isCorrupted(imageFile: File?): Boolean {
            if (imageFile == null)
                return true

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeFile(imageFile.absolutePath, options)

            return options.outWidth == -1 || options.outHeight == -1
        }
    }
}

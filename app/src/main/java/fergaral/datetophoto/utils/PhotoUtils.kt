package fergaral.datetophoto.utils

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.widget.Toast

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashSet

import fergaral.datetophoto.R

/**
 * Created by Parejúa on 02/11/2014.
 */
class PhotoUtils(private val context: Context) {
    private var msConn: MediaScannerConnection? = null

    /**
     * Method that returns an ArrayList with the photos of the gallery
     * @return ArrayList<String> photos from gallery
    </String> */
    // Set up an array of the Thumbnail Image ID column we want
    val cameraImages: ArrayList<String>
        get() {
            val projection = arrayOf(MediaStore.Images.Media.DATA)

            val cursorExternal = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null,
                    MediaStore.MediaColumns.DATE_ADDED + " DESC")

            val cursorInternal = context.contentResolver.query(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    projection, null, null,
                    MediaStore.MediaColumns.DATE_ADDED + " DESC")

            val result: ArrayList<String>

            if (cursorExternal != null && cursorInternal == null)
                result = ArrayList(cursorExternal.count)
            else if (cursorExternal != null)
                result = ArrayList(cursorExternal.count + cursorInternal!!.count)
            else if (cursorInternal != null)
                result = ArrayList(cursorInternal.count)
            else
                result = ArrayList()

            processPhotosCursor(cursorExternal, result)
            processPhotosCursor(cursorInternal, result)

            return result
        }

    val cameraThumbnails: List<Thumbnail>
        get() {
            val projection = arrayOf(MediaStore.Images.Thumbnails.DATA, MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails._ID)

            val cursorExternal = context.contentResolver.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    projection, null, null, null)

            val cursorInternal = context.contentResolver.query(MediaStore.Images.Thumbnails.INTERNAL_CONTENT_URI,
                    projection, null, null, null)

            val cameraThumbnails: ArrayList<Thumbnail>

            if (cursorExternal != null && cursorInternal == null)
                cameraThumbnails = ArrayList(cursorExternal.count)
            else if (cursorExternal != null)
                cameraThumbnails = ArrayList(cursorExternal.count + cursorInternal!!.count)
            else if (cursorInternal != null)
                cameraThumbnails = ArrayList(cursorInternal.count)
            else
                cameraThumbnails = ArrayList()

            processPhotosThumbnailPhotoCursor(cursorExternal, cameraThumbnails)
            processPhotosThumbnailPhotoCursor(cursorInternal, cameraThumbnails)

            return cameraThumbnails
        }

    private fun processPhotosThumbnailPhotoCursor(cursor: Cursor?, thumbnails: MutableList<Thumbnail>?) {
        if (cursor == null || thumbnails == null)
            return

        if (cursor.moveToFirst()) {
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA)
            val imageIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID)

            do {
                val thumbPath = cursor.getString(dataColumn)
                val imageId = cursor.getInt(imageIdColumn)
                val imageURI = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        Integer.toString(cursor.getInt(idColumn)))

                thumbnails.add(Thumbnail(imageId.toString(), thumbPath, imageURI))
            } while (cursor.moveToNext())
        }

        cursor.close()
    }

    private fun processPhotosThumbnailCursor(cursor: Cursor, photosUriList: MutableList<String>) {
        processImagesCursor(cursor, photosUriList, MediaStore.Images.Thumbnails.DATA)
    }

    private fun processPhotosCursor(cursor: Cursor?, photosUriList: MutableList<String>) {
        processImagesCursor(cursor, photosUriList, MediaStore.Images.Media.DATA)
    }

    private fun processImagesCursor(cursor: Cursor?, photosUriList: MutableList<String>?, imageDataColumn: String) {
        if (photosUriList == null || cursor == null)
            return

        if (cursor.moveToFirst()) {
            val dataColumn = cursor.getColumnIndexOrThrow(imageDataColumn)
            do {
                val data = cursor.getString(dataColumn)
                //Log.i("data :", data);
                photosUriList.add(data)
            } while (cursor.moveToNext())
        }

        cursor.close()
    }

    /**
     * Method that puts date to the photos of the gallery
     * @param cameraImages ArrayList with the photos of the gallery
     */
    fun processPhotos(cameraImages: ArrayList<String>?) {

        if (cameraImages != null) {
            var total = 0
            var actual = 0
            total = cameraImages.size


            for (s in cameraImages) {
                var imgFile: File? = File(s)
                if (imgFile!!.exists()) {


                    if (!imgFile.name.contains("dtp-")) {


                        val imgFileWithDate = File(imgFile.parentFile.absolutePath + "/dtp-" + imgFile.name)

                        if (!imgFileWithDate.exists()) {


                            val options = BitmapFactory.Options()
                            //options.inMutable = true;
                            options.inPreferredConfig = Bitmap.Config.ARGB_8888

                            var myBitmap: Bitmap? = BitmapFactory.decodeFile(imgFile.absolutePath, options)
                            myBitmap = convertToMutable(myBitmap)

                            var date = ""

                            try {
                                val exifInterface = ExifInterface(imgFile.absolutePath)
                                date = getExifTag(exifInterface, ExifInterface.TAG_DATETIME, imgFile)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            var bitmap2: Bitmap? = writeDateOnBitmap(myBitmap, date)

                            myBitmap = null

                            //CapturePhotoUtils.insertImage(getContentResolver(), bitmap2, imgFile.getName() + "-dtp.jpg", "generated using Date To Photo");
                            savePhoto(bitmap2, imgFile.parentFile.absolutePath, "dtp-" + imgFile.name)

                            bitmap2 = null

                        }
                    }
                }

                actual++

                imgFile = null
            }

        } else {
            /*AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MyActivity.this);
            alertDialogBuilder.setTitle("No hay fotos");
            alertDialogBuilder.setMessage("No se han encontrado fotos en la memoria de tu dispositivo");
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alertDialogBuilder.show();

            return null;*/
        }
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

    /**
     * Method that converts an immutable Bitmap to a mutable one
     * @param imgIn immutable Bitmap
     * @return Bitmap the bitmap passed in, but mutable
     */
    private fun convertToMutable(imgIn: Bitmap?): Bitmap? {
        var imgIn = imgIn
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            val file = File(Environment.getExternalStorageDirectory().toString() + File.separator + "temp.tmp")

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            val randomAccessFile = RandomAccessFile(file, "rw")

            // get the width and height of the source bitmap.
            val width = imgIn!!.width
            val height = imgIn.height
            val type = imgIn.config

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            val channel = randomAccessFile.channel
            val map = channel.map(FileChannel.MapMode.READ_WRITE, 0, (imgIn.rowBytes * height).toLong())
            imgIn.copyPixelsToBuffer(map)
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle()
            System.gc()// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type)
            map.position(0)
            //load it back from temporary
            imgIn!!.copyPixelsFromBuffer(map)
            //close the temporary file and channel , then delete that also
            channel.close()
            randomAccessFile.close()

            // delete the temp file
            file.delete()

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return imgIn
    }

    /**
     * Method that puts date on a Bitmap
     * @param b Bitmap without date
     * @param text Date as a string
     * @return Bitmap with date
     */
    fun writeDateOnBitmap(b: Bitmap?, text: String): Bitmap {

        val scale = context.resources.displayMetrics.density
        var bitmapConfig: Bitmap.Config? = b!!.config

        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888
        }

        //b = b.copy(bitmapConfig, true);


        val canvas = Canvas(b)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.YELLOW
        paint.textSize = (b.height / 20).toFloat()
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        var x = (b.width - bounds.width()) / 2
        var y = (b.height + bounds.height()) / 2

        x = b.width - bounds.width()
        y = b.height - bounds.height()

        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

        //Toast.makeText(this, String.valueOf(b.getHeight()), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Height: "+ String.valueOf(b.getHeight()) + " Width: " + String.valueOf(b.getWidth()), Toast.LENGTH_LONG).show();
        return b
    }

    /**
     * Method that saves the Bitmap passed in to the device's gallery
     * @param bmp
     * @param basePath
     * @param name
     */
    fun savePhoto(bmp: Bitmap?, basePath: String, name: String) {
        val imageFileFolder = File(basePath)
        var out: FileOutputStream? = null
        val imageFileName = File(imageFileFolder, name)
        try {
            out = FileOutputStream(imageFileName)
            bmp!!.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            scanPhoto(imageFileName.toString())
            out = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

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

        fun getFolders(ctx: Context): ArrayList<String> {
            val cameraImages = PhotoUtils(ctx).cameraImages
            val folderNames = ArrayList<String>()

            for (path in cameraImages) {
                val parentFolderName = getParentFolderName(path)

                if (!folderNames.contains(parentFolderName)) {
                    folderNames.add(parentFolderName)
                }
            }

            return folderNames
        }


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

        fun selectAllFolders(context: Context): List<String> {
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

        fun incorrectFormat(image: String): Boolean {
            return !(image.endsWith(".jpg") || image.endsWith(".JPG") || image.endsWith(".jpeg") ||
                    image.endsWith(".JPEG") || image.endsWith(".png") || image.endsWith(".PNG"))

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

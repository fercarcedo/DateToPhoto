package fergaral.datetophoto.services

import android.app.Activity
import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ResultReceiver
import android.preference.PreferenceManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log
import android.webkit.MimeTypeMap

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.ArrayList
import java.util.Calendar
import java.util.Date

import fergaral.datetophoto.R
import fergaral.datetophoto.activities.PhotosActivity
import fergaral.datetophoto.db.DatabaseHelper
import fergaral.datetophoto.exif.ExifInterface
import fergaral.datetophoto.listeners.ProgressChangedListener
import fergaral.datetophoto.utils.NotificationUtils
import fergaral.datetophoto.utils.Utils

class ProcessPhotosURIService : IntentService("ProcessPhotosURIService") {

    private val msConn: MediaScannerConnection? = null
    private var onBackground = true
    private var receiver: ResultReceiver? = null
    private var tempStr = ""
    private var dialogCancelled: Boolean = false
    private val mNotifStartTime: Long = 0
    private var running: Boolean = false
    private var showNotif: Boolean = false
    private var total: Int = 0
    private var actual: Int = 0
    private var secondListener: ProgressChangedListener? = null
    private var mNotificationUtils: NotificationUtils? = null
    private var wasLarge: Boolean = false
    private var wasPathFound = true

    private val currentLocalizedDate: String
        get() = Utils.getFormattedDate(Date())

    override fun onHandleIntent(intent: Intent?) {

        mNotificationUtils = NotificationUtils(this)
        receiver = intent!!.getParcelableExtra("receiver")
        onBackground = intent.getBooleanExtra("onBackground", true)

        if (onBackground) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val active = sharedPreferences.getBoolean(getString(R.string.pref_active_key), true)

            if (!active) {
                return
            }
        }

        running = true

        /*LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (running) {
                    myReceiver = intent.getParcelableExtra("receiver");

                    if (myReceiver != null) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("total", total);

                        myReceiver.send(Activity.RESULT_OK, bundle);

                        bundle = new Bundle();
                        bundle.putInt("progress", actual);

                        myReceiver.send(Activity.RESULT_OK, bundle);
                    }
                }
            }
        }, new IntentFilter(MyActivity.INTENT_QUERY_ACTION));*/

        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                if (running) {
                    secondListener = intent.getSerializableExtra("listener") as ProgressChangedListener

                    if (secondListener != null) {
                        secondListener!!.reportTotal(total)
                        secondListener!!.onProgressChanged(actual)
                    }
                }
            }
        }, IntentFilter(PhotosActivity.INTENT_QUERY_ACTION))

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        showNotif = sharedPreferences.getBoolean(getString(R.string.pref_shownotification_key), true)

        if (showNotif)
            mNotificationUtils!!.showProgressNotification("Procesando tus fotos en segundo plano...")

        val galleryImages = ArrayList<Uri>()

        if (intent.getStringArrayListExtra("cameraimages") != null) {
            val galleryImagesString = intent.getStringArrayListExtra("cameraimages")

            for (uriString in galleryImagesString) {
                galleryImages.add(Uri.parse(uriString))
            }

            Log.d("MIDEBUG", galleryImages.size.toString())
        } else {
            return
        }

        total = 0
        actual = 0
        total = galleryImages.size

        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dialogCancelled = intent.getBooleanExtra("dialogcancelled", false)
            }
        }, IntentFilter(PhotosActivity.INTENT_ACTION))

        if (receiver != null) {
            val bundle = Bundle()
            bundle.putInt("total", total)

            receiver!!.send(Activity.RESULT_OK, bundle)
        }

        if (secondListener != null) {
            secondListener!!.reportTotal(total)
        }

        val isHoneycomb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB

        for (uri in galleryImages) {
            if (!dialogCancelled) {

                //Primero comprobamos si su extensión es soportada
                val contentResolver = contentResolver
                val mime = MimeTypeMap.getSingleton()
                val fileExtension = mime.getExtensionFromMimeType(contentResolver.getType(uri))

                if (fileExtension != null) {
                    if (!(fileExtension == "jpeg" || fileExtension == "jpg" || fileExtension == "png"))
                        continue
                } else {
                    //Podría ser que la URI fuese de la forma file:/// (porque las de la forma content:/// pasan siempre
                    //por el if). Intentamos ver si no acaba en alguno de los formatos admitidos

                    val uriString = uri.toString()

                    if (!(uriString.endsWith(".jpeg") || uriString.endsWith(".jpg") || uriString.endsWith(".png"))) {
                        continue
                    }
                }

                var myBitmap: Bitmap?

                try {
                    //myBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    myBitmap = decodeUri(this, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                    continue
                }

                if (myBitmap == null) {
                    actual++

                    if (receiver != null) {
                        val bundle = Bundle()
                        bundle.putInt("progress", actual)

                        receiver!!.send(Activity.RESULT_OK, bundle)
                    }

                    if (secondListener != null) {
                        secondListener!!.onProgressChanged(actual)
                    }

                    mNotificationUtils!!.setNotificationProgress(total, actual)

                    continue
                }

                tempStr += uri.toString() + " -> "

                if (!isHoneycomb)
                    myBitmap = convertToMutable(myBitmap)

                var date = ""
                var exifDate: String? = ""
                var orientationAndroidExif = android.media.ExifInterface.ORIENTATION_NORMAL
                //int rotation = ExifInterface.Orientation

                try {
                    val exifInterface = ExifInterface()
                    exifInterface.readExif(getContentResolver().openInputStream(uri))
                    date = getExifDate(exifInterface)
                    exifDate = exifInterface.getTagStringValue(ExifInterface.TAG_DATE_TIME)

                    //Obtenemos la rotación de la imagen y la convertimos a las constantes de android.media.ExifInterface
                    val orientationObj = exifInterface.getTagValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION)
                    var orientation = fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT //ExifInterface.ORIENTATION_NORMAL

                    if (orientationObj != null)
                        orientation = exifInterface.getTagIntValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION)!!.toShort()

                    when (orientation) {
                        fergaral.datetophoto.exif.ExifInterface.Orientation.BOTTOM_LEFT -> orientationAndroidExif = android.media.ExifInterface.ORIENTATION_ROTATE_180
                        fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_BOTTOM -> orientationAndroidExif = android.media.ExifInterface.ORIENTATION_ROTATE_270
                        fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_TOP -> orientationAndroidExif = android.media.ExifInterface.ORIENTATION_ROTATE_90
                        fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT -> orientationAndroidExif = android.media.ExifInterface.ORIENTATION_NORMAL
                    }

                    if (exifDate != null) {
                        exifDate = exifDate.replace(" ".toRegex(), "").replace(":".toRegex(), "")
                    } else {
                        //Si no hay fecha EXIF, le ponemos de nombre la fecha de hoy, con el mismo formato
                        exifDate = getCurrentDate("")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                if (exifDate == "") {
                    exifDate = getCurrentDate("")
                }

                if (date == "") {
                    date = currentLocalizedDate
                }

                var bitmap2: Bitmap? = writeDateOnBitmap(myBitmap, date, orientationAndroidExif)
                @Suppress("UNUSED_VALUE")
                myBitmap = null

                val file2 = File(Environment.getExternalStorageDirectory().path + "/Date To Photo")

                if (!file2.exists())
                    file2.mkdir()

                var hasRealPath = false
                var filePath: String? = ""

                try {
                    filePath = Utils.getRealPathFromURI(this, uri)

                    if (filePath != null) {
                        //Hay ruta real
                        hasRealPath = true
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

                if (hasRealPath) {
                    val imgFile = File(filePath!!)
                    hasRealPath = imgFile.exists()
                    Log.d("TAG", "hasRealPath=" + hasRealPath)
                }

                if (!hasRealPath) {
                    wasPathFound = false
                    savePhoto(bitmap2, Environment.getExternalStorageDirectory().path + "/Date To Photo", "dtp-$exifDate.jpg")
                } else {
                    val imgFile = File(filePath!!)
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                    val overwritePhotos = prefs.getBoolean(getString(R.string.pref_overwrite_key), true)
                    var keepLargePhoto = prefs.getBoolean(getString(R.string.pref_keeplargephoto_key), true)
                    keepLargePhoto = keepLargePhoto && wasLarge

                    if (overwritePhotos && !keepLargePhoto) {
                        savePhoto(bitmap2, imgFile.parentFile.absolutePath, imgFile.name)
                    } else {
                        savePhoto(bitmap2, imgFile.parentFile.absolutePath, "dtp-" + imgFile.name)
                    }
                }

                try {
                    val exifInterface: android.media.ExifInterface

                    if (!hasRealPath) {
                        exifInterface = android.media.ExifInterface(File(
                                Environment.getExternalStorageDirectory().path + "/DateToPhoto/" + "dtp-" + exifDate + ".jpg").absolutePath)
                    } else {
                        val imgFile = File(filePath!!)
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                        val overwritePhotos = prefs.getBoolean(getString(R.string.pref_overwrite_key), true)
                        var keepLargePhoto = prefs.getBoolean(getString(R.string.pref_keeplargephoto_key), true)
                        keepLargePhoto = keepLargePhoto && wasLarge

                        if (overwritePhotos && !keepLargePhoto) {
                            exifInterface = android.media.ExifInterface(imgFile.absolutePath)
                        } else {
                            exifInterface = android.media.ExifInterface(
                                    imgFile.parentFile.absolutePath + "/dtp-" + imgFile.name
                            )
                        }
                    }
                    exifInterface.setAttribute(android.media.ExifInterface.TAG_MAKE, "dtp-")
                    exifInterface.setAttribute(android.media.ExifInterface.TAG_ORIENTATION,
                            orientationAndroidExif.toString())

                    exifInterface.saveAttributes()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                @Suppress("UNUSED_VALUE")
                bitmap2 = null

                /*if(Utils.overwritePhotos(this))
                            {
                                PhotoUtils.deletePhoto(this, s);
                            }*/
            }

            actual++

            if (receiver != null) {
                val bundle = Bundle()
                bundle.putInt("progress", actual)

                receiver!!.send(Activity.RESULT_OK, bundle)
            }

            if (secondListener != null) {
                secondListener!!.onProgressChanged(actual)
            }

            if (showNotif)
                mNotificationUtils!!.setNotificationProgress(total, actual)
        }

        end()
    }

    private fun getExifDate(exif: ExifInterface): String {
        //De la forma: 2014:09:21 13:53:58
        val attribute = exif.getTagStringValue(ExifInterface.TAG_DATE_TIME)

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

            val year = Integer.parseInt(attribute.substring(0, 4))
            val month = Integer.parseInt(attribute.substring(5, 7))
            val day = Integer.parseInt(attribute.substring(8, 10))

            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day)

            return Utils.getFormattedDate(cal.time)
        }

        return currentLocalizedDate
    }

    fun savePhoto(bmp: Bitmap?, basePath: String, name: String) {
        val imageFileFolder = File(basePath)

        var out: FileOutputStream?
        val imageFileName = File(imageFileFolder, name)
        try {
            out = FileOutputStream(imageFileName)
            bmp!!.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            scanPhoto(imageFileName.toString())
            @Suppress("UNUSED_VALUE")
            out = null
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("TAG", "Excepción al guardar la foto")
        }

    }

    fun scanPhoto(imageFileName: String) {

        /*     msConn = new MediaScannerConnection(MyActivity.this,new MediaScannerConnection.MediaScannerConnectionClient()
        {
            public void onMediaScannerConnected()
            {
                msConn.scanFile(imageFileName, null);
            }
            public void onScanCompleted(String path, Uri uri)
            {
                msConn.disconnect();
            }
        });
        msConn.connect();*/

        /*Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.parse(imageFileName));
        sendBroadcast(intent);*/

        MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(imageFileName), null
        ) { _, _ -> }
    }

    private fun getStringOfNumber(number: Int): String {
        return if (number < 10) {
            "0$number"
        } else {
            number.toString()
        }
    }

    private fun end() {
        Utils.write(Environment.getExternalStorageDirectory().path + "/Download/datetophotoimages.txt", tempStr)

        if (showNotif)
            mNotificationUtils!!.endProgressNotification("El proceso ha finalizado")

        running = false

        if (receiver != null) {
            val bundle = Bundle()
            bundle.putString("endShared", "endShared")

            receiver!!.send(Activity.RESULT_OK, bundle)
        }

        if (secondListener != null) {
            secondListener!!.reportEnd(!wasPathFound)
        }

        if (dialogCancelled)
            dialogCancelled = false
    }

    @Throws(FileNotFoundException::class)
    fun decodeUri(c: Context, uri: Uri): Bitmap? {
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(c.contentResolver.openInputStream(uri), null, options)

        var imageWidth : Double = options.outWidth.toDouble()
        var imageHeight : Double = options.outHeight.toDouble()

        //Hay que asegurarse de que nuestro bitmap no exceda el tamaño de maxMemory
        // (Runtime.getRuntime().maxMemory()), teniendo en cuenta que cada píxel ocupa 4 bytes
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024
        val freeMemory = maxMemory - usedMemory

        var imageSize = (imageWidth * imageHeight * 4 / 1024).toLong() //Este valor era 12 unidades menos que el que debía ser

        wasLarge = imageSize >= freeMemory

        while (imageSize >= freeMemory) {
            imageWidth /= 1.15
            imageHeight /= 1.15
            imageSize = (imageWidth * imageHeight * 4 / 1024).toLong()
        }

        val previousSize = (options.outWidth * options.outHeight * 4 / 1024).toLong()
        //double reduction = (double)previousSize / imageSize;
        val reduction = Math.ceil(previousSize.toDouble() / imageSize).toInt()

        options = BitmapFactory.Options()

        options.inMutable = true
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        if (wasLarge)
            options.inSampleSize = reduction

        return BitmapFactory.decodeStream(c.contentResolver.openInputStream(uri), null, options)
    }

    private fun getCurrentDate(separator: String): String {
        val currentDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        return (getStringOfNumber(calendar.get(Calendar.YEAR)) + separator
                + getStringOfNumber(calendar.get(Calendar.MONTH) + 1) + separator
                + getStringOfNumber(calendar.get(Calendar.DAY_OF_MONTH)) + separator +
                getStringOfNumber(calendar.get(Calendar.HOUR_OF_DAY)) + separator
                + getStringOfNumber(calendar.get(Calendar.MINUTE)) + separator
                + getStringOfNumber(calendar.get(Calendar.SECOND)))
    }

    fun writeDateOnBitmap(b: Bitmap, text: String, orientation: Int): Bitmap {
        val canvas = Canvas(b)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.YELLOW
        paint.textSize = Math.min(b.width / 20, b.height / 20).toFloat()
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        //int x = (b.getWidth() - bounds.width()) / 2;
        //int y = (b.getHeight() + bounds.height()) / 2;

        val marginWidth = b.width * 0.2 / 10
        val marginHeight = b.height * 0.2 / 10
        var x = (b.width.toDouble() - bounds.width().toDouble() - marginWidth).toInt()
        var y = (b.height - marginHeight).toInt()

        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> {
                canvas.save()

                x = bounds.height() //altotexto
                y = (b.height.toDouble() - bounds.width().toDouble() - marginHeight).toInt() //alto - anchotexto

                canvas.rotate(-270f, x.toFloat(), y.toFloat())

                canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

                canvas.restore()
            }
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> {
                canvas.save()

                /*x = b.getWidth() - bounds.height(); //ancho - altotexto
                y = (int) (bounds.width() + margin); //anchotexto*/

                x = b.width - bounds.height() //ancho - altotexto
                y = (bounds.width() + marginHeight).toInt() //anchotexto

                canvas.rotate(-90f, x.toFloat(), y.toFloat())


                canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

                canvas.restore()
            }
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> {
                canvas.save()

                x = (bounds.width() + marginWidth).toInt()
                y = marginHeight.toInt()

                canvas.rotate(-180f, x.toFloat(), y.toFloat())

                canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

                canvas.restore()
            }
            else -> {
                canvas.drawText(text, x.toFloat(), y.toFloat(), paint)
            }
        }

        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> tempStr += "ORIENTATION_ROTATE_270"
            android.media.ExifInterface.ORIENTATION_TRANSVERSE -> tempStr += "ORIENTATION_TRANSVERSE"
            android.media.ExifInterface.ORIENTATION_TRANSPOSE -> tempStr += "ORIENTATION_TRANSPOSE"
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> tempStr += "ORIENTATION_ROTATE_90"
            android.media.ExifInterface.ORIENTATION_UNDEFINED -> tempStr += "ORIENTATION_UNDEFINED"
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> tempStr += "ORIENTATION_ROTATE_180"
            android.media.ExifInterface.ORIENTATION_NORMAL -> tempStr += "ORIENTATION_NORMAL"
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> tempStr += "ORIENTATION_FLIP_HORIZONTAL"
            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> tempStr += "ORIENTATION_FLIP_VERTICAL"
        }

        tempStr += "\n"

        //Toast.makeText(this, String.valueOf(b.getHeight()), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Height: "+ String.valueOf(b.getHeight()) + " Width: " + String.valueOf(b.getWidth()), Toast.LENGTH_LONG).show();
        return b
    }

    companion object {

        private val NOTIFICATION_ID = 1

        fun convertToMutable(imgIn: Bitmap): Bitmap {
            var bitmap = imgIn
            try {
                //this is the file going to use temporally to save the bytes.
                // This file will not be a image, it will store the raw image data.
                val file = File(Environment.getExternalStorageDirectory().toString() + File.separator + "temp.tmp")

                //Open an RandomAccessFile
                //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                //into AndroidManifest.xml file
                val randomAccessFile = RandomAccessFile(file, "rw")

                // get the width and height of the source bitmap.
                val width = bitmap.width
                val height = bitmap.height
                val type = bitmap.config

                //Copy the byte to the file
                //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
                val channel = randomAccessFile.channel
                val map = channel.map(FileChannel.MapMode.READ_WRITE, 0, (bitmap.rowBytes * height).toLong())
                bitmap.copyPixelsToBuffer(map)
                //recycle the source bitmap, this will be no longer used.
                bitmap.recycle()
                System.gc()// try to force the bytes from the imgIn to be released

                //Create a new bitmap to load the bitmap again. Probably the memory will be available.
                bitmap = Bitmap.createBitmap(width, height, type)
                map.position(0)
                //load it back from temporary
                bitmap.copyPixelsFromBuffer(map)
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

            return bitmap
        }
    }
}

package fergaral.datetophoto.utils

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
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
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import fergaral.datetophoto.DateToPhoto

import fergaral.datetophoto.R
import fergaral.datetophoto.activities.PhotosActivity
import fergaral.datetophoto.db.DatabaseHelper
import java.io.*
import java.util.*
import kotlin.math.ceil

/**
 * Created by Fer on 06/10/2017.
 */

class ProcessPhotos {

    private val msConn: MediaScannerConnection? = null
    private val onBackground = true
    private var receivers: Array<ResultReceiver?> = arrayOfNulls(2)
    private var tempStr = ""
    private var dialogCancelled: Boolean = false
    private var cancelledCharger: Boolean = false
    private var running: Boolean = false
    private var total: Int = 0
    private var actual: Int = 0
    private var scale: Float = 0.toFloat()
    private var paint: Paint? = null
    private var photosDb: SQLiteDatabase? = null
    private var mNotificationUtils: NotificationUtils? = null
    private var context: Context? = null
    private var shouldRegisterPhoto: Boolean = false
    private var keepLargePhoto: Boolean = false

    fun execute(context: Context) {
        execute(null, true, null, context)
    }

    fun execute(resultReceiver: ResultReceiver?, onBackground: Boolean,
                cameraImages: ArrayList<Image>?, context: Context) {
        this.context = context
        mNotificationUtils = NotificationUtils(context)
        scale = context.resources.displayMetrics.density
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint!!.color = Color.YELLOW
        paint!!.setShadowLayer(1f, 0f, 1f, Color.WHITE)
        photosDb = DatabaseHelper(context).writableDatabase

        receivers[RECEIVER_POSITION] = resultReceiver

        //Check if we have permission to write to the external storage
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //We don't have permission, notify the user about it
            mNotificationUtils!!.showPermissionNotification()
            return  //We can't do anything without it
        }

        if (onBackground) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val active = sharedPreferences.getBoolean(context.getString(R.string.pref_active_key), false)

            if (!active) {
                if (photosDb != null)
                    photosDb!!.close()

                return
            }
        }

        running = true

        LocalBroadcastManager.getInstance(context).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                if (running) {
                    receivers[SECOND_RECEIVER_POSITION] = intent.getParcelableExtra("dialogreceiver")

                    //Release the previous reference to the Activity, so that it can be
                    //garbage collected
                    if (receivers[RECEIVER_POSITION] != null) {
                        val bundle = Bundle()
                        bundle.putInt("releaseActivity", 0)

                        receivers[RECEIVER_POSITION]!!.send(Activity.RESULT_OK, bundle)
                    }

                    if (receivers[SECOND_RECEIVER_POSITION] != null) {
                        //reportTotal(total);
                        var bundle = Bundle()
                        bundle.putInt("total", total)

                        receivers[SECOND_RECEIVER_POSITION]!!.send(Activity.RESULT_OK, bundle)

                        //onProgressChanged(actual);
                        bundle = Bundle()
                        bundle.putInt("progress", actual)

                        receivers[SECOND_RECEIVER_POSITION]!!.send(Activity.RESULT_OK, bundle)
                    }
                }
            }
        }, IntentFilter(PhotosActivity.INTENT_QUERY_ACTION))

        LocalBroadcastManager.getInstance(context).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val sendIntent = Intent(PhotosActivity.PROGRESS_ACTION_SEND)
                sendIntent.putExtra(PhotosActivity.PROGRESS_KEY, actual)
                LocalBroadcastManager.getInstance(context).sendBroadcast(sendIntent)
            }
        }, IntentFilter(PhotosActivity.PROGRESS_ACTION_QUERY))

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val showNotif = sharedPreferences.getBoolean(context.getString(R.string.pref_shownotification_key), true)
        keepLargePhoto = sharedPreferences.getBoolean(context.getString(R.string.pref_keeplargephoto_key), true)

        if (onBackground) {
            //Si estamos ejecutando esto mientras el dispositivo se está cargando, y hay 0 fotos en la base
            //de datos (primer uso), buscamos fotos ya fechadas
            val db = DatabaseHelper(context).readableDatabase
            val cursor = db.rawQuery("SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " +
                    DatabaseHelper.TABLE_NAME, null)

            Log.d("TAG", "DBsize: " + cursor.count)

            if (!cursor.moveToFirst()) {
                cursor.close()

                if (showNotif)
                    mNotificationUtils!!.showStandAloneNotification("Buscando fotos ya fechadas...")

                Utils.searchForAlreadyProcessedPhotos(context)
            }
        }

        if (showNotif)
            mNotificationUtils!!.showSearchingPhotosNotification("Buscando fotos sin fechar...")

        var galleryImages = cameraImages ?: PhotoUtils(context).getCameraImages(Utils.getFoldersToProcess(context))

        galleryImages = Utils.getPhotosWithoutDate(context, galleryImages, photosDb)

        total = 0
        actual = 0
        total = galleryImages.size

        for (receiver in receivers) {
            if (receiver != null) {
                val bundle = Bundle()
                bundle.putInt("total", total)

                receiver.send(Activity.RESULT_OK, bundle)
            }
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dialogCancelled = intent.getBooleanExtra("dialogcancelled", false)
            }
        }, IntentFilter(PhotosActivity.INTENT_ACTION))

        LocalBroadcastManager.getInstance(context).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (onBackground)
                    cancelledCharger = intent.getBooleanExtra(CANCEL_SERVICE, false)
            }
        }, IntentFilter(ACTION_CANCEL_CHARGER_DISCONNECTED))

        if (showNotif)
            mNotificationUtils!!.showProgressNotification("Procesando fotos...")

        loop@ for (image in galleryImages) {
            if (!dialogCancelled && !cancelledCharger) {
                context.contentResolver.openInputStream(image.uri)?.let {
                    val options = BitmapFactory.Options()
                    it.use { inputStream ->
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
                    handlePhoto(context, image, options)
                }
            }

            actual++

            for (receiver in receivers) {
                if (receiver != null) {
                    val bundle = Bundle()
                    bundle.putInt("progress", actual)

                    receiver.send(Activity.RESULT_OK, bundle)
                }
            }

            if (showNotif)
                mNotificationUtils!!.setNotificationProgress(total, actual)
        }

        if (showNotif)
            mNotificationUtils!!.endProgressNotification("El proceso ha finalizado")

        running = false

        if (photosDb != null)
            photosDb!!.close()

        for (receiver in receivers) {
            if (receiver != null) {
                val bundle = Bundle()
                bundle.putString("end", "end")

                receiver.send(Activity.RESULT_OK, bundle)
            }
        }

        if (dialogCancelled)
            dialogCancelled = false
    }

    private fun handlePhoto(context: Context, image: Image, options: BitmapFactory.Options) {
        var imageWidth : Double = options.outWidth.toDouble()
        var imageHeight : Double = options.outHeight.toDouble()

        // We have to make sure our bitmap doesn't exceed maxMemory size
        // (Runtime.getRuntime().maxMemory()), taking into account each pixel is 4 bytes in size
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024
        val freeMemory = maxMemory - usedMemory

        var imageSize = (imageWidth * imageHeight * 4 / 1024).toLong()
        var wasLarge = false

        if (imageSize >= freeMemory) {
            wasLarge = true
        }

        keepLargePhoto = keepLargePhoto && wasLarge

        while (imageSize >= freeMemory) {
            imageWidth /= 1.15
            imageHeight /= 1.15
            imageSize = (imageWidth * imageHeight * 4 / 1024).toLong()
        }

        val previousSize = (options.outWidth * options.outHeight * 4 / 1024).toLong()
        val reduction = ceil(previousSize.toDouble() / imageSize).toInt()

        val options = BitmapFactory.Options()

        options.inMutable = true
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        if (wasLarge)
            options.inSampleSize = reduction

        var myBitmap = context.contentResolver.openInputStream(image.uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        if (myBitmap == null) {
            actual++

            for (receiver in receivers) {
                if (receiver != null) {
                    val bundle = Bundle()
                    bundle.putInt("progress", actual)

                    receiver.send(Activity.RESULT_OK, bundle)
                }
            }

            mNotificationUtils!!.setNotificationProgress(total, actual)

            registerPhotoIntoDb(image.toString())

            return
        }

        var date = ""
        var rotation = ExifInterface.ORIENTATION_NORMAL

        context.contentResolver.openInputStream(image.uri)?.use { imageStream ->
            try {
                val exifInterface = ExifInterface(imageStream)
                date = getExifTag(exifInterface, ExifInterface.TAG_DATETIME, image)
                rotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } ?: return

        if (isAlreadyDatestamped(myBitmap, rotation)) {
            return
        }

        var bitmap2: Bitmap? = writeDateOnBitmap(myBitmap, date, rotation)
        markAsAlreadyDatestamped(bitmap2, rotation)

        @Suppress("UNUSED_VALUE")
        myBitmap = null

        context.contentResolver.openInputStream(image.uri)?.use { inputStream ->
            if (Utils.overwritePhotos(context) && !keepLargePhoto) {
                try {
                    savePhoto(context, bitmap2, image.bucketName, image, true, true)
                } catch (e: SecurityException) {
                    NotificationUtils(context).showSAFPermissionNotification(image.bucketName)
                    return@use
                }
                if (shouldRegisterPhoto) {
                    registerPhotoIntoDb(image.toString())
                }
            } else {
                savePhoto(context, bitmap2, "Date To Photo", image, true, false)
                registerPhotoIntoDb(image.toString())
            }
        }

        @Suppress("UNUSED_VALUE")
        bitmap2 = null
    }

    private fun isAlreadyDatestamped(myBitmap: Bitmap?, rotation: Int): Boolean {
        val upperLeft = myBitmap!!.getPixel(0, 0)
        val upperRight = myBitmap.getPixel(myBitmap.width - 1, 0)
        val lowerLeft = myBitmap.getPixel(0, myBitmap.height - 1)
        val lowerRight = myBitmap.getPixel(myBitmap.width - 1, myBitmap.height - 1)

        var printWriter: PrintWriter? = null

        try {
            printWriter = PrintWriter(
                    BufferedWriter(
                            FileWriter(Environment.getExternalStorageDirectory().path
                                    + File.separator + "Download" + File.separator + "dtpcolors.txt")))

            if (Utils.getColor(upperLeft) == Color.BLUE)
                printWriter.println("Superior izquierda bien")
            else
                printWriter.println("Superior izquierda mal, el color era " + Utils.getColorName(
                        Utils.getColor(upperLeft)
                ))

            if (Utils.getColor(upperRight) == Color.RED)
                printWriter.println("Superior derecha bien")
            else
                printWriter.println("Superior derecha mal, el color era " + Utils.getColorName(
                        Utils.getColor(upperRight)
                ))

            if (Utils.getColor(lowerLeft) == Color.GREEN)
                printWriter.println("Inferior izquierda bien")
            else
                printWriter.println("Inferior izquierda mal, el color era " + Utils.getColorName(
                        Utils.getColor(lowerLeft)
                ))

            if (Utils.getColor(lowerRight) == Color.YELLOW)
                printWriter.println("Inferior derecha bien")
            else
                printWriter.println("Inferior derecha mal, el color era " + Utils.getColorName(
                        Utils.getColor(lowerRight)
                ))

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (printWriter != null)
                printWriter.close()
        }

        return false
    }

    private fun markAsAlreadyDatestamped(myBitmap: Bitmap?, rotation: Int) {

    }

    private fun getExifTag(exif: ExifInterface, tag: String, image: Image): String {
        // 2014:09:21 13:53:58
        val attribute = exif.getAttribute(tag)

        return Utils.getFormattedDate(if (attribute != null) {
            val year = Integer.parseInt(attribute.substring(0, 4))
            val month = Integer.parseInt(attribute.substring(5, 7))
            val day = Integer.parseInt(attribute.substring(8, 10))

            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day)

            cal.time
        } else Date(image.dateAdded * 1000))
    }

    private fun writeDateOnBitmap(b: Bitmap?, text: String, orientation: Int): Bitmap {
        val canvas = Canvas(b!!)

        paint!!.textSize = Math.min(b.width / 20, b.height / 20).toFloat()

        val bounds = Rect()
        paint!!.getTextBounds(text, 0, text.length, bounds)
        //int x = (b.getWidth() - bounds.width()) / 2;
        //int y = (b.getHeight() + bounds.height()) / 2;

        val marginWidth = b.width * 0.2 / 10
        val marginHeight = b.height * 0.2 / 10
        var x = (b.width.toDouble() - bounds.width().toDouble() - marginWidth).toInt()
        var y = (b.height - marginHeight).toInt()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                canvas.save()

                x = bounds.height() //altotexto
                y = (b.height.toDouble() - bounds.width().toDouble() - marginHeight).toInt() //alto - anchotexto

                canvas.rotate(-270f, x.toFloat(), y.toFloat())

                canvas.drawText(text, x.toFloat(), y.toFloat(), paint!!)

                canvas.restore()
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                canvas.save()

                /*x = b.getWidth() - bounds.height(); //ancho - altotexto
                y = (int) (bounds.width() + margin); //anchotexto*/

                x = b.width - bounds.height() //ancho - altotexto
                y = (bounds.width() + marginHeight).toInt() //anchotexto

                canvas.rotate(-90f, x.toFloat(), y.toFloat())


                canvas.drawText(text, x.toFloat(), y.toFloat(), paint!!)

                canvas.restore()
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                canvas.save()

                x = (bounds.width() + marginWidth).toInt()
                y = marginHeight.toInt()

                canvas.rotate(-180f, x.toFloat(), y.toFloat())

                canvas.drawText(text, x.toFloat(), y.toFloat(), paint!!)

                canvas.restore()
            }
            else -> {
                canvas.drawText(text, x.toFloat(), y.toFloat(), paint!!)
            }
        }

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> tempStr += "ORIENTATION_ROTATE_270"
            ExifInterface.ORIENTATION_TRANSVERSE -> tempStr += "ORIENTATION_TRANSVERSE"
            ExifInterface.ORIENTATION_TRANSPOSE -> tempStr += "ORIENTATION_TRANSPOSE"
            ExifInterface.ORIENTATION_ROTATE_90 -> tempStr += "ORIENTATION_ROTATE_90"
            ExifInterface.ORIENTATION_UNDEFINED -> tempStr += "ORIENTATION_UNDEFINED"
            ExifInterface.ORIENTATION_ROTATE_180 -> tempStr += "ORIENTATION_ROTATE_180"
            ExifInterface.ORIENTATION_NORMAL -> tempStr += "ORIENTATION_NORMAL"
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> tempStr += "ORIENTATION_FLIP_HORIZONTAL"
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> tempStr += "ORIENTATION_FLIP_VERTICAL"
        }

        tempStr += "\n"

        return b
    }

    @SuppressLint("InlinedApi")
    private fun findPhotoUri(context: Context, bucketName: String, name: String): Uri? {
        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME}=? AND ${MediaStore.Images.Media.DISPLAY_NAME}=?", arrayOf(bucketName, name), null)
        cursor?.use {
            if (cursor.moveToNext()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null
    }

    private fun savePhotoToUri(context: Context, imageUri: Uri, name: String, bmp: Bitmap) {
        context.contentResolver.openFileDescriptor(imageUri, "w").use { descriptor ->
            descriptor?.let { saveBitmapToOutputStream(FileOutputStream(descriptor.fileDescriptor), bmp, name) }
        }
    }

    private fun saveBitmapToOutputStream(out: OutputStream, bmp: Bitmap, name: String) {
        if (name.isJPEG()) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        } else {
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
    }

    private fun hasScopedStorage() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun savePhoto(context: Context, bmp: Bitmap?, bucketName: String, image: Image, keepOrientation: Boolean, overwritePhoto: Boolean) {
        if (overwritePhoto) {
            val uri = findPhotoUri(context, bucketName, image.name)
            if (uri != null && bmp != null) {
                savePhotoToUri(context, if (hasScopedStorage()) MediaStore.getDocumentUri(context, uri) else uri, image.name, bmp)
                shouldRegisterPhoto = true
            }
        } else {
            if (hasScopedStorage()) {
                savePhotoScopedStorage(context, image, bmp, bucketName, image.name, keepOrientation)
            } else {
                savePhotoLegacy(context, image, bucketName, bmp, keepOrientation)
            }
        }
    }

    @SuppressLint("InlinedApi")
    @TargetApi(Build.VERSION_CODES.Q)
    fun savePhotoScopedStorage(context: Context, image: Image, bmp: Bitmap?, bucketName: String, name: String, keepOrientation: Boolean) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, name)
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.DESCRIPTION, name)
            put(MediaStore.Images.Media.MIME_TYPE, if (name.isJPEG()) "image/jpeg" else "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$bucketName/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return
        savePhotoToUri(context, imageUri, name, bmp!!)
        shouldRegisterPhoto = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(imageUri, values, null, null)
        }

        context.contentResolver.openInputStream(imageUri)?.use { imageStream ->
            moveEXIFData(context, image, imageStream, keepOrientation)
        }
    }

    fun savePhotoLegacy(context: Context, image: Image, bucketName: String, bmp: Bitmap?, keepOrientation: Boolean) {
        val basePath = Environment.getExternalStorageDirectory().path + "/" + bucketName
        val baseFile = File(basePath)
        if (!baseFile.exists()) {
            baseFile.mkdirs()
        }
        val imageFile = File(baseFile, image.name)
        try {
            bmp?.let {
                val out = FileOutputStream(imageFile)
                saveBitmapToOutputStream(out, bmp, image.name)
                out.flush()
                out.close()
                moveEXIFData(context, image, imageFile, keepOrientation)
                scanPhoto(imageFile.toString())
                shouldRegisterPhoto = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun String.isJPEG() = toLowerCase(Locale.ROOT).endsWith(".jpg") || toLowerCase(Locale.ROOT).endsWith(".jpeg")

    private fun moveEXIFData(context: Context, image: Image, fileTo: File, keepOrientation: Boolean) {
        try {
            val exifFrom = context.contentResolver.openInputStream(image.uri)?.use {
                ExifInterface(it)
            } ?: return
            val exifTo = ExifInterface(fileTo.absolutePath)
            moveEXIFData(image, exifFrom, exifTo, keepOrientation)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun moveEXIFData(context: Context, image: Image, inputStreamTo: InputStream, keepOrientation: Boolean) {
        try {
            val exifFrom = context.contentResolver.openInputStream(image.uri)?.use {
                ExifInterface(it)
            } ?: return
            val exifTo = ExifInterface(inputStreamTo)
            moveEXIFData(image, exifFrom, exifTo, keepOrientation)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun moveEXIFData(image: Image, exifFrom: ExifInterface, exifTo: ExifInterface, keepOrientation: Boolean) {
        if (exifFrom.getAttribute(ExifInterface.TAG_DATETIME) != null)
            exifTo.setAttribute(ExifInterface.TAG_DATETIME, exifFrom.getAttribute(ExifInterface.TAG_DATETIME))
        else {
            val lastModDate = Date(image.dateAdded)
            val cal = Calendar.getInstance()
            cal.time = lastModDate
            val year = getStringOfNumber(cal.get(Calendar.YEAR))
            val month = getStringOfNumber(cal.get(Calendar.MONTH) + 1)
            val day = getStringOfNumber(cal.get(Calendar.DAY_OF_MONTH))
            val hours = getStringOfNumber(cal.get(Calendar.HOUR))
            val minutes = getStringOfNumber(cal.get(Calendar.MINUTE))
            val seconds = getStringOfNumber(cal.get(Calendar.SECOND))
            exifTo.setAttribute(ExifInterface.TAG_DATETIME, "$year:$month:$day $hours:$minutes:$seconds")
        }
        if (exifFrom.getAttribute(ExifInterface.TAG_FLASH) != null)
            exifTo.setAttribute(ExifInterface.TAG_FLASH, exifFrom.getAttribute(ExifInterface.TAG_FLASH))
        if (exifFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) != null)
            exifTo.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exifFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exifFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exifFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exifFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exifFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exifFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
        if (exifFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD) != null)
            exifTo.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exifFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD))
        if (exifFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null)
            exifTo.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, exifFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH))
        if (exifFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null)
            exifTo.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, exifFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH))
        if (exifFrom.getAttribute(ExifInterface.TAG_MAKE) != null) {
            if (Utils.overwritePhotos(context!!))
                exifTo.setAttribute(ExifInterface.TAG_MAKE, "dtp-" + exifFrom.getAttribute(ExifInterface.TAG_MAKE))
            else
                exifTo.setAttribute(ExifInterface.TAG_MAKE, exifFrom.getAttribute(ExifInterface.TAG_MAKE))
        } else {
            exifTo.setAttribute(ExifInterface.TAG_MAKE, "dtp-")
        }
        if (exifFrom.getAttribute(ExifInterface.TAG_MODEL) != null)
            exifTo.setAttribute(ExifInterface.TAG_MODEL, exifFrom.getAttribute(ExifInterface.TAG_MODEL))
        if (exifFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE) != null)
            exifTo.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exifFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE))
        if (keepOrientation && exifFrom.getAttribute(ExifInterface.TAG_ORIENTATION) != null)
            exifTo.setAttribute(ExifInterface.TAG_ORIENTATION, exifFrom.getAttribute(ExifInterface.TAG_ORIENTATION))
        else
            exifTo.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED.toString())

        exifTo.saveAttributes()
    }

    fun scanPhoto(imageFileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(
                    DateToPhoto.instance,
                    arrayOf(imageFileName), null
            ) { _, _ -> }
        } else {
            context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://$imageFileName")))
        }
    }

    private fun getStringOfNumber(number: Int): String {
        val numberString: String
        if (number < 10) {
            numberString = "0" + number.toString()
        } else {
            numberString = number.toString()
        }

        return numberString
    }

    private fun registerPhotoIntoDb(path: String) {
        if (photosDb == null || !photosDb!!.isOpen) {
            photosDb = DatabaseHelper(context!!).writableDatabase
        }

        val values = ContentValues()
        values.put(DatabaseHelper.PATH_COLUMN, path)

        photosDb!!.insert(DatabaseHelper.TABLE_NAME, null, values)
    }

    /**
     * Este método obtiene la orientación de la foto con la otra interfaz EXIF y escribe otra línea
     * en el archivo de logs con la información
     *
     * @param imgPath ruta de la imagen para obtener su orientación de EXIF
     */
    private fun testEXIFDate(imgPath: String) {
        val exifInterface = fergaral.datetophoto.exif.ExifInterface()

        try {
            exifInterface.readExif(imgPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val orientationObj = exifInterface.getTagValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION)
        var orientation = fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT //ExifInterface.ORIENTATION_NORMAL

        if (orientationObj != null)
            orientation = exifInterface.getTagIntValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION)!!.toShort()

        tempStr += imgPath + " -> "

        when (orientation) {
            fergaral.datetophoto.exif.ExifInterface.Orientation.BOTTOM_LEFT -> tempStr += "BOTTOM_LEFT"
            fergaral.datetophoto.exif.ExifInterface.Orientation.BOTTOM_RIGHT -> tempStr += "BOTTOM_RIGHT"
            fergaral.datetophoto.exif.ExifInterface.Orientation.LEFT_BOTTOM -> tempStr += "LEFT_BOTTOM"
            fergaral.datetophoto.exif.ExifInterface.Orientation.LEFT_TOP -> tempStr += "LEFT_TOP"
            fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_BOTTOM -> tempStr += "RIGHT_BOTTOM"
            fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_TOP -> tempStr += "RIGHT_TOP"
            fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT -> tempStr += "TOP_LEFT"
            fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_RIGHT -> tempStr += "TOP_RIGHT"
        }

        tempStr += "\n"
    }

    companion object {
        private const val LOG = true
        const val ACTION_CANCEL_CHARGER_DISCONNECTED = "cancel_charger_disconnected"
        const val CANCEL_SERVICE = "cancel"
        private const val RECEIVER_POSITION = 0
        private const val SECOND_RECEIVER_POSITION = 1
    }
}

package fergaral.datetophoto.utils

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ResultReceiver
import android.preference.PreferenceManager
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.ArrayList
import java.util.Calendar
import java.util.Date

import fergaral.datetophoto.R
import fergaral.datetophoto.activities.PhotosActivity
import fergaral.datetophoto.db.DatabaseHelper

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
    private val mNotifStartTime: Long = 0
    private var running: Boolean = false
    private var total: Int = 0
    private var actual: Int = 0
    private var scale: Float = 0.toFloat()
    private var paint: Paint? = null
    private var photosDb: SQLiteDatabase? = null
    private var mNotificationUtils: NotificationUtils? = null
    private var printWriter: PrintWriter? = null
    private var shouldRegisterPhoto: Boolean = false
    private var context: Context? = null

    fun execute(context: Context) {
        execute(null, true, null, context)
    }

    fun execute(resultReceiver: ResultReceiver?, onBackground: Boolean,
                cameraImages: ArrayList<String>?, context: Context) {
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
        var keepLargePhoto = sharedPreferences.getBoolean(context.getString(R.string.pref_keeplargephoto_key), true)

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

        var galleryImages: List<String>?

        if (cameraImages != null) {
            galleryImages = cameraImages
        } else {
            galleryImages = getCameraImages(context)
        }

        if (galleryImages != null) {
            galleryImages = Utils.getPhotosWithoutDate(context, Utils.getImagesToProcess(context, galleryImages), photosDb)
        }

        if (galleryImages != null) {
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

            for (s in galleryImages) {
                var imgFile: File? = File(s)
                if (imgFile!!.exists() && !dialogCancelled && !cancelledCharger) {

                    if (LOG) {
                        try {
                            val dirFile = File(Environment.getExternalStorageDirectory().path
                                    + File.separator + "Download")

                            if (!dirFile.exists())
                                dirFile.mkdirs()

                            printWriter = PrintWriter(FileWriter(dirFile.path
                                    + File.separator + "dtptimes.txt"))
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }

                    val startTime = System.currentTimeMillis()

                    //if (!Utils.isAlreadyDatestamped(imgFile)) {


                    //File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

                    //if (!imgFileWithDate.exists() && Utils.processSelectedFolder(this, PhotoUtils.getParentFolderName(s))) {

                    //Primero obtenemos ancho y alto de la imagen
                    var options = BitmapFactory.Options()

                    //Al establecerlo a true, el decodificador retornará null, pero options tendrá el ancho y alto
                    //de ese bitmap
                    options.inJustDecodeBounds = true

                    BitmapFactory.decodeFile(imgFile.absolutePath, options)

                    var imageWidth : Double = options.outWidth.toDouble()
                    var imageHeight : Double = options.outHeight.toDouble()

                    //Hay que asegurarse de que nuestro bitmap no exceda el tamaño de maxMemory
                    // (Runtime.getRuntime().maxMemory()), teniendo en cuenta que cada píxel ocupa 4 bytes
                    val maxMemory = Runtime.getRuntime().maxMemory() / 1024
                    val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024
                    val freeMemory = maxMemory - usedMemory

                    var imageSize = (imageWidth * imageHeight * 4 / 1024).toLong() //Este valor era 12 unidades menos que el que debía ser
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
                    //double reduction = (double)previousSize / imageSize;
                    val reduction = Math.ceil(previousSize.toDouble() / imageSize).toInt()

                    options = BitmapFactory.Options()

                    options.inMutable = true
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888

                    if (wasLarge)
                        options.inSampleSize = reduction

                    val startTimeDecode = System.currentTimeMillis()

                    var myBitmap: Bitmap? = BitmapFactory.decodeFile(imgFile.absolutePath, options)

                    val timeDecode = System.currentTimeMillis() - startTimeDecode //En ms
                    val timeElapsedDecode = timeDecode / 1000.0

                    printWriter!!.println("decode image: $timeElapsedDecode")

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

                        //Si la imagen no pudo ser decodificada, le añadimos dtp- al nombre para no volver a procesarla
                        imgFile.renameTo(File(
                                imgFile.parentFile.absolutePath + "/" + "dtp-" + imgFile.name))

                        scanPhoto(imgFile.parentFile.absolutePath + "/" + imgFile.name.substring(4))

                        continue
                    }

                    tempStr += "$s -> "

                    var date = ""
                    var rotation = ExifInterface.ORIENTATION_NORMAL

                    try {
                        val exifInterface = ExifInterface(imgFile.absolutePath)
                        date = getExifTag(exifInterface, ExifInterface.TAG_DATETIME, imgFile)
                        rotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    val startTimeWriteDate = System.currentTimeMillis()

                    if (isAlreadyDatestamped(myBitmap, rotation)) {
                        continue
                    }

                    var bitmap2: Bitmap? = writeDateOnBitmap(myBitmap, date, rotation)

                    val endTimeWriteDate = System.currentTimeMillis() - startTimeWriteDate
                    val elapsedTimeWriteDate = endTimeWriteDate / 1000.0

                    printWriter!!.println("write date: " + elapsedTimeWriteDate)

                    //Ahora la marcamos como ya fechada
                    markAsAlreadyDatestamped(bitmap2, rotation)

                    //Este método sirve para comprobar la rotación con la segunda interfaz EXIF
                    //testEXIFDate(imgFile.getAbsolutePath());

                    @Suppress("UNUSED_VALUE")
                    myBitmap = null

                    /*if(Utils.overwritePhotos(this)) {
                                //CapturePhotoUtils.insertImage(getContentResolver(), bitmap2, imgFile.getName() + "-dtp.jpg", "generated using Date To Photo");
                                savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtpo-" + imgFile.getName(), imgFile, true
                                );
                            }else{
                                savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtp-" + imgFile.getName(), imgFile, true
                                );
                            }*/

                    val startTimeSavePhoto = System.currentTimeMillis()

                    if (Utils.overwritePhotos(context) && !keepLargePhoto) {
                        savePhoto(bitmap2, imgFile.parentFile.absolutePath, "dtpo-" + imgFile.name, imgFile, true
                        )

                        if (shouldRegisterPhoto)
                            registerPhotoIntoDb(imgFile.absolutePath)
                    } else {
                        if (imgFile.parentFile.name == "Date To Photo originals") {
                            val nameParts = imgFile.name.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                            val photoFolder = nameParts[0]
                            val photoName = nameParts[1]

                            val folderFile = Folders.get(photoFolder)

                            savePhoto(bitmap2, folderFile!!.absolutePath, "dtpo-" + photoName,
                                    imgFile, true)

                            registerPhotoIntoDb(File(folderFile.absolutePath,
                                    imgFile.name).path)
                        } else {
                            val originalsFile = File(Environment.getExternalStorageDirectory().path + "/Date To Photo originals")

                            if (!originalsFile.exists())
                                originalsFile.mkdir()

                            //Guardamos la foto original en la carpeta de originales
                            val originalPhotoFile = File(originalsFile.path, imgFile.parentFile.name
                                    + "-" +
                                    imgFile.name)

                            try {
                                PhotoUtils.copy(imgFile,
                                        originalPhotoFile)
                            } catch (e: IOException) {
                                Log.e("TAG", "Error while saving image copy")
                            }

                            savePhoto(bitmap2, imgFile.parentFile.absolutePath, "dtpo-" + imgFile.name, imgFile, true
                            )

                            registerPhotoIntoDb(imgFile.absolutePath)
                            scanPhoto(originalPhotoFile.path)
                        }
                    }

                    val endTimeSavePhoto = System.currentTimeMillis() - startTimeSavePhoto
                    val elapsedTimeSavePhoto = endTimeSavePhoto / 1000.0

                    printWriter!!.println("save photo: $elapsedTimeSavePhoto")

                    @Suppress("UNUSED_VALUE")
                    bitmap2 = null

                    /* if(Utils.overwritePhotos(this))
                            {
                                PhotoUtils.deletePhoto(this, s);
                            }*/
                    //}
                    //}

                    val endTime = System.currentTimeMillis() - startTime
                    val elapsedEndTime = endTime / 1000.0

                    printWriter!!.println("total: $elapsedEndTime")

                    val retrieveDB = Utils.getPhotosWithoutDate2(context, galleryImages, photosDb)
                    printWriter!!.println("retrieve db: $retrieveDB")

                    printWriter!!.close()
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

                @Suppress("UNUSED_VALUE")
                imgFile = null
            }
        }

        Utils.write(Environment.getExternalStorageDirectory().path + "/Download/datetophotoimages.txt", tempStr)

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

    private fun getExifTag(exif: ExifInterface, tag: String, imgFile: File?): String {
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

            val year = Integer.parseInt(attribute.substring(0, 4))
            val month = Integer.parseInt(attribute.substring(5, 7))
            val day = Integer.parseInt(attribute.substring(8, 10))

            //return day + "/" + month + "/" + year;

            //Return a localized date String
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day)

            return Utils.getFormattedDate(cal.time)
        }

        return Utils.getFormattedDate(Date(imgFile!!.lastModified()))
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

    fun getCameraImages(context: Context): ArrayList<String>? {
        // Set up an array of the Thumbnail Image ID column we want
        val projection = arrayOf(MediaStore.Images.Media.DATA)


        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null)

        var result: ArrayList<String>? = null

        if (cursor != null) {
            result = ArrayList(cursor.count)


            if (cursor.moveToFirst()) {
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                do {
                    val data = cursor.getString(dataColumn)
                    //Log.i("data :", data);
                    result.add(data)
                } while (cursor.moveToNext())
            }
            cursor.close()

            //String uri3 = result.get(2);

            /*File imgFile = new File(uri3);
       if(imgFile.exists()) {
           Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
           iv1.setImageBitmap(myBitmap);
       }*/
        } else {
            val cursor1 = context.contentResolver.query(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    projection, null, null, null)

            if (cursor1 != null) {
                result = ArrayList(cursor1.count)


                if (cursor1.moveToFirst()) {
                    val dataColumn = cursor1.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    do {
                        val data = cursor1.getString(dataColumn)
                        //Log.i("data :", data);
                        result.add(data)
                    } while (cursor1.moveToNext())
                }
                cursor1.close()
            } else {
                result = null
            }
        }
        return result
    }

    fun savePhoto(bmp: Bitmap?, basePath: String, name: String, imageFrom: File?, keepOrientation: Boolean) {
        shouldRegisterPhoto = true
        val imageFileFolder = File(basePath)

        var out: FileOutputStream?
        val imageFileName = File(imageFileFolder, name)
        try {
            out = FileOutputStream(imageFileName)

            val compressStartTime = System.currentTimeMillis()

            bmp!!.compress(Bitmap.CompressFormat.JPEG, 90, out)

            val compressTimeMillis = System.currentTimeMillis() - compressStartTime
            val compressTime = compressTimeMillis / 1000.0
            printWriter!!.println("compress image: $compressTime")

            out.flush()
            out.close()

            val moveEXIFStartTime = System.currentTimeMillis()

            moveEXIFdata(imageFrom, imageFileName, keepOrientation)

            val moveEXIFTimeMillis = System.currentTimeMillis() - moveEXIFStartTime
            val moveEXIFTime = moveEXIFTimeMillis / 1000.0
            printWriter!!.println("move exif: $moveEXIFTime")

            val renameStartTime = System.currentTimeMillis()

            if (imageFileName.name.startsWith("dtpo-")) { //Si se cumple, sobreescribimos
                val previousFile = File(imageFileName.toString())
                val originalFile = File(imageFileName.parentFile.absolutePath +
                        "/" + imageFileName.name.substring(5))

                //Check if copy is corrupted
                var renamed = false
                if (!PhotoUtils.isCorrupted(imageFileName)) {
                    renamed = imageFileName.renameTo(
                            originalFile)
                }

                if (previousFile.exists())
                    previousFile.delete()

                scanPhoto(previousFile.toString())

                shouldRegisterPhoto = renamed
            }

            val renameTimeMillis = System.currentTimeMillis() - renameStartTime
            val renameTime = renameTimeMillis / 1000.0
            printWriter!!.println("rename: $renameTime")

            val scanPhotoStartTime = System.currentTimeMillis()

            scanPhoto(imageFileName.toString())

            val scanPhotoTimeMillis = System.currentTimeMillis() - scanPhotoStartTime
            val scanPhotoTime = scanPhotoTimeMillis / 1000.0
            printWriter!!.println("scan photo: $scanPhotoTime")

            @Suppress("UNUSED_VALUE")
            out = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun moveEXIFdata(imageFrom: File?, imageTo: File, keepOrientation: Boolean) {
        try {
            val exifInterfaceFrom = ExifInterface(imageFrom!!.absolutePath)
            val exifInterfaceTo = ExifInterface(imageTo.absolutePath)

            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME))
            else {
                val lastModDate = Date(imageFrom.lastModified())
                val cal = Calendar.getInstance()
                cal.time = lastModDate
                val year = getStringOfNumber(cal.get(Calendar.YEAR))
                val month = getStringOfNumber(cal.get(Calendar.MONTH) + 1)
                val day = getStringOfNumber(cal.get(Calendar.DAY_OF_MONTH))
                val hours = getStringOfNumber(cal.get(Calendar.HOUR))
                val minutes = getStringOfNumber(cal.get(Calendar.MINUTE))
                val seconds = getStringOfNumber(cal.get(Calendar.SECOND))
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, "$year:$month:$day $hours:$minutes:$seconds")
            }
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FLASH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE) != null) {
                if (Utils.overwritePhotos(context!!))
                    exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, "dtp-" + exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE))
                else
                    exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE))
            } else {
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, "dtp-")
            }
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MODEL, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE))
            if (keepOrientation && exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_ORIENTATION, exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION))
            else
                exifInterfaceTo.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED.toString())

            exifInterfaceTo.saveAttributes()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun scanPhoto(imageFileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(
                    context!!.applicationContext,
                    arrayOf(imageFileName), null
            ) { _, _ -> }
        } else {
            context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + imageFileName)))
        }
    }

    fun moveEXIFdata(imageFrom: File, imageTo: File) {
        try {
            val exifInterfaceFrom = ExifInterface(imageFrom.absolutePath)
            val exifInterfaceTo = ExifInterface(imageTo.absolutePath)
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME))
            else {
                val lastModDate = Date(imageFrom.lastModified())
                val cal = Calendar.getInstance()
                cal.time = lastModDate
                val year = getStringOfNumber(cal.get(Calendar.YEAR))
                val month = getStringOfNumber(cal.get(Calendar.MONTH) + 1)
                val day = getStringOfNumber(cal.get(Calendar.DAY_OF_MONTH))
                val hours = getStringOfNumber(cal.get(Calendar.HOUR_OF_DAY))
                val minutes = getStringOfNumber(cal.get(Calendar.MINUTE))
                val seconds = getStringOfNumber(cal.get(Calendar.SECOND))
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, "$year:$month:$day $hours:$minutes:$seconds")
            }
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FLASH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MODEL, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE))
            if (exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_ORIENTATION, exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION))
            exifInterfaceTo.saveAttributes()
        } catch (e: IOException) {
            e.printStackTrace()
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

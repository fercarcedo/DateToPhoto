package fergaral.datetophoto.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.loader.content.CursorLoader
import fergaral.datetophoto.R
import fergaral.datetophoto.algorithms.ColorAPIPhotoProcessedAlgorithm
import fergaral.datetophoto.algorithms.PhotoProcessedAlgorithm
import fergaral.datetophoto.algorithms.VisionAPIPhotoProcessedAlgorithm
import fergaral.datetophoto.db.DatabaseHelper
import fergaral.datetophoto.listeners.ProgressChangedListener
import fergaral.datetophoto.services.ProcessPhotosService
import fergaral.datetophoto.services.ProcessPhotosURIService
import fergaral.datetophoto.services.TestDatestampDetectionAlgorithmService
import fergaral.datetophoto.tasks.SearchForAlreadyProcessedPhotosTask
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and

/**
 * Created by Parejúa on 30/03/2015.
 */
object Utils {

    private val PERMISSION_NEVER_ASKED_KEY = "permissionneverasked"

    fun write(path: String, text: String) {

        try {

            val file = File(path)
            val filewriter = FileWriter(file)
            val bufferedwriter = BufferedWriter(filewriter)
            val printwriter = PrintWriter(bufferedwriter)
            printwriter.write(text)

            bufferedwriter.close()
            printwriter.close()
        } catch (e: Exception) {


        }

    }

    fun getFoldersToProcess(context: Context?): Array<String> {
        if (context == null)
            return arrayOf("")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val foldersProcessKey = context.getString(R.string.pref_folderstoprocess_key)
        return prefs.getStringSet(foldersProcessKey, mutableSetOf(""))!!.toTypedArray()
    }

    fun overwritePhotos(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.pref_overwrite_key), true)
    }

    fun getPhotosWithoutDate(context: Context, photos: ArrayList<Image>, db: SQLiteDatabase?): ArrayList<Image> {
        var database = db
        val firstTime = System.currentTimeMillis()
        var s = ""
        var startTime = System.currentTimeMillis()

        val photosMap = HashMap<String, Boolean>()
        var numPhotos = 0

        var elapsedTime = System.currentTimeMillis() - startTime
        s += "Creating ArrayList: $elapsedTime\n"

        startTime = System.currentTimeMillis()

        for (photo in photos) {
            photosMap[photo.toString()] = false //false significa que la foto no está fechada
        }

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Filling HashMap: $elapsedTime\n"

        //Comprobamos si el nombre de la imagen está en la base de datos
        if (database == null || !database.isOpen)
            database = DatabaseHelper(context).readableDatabase

        val searchQuery = "SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " + DatabaseHelper.TABLE_NAME

        startTime = System.currentTimeMillis()

        val cursor = database!!.rawQuery(searchQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val imageName = cursor.getString(0)

                if (photosMap.containsKey(imageName)) {
                    photosMap[imageName] = true
                    numPhotos++
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Iterating database: $elapsedTime\n"

        startTime = System.currentTimeMillis()

        for (image in photos) {
            if (PhotoUtils.incorrectFormat(image)) {
                photosMap[image.toString()] = true
                numPhotos++
            } else {
                if (image.name.startsWith("dtp-")) {
                    photosMap[image.toString()] = true
                    numPhotos++
                } else if (photosMap.containsKey("${image.bucketName}/dtp-${image.name}")) {
                    photosMap[image.toString()] = true
                    numPhotos++
                }
            }
        }

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Final for loop O(n): $elapsedTime\n"

        startTime = System.currentTimeMillis()

        val photosWithoutDate = ArrayList<Image>(numPhotos)

        for (photo in photos) {
            if (photo.bucketName == "Date To Photo originals") {
                //Miramos a ver si hay una foto con su mismo nombre
                val nameParts = photo.name.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val folderName = nameParts[0] //Si está fechada, estará en esta carpeta
                val photoName = nameParts[1] //Si está fechada, tendrá este nombre

                if ((!photosMap.containsKey(photo.toString()) || !photosMap[photo.toString()]!!) && !photosMap.containsKey("$folderName/$photoName"))
                    photosWithoutDate.add(photo)
            } else if (photosMap.containsKey(photo.toString()) && !photosMap[photo.toString()]!!) {
                photosWithoutDate.add(photo)
                Log.d("TAGP", photo.toString())
            }
        }

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Adding items to ArrayList: $elapsedTime\n"

        val elapsedTimeTotal = System.currentTimeMillis() - firstTime
        s += "TOTAL: $elapsedTimeTotal"

        Utils.write(Environment.getExternalStorageDirectory().path + File.separator + "Download"
                + File.separator + "dtptimephotowithoutdate.txt", s)

        return photosWithoutDate
    }

    fun getImagesToProcess(context: Context): ArrayList<Image> = PhotoUtils(context).getCameraImages(getFoldersToProcess(context))

    fun containsInteger(integer: Int?, integers: Array<Int>): Boolean {
        for (element in integers) {
            if (integer == element)
                return true
        }

        return false
    }

    fun getRealPathFromURI(context: Context?, contentUri: Uri?): String? {

        if (context == null || contentUri == null)
            return null

        val proj = arrayOf(MediaStore.Images.Media.DATA)

        val cursorLoader = CursorLoader(context, contentUri, proj, null, null, null)
        val cursor = cursorLoader.loadInBackground()

        val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        val hasResult = cursor.moveToFirst()

        if (hasResult) {
            val result = cursor.getString(columnIndex)
            cursor.close()
            return result
        } else {
            cursor.close()
            return null
        }
    }

    fun startProcessPhotosService(context: Context, listener: ProgressChangedListener, selectedPaths: ArrayList<Image>?) {
        //Lanzamos el servicio con las imágenes seleccionadas
        val powerManager = context.getSystemService(Activity.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ProcessPhotosWakeLock")

        val myReceiver = MyResultReceiver(Handler())
        myReceiver.setReceiver(listener)
        myReceiver.wakeLock = wakeLock

        val intent = Intent(context, ProcessPhotosService::class.java)
        intent.putExtra("receiver", myReceiver)
        intent.putExtra("onBackground", false)

        if (selectedPaths != null)
            intent.putParcelableArrayListExtra("cameraimages", selectedPaths)

        context.startService(intent)
    }

    fun startProcessPhotosURIService(context: Context, listener: ProgressChangedListener, selectedPaths: ArrayList<Image>?) {
        //Lanzamos el servicio con las imágenes seleccionadas
        val powerManager = context.getSystemService(Activity.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ProcessPhotosWakeLock")

        val myReceiver = MyResultReceiver(Handler())
        myReceiver.setReceiver(listener)
        myReceiver.wakeLock = wakeLock

        val intent = Intent(context, ProcessPhotosURIService::class.java)
        intent.putExtra("receiver", myReceiver)
        intent.putExtra("onBackground", false)

        if (selectedPaths != null)
            intent.putParcelableArrayListExtra("cameraimages", selectedPaths)

        context.startService(intent)
    }

    /**
     * Si el número como long es igual al número como entero, entonces devuelve ese número como entero
     * Sino devuelve el siguiente entero
     *
     * @param value número a redondear
     */
    fun roundUp(value: Double): Int {
        return if (value == value.toInt().toDouble()) value.toInt() else value.toInt() + 1

    }

    @Throws(NoSuchAlgorithmException::class, IOException::class)
    fun hashFile(file: File, algorithm: String): String {
        val inputStream = FileInputStream(file)
        val digest = MessageDigest.getInstance(algorithm)

        val bytesBuffer = ByteArray(1024)
        var bytesRead = -1

        bytesRead = inputStream.read(bytesBuffer)

        while (bytesRead != -1) {
            digest.update(bytesBuffer, 0, bytesRead)
            bytesRead = inputStream.read(bytesBuffer)
        }

        val hashedBytes = digest.digest()

        return convertByteArrayToHexString(hashedBytes)
    }

    private fun convertByteArrayToHexString(arrayBytes: ByteArray): String {
        val stringBuffer = StringBuffer()
        for (i in arrayBytes.indices) {
            stringBuffer.append(Integer.toString((arrayBytes[i] and Integer.valueOf(0xff).toByte()) + 0x100, 16)
                    .substring(1))
        }

        return stringBuffer.toString()
    }

    @Throws(NoSuchAlgorithmException::class, IOException::class)
    fun generateMD5(file: File): String {
        return hashFile(file, "MD5")
    }

    fun searchForAlreadyProcessedPhotos(context: Context, listener: ProgressChangedListener) {
        val imagesToProcess = PhotoUtils(context).getCameraImages()
        SearchForAlreadyProcessedPhotosTask(listener, imagesToProcess, context).execute()
    }

    fun testVisionDatestampDetectionAlgorithm(context: Context) {
        testDatestampDetectionAlgorithm(context, VisionAPIPhotoProcessedAlgorithm())
    }

    fun testColorDatestampDetectionAlgorithm(context: Context) {
        testDatestampDetectionAlgorithm(context, ColorAPIPhotoProcessedAlgorithm())
    }

    private fun testDatestampDetectionAlgorithm(context: Context, algorithm: PhotoProcessedAlgorithm) {
        val intent = Intent(context, TestDatestampDetectionAlgorithmService::class.java)
        intent.putExtra(TestDatestampDetectionAlgorithmService.ALGORITHM_KEY, algorithm)
        context.startService(intent)
    }

    fun searchForAlreadyProcessedPhotos(context: Context) {
        val imagesToProcess = PhotoUtils(context).getCameraImages()

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
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        db.close()
    }

    fun containsEXIFMAKEdtp(path: String): Boolean {
        val exifInterface = fergaral.datetophoto.exif.ExifInterface()
        try {
            exifInterface.readExif(path)
            val exifMake = exifInterface.getTagStringValue(fergaral.datetophoto.exif.ExifInterface.TAG_MAKE)

            return exifMake != null && exifMake.startsWith("dtp-")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    fun startActivityCompat(activity: Activity?, intent: Intent?) {

        if (activity == null || intent == null)
            return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
        } else {
            activity.startActivity(intent)
        }
    }

    fun lockOrientation(activity: Activity) {
        //Bloqueamos la rotación a vertical
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun unlockOrientation(activity: Activity) {
        //Desbloqueamos la rotación
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun showToast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun getFormattedDate(date: Date): String {
        val df = DateFormat.getDateInstance(DateFormat.SHORT)
        if (df is SimpleDateFormat) {
// To show Locale specific short date expression with full year
            val pattern = df.toPattern().replace("y+".toRegex(), "yyyy")
                    .replace("d+".toRegex(), "dd")
                    .replace("M+".toRegex(), "MM")
            df.applyPattern(pattern)
            return df.format(date)
        }

        return df.format(date)
    }

    fun permissionNeverAsked(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(Utils.PERMISSION_NEVER_ASKED_KEY, true)
    }

    fun setPermissionAsked(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putBoolean(Utils.PERMISSION_NEVER_ASKED_KEY, false)
        editor.apply()
    }

    fun getColor(rgb: Int): Int {
        return getColor(rgbToHSV(rgb))
    }

    fun rgbToHSV(rgb: Int): FloatArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(rgb, hsv)

        return hsv
    }

    fun getColor(hsb: FloatArray): Int {
        if (hsb[1] < 0.1 && hsb[2] > 0.9)
            return Color.WHITE
        else if (hsb[2] < 0.1)
            return Color.BLACK
        else {
            val deg = hsb[0] * 360
            return if (deg >= 0 && deg < 30)
                Color.RED
            else if (deg >= 30 && deg < 90)
                Color.YELLOW
            else if (deg >= 90 && deg < 150)
                Color.GREEN
            else if (deg >= 150 && deg < 210)
                Color.CYAN
            else if (deg >= 210 && deg < 270)
                Color.BLUE
            else if (deg >= 270 && deg < 330)
                Color.MAGENTA
            else
                Color.RED
        }
    }

    fun getColorName(color: Int): String {
        when (color) {
            Color.WHITE -> return "WHITE"
            Color.BLACK -> return "BLACK"
            Color.RED -> return "RED"
            Color.YELLOW -> return "YELLOW"
            Color.GREEN -> return "GREEN"
            Color.CYAN -> return "CYAN"
            Color.BLUE -> return "BLUE"
            Color.MAGENTA -> return "MAGENTA"
            else -> return "UNDEFINED"
        }
    }
}

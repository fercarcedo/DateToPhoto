package fergaral.datetophoto.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast

import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.LinkedList

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

        var foldersToProcessStr: String?
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val foldersProcessKey = context.getString(R.string.pref_folderstoprocess_key)
        try {
            foldersToProcessStr = prefs.getString(foldersProcessKey, "")
        } catch (e: ClassCastException) {
            foldersToProcessStr = ""
            prefs.edit().putString(foldersProcessKey, foldersToProcessStr).apply()
        }

        return foldersToProcessStr!!.split(FoldersListPreference.SEPARATOR.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
    }

    fun overwritePhotos(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.pref_overwrite_key), true)
    }


    /**
     * Method that returns the disk space usage by the photos with date and by the photos without date
     *
     * @return int[] array with the number of MB used by the photos with date and the number of MB used by the photos without date
     */
    fun getDiskSpaceUsage(context: Context): IntArray {
        val photoUtils = PhotoUtils(context)
        val cameraImages = photoUtils.cameraImages
        var nMBwithDate = 0
        var nMBwithoutDate = 0
        for (s in cameraImages) {
            val file = File(s)

            val sizeInBytes = file.length()
            val sizeInMb = sizeInBytes / (1024 * 1024)

            if (file.name.contains("dtp-")) {
                nMBwithDate += sizeInMb.toInt()
            } else {
                nMBwithoutDate += sizeInMb.toInt()
            }
        }

        return intArrayOf(nMBwithDate, nMBwithoutDate)
    }

    fun getNumberOfPhotosWithDate(paths: ArrayList<String>?): Int {
        if (paths == null)
            return 0

        var count = 0

        for (path in paths) {
            if (path.startsWith("dtp-"))
                count++
        }

        return count
    }

    fun getPhotosWithoutDate(photos: ArrayList<String>): ArrayList<String> {
        val photosWithoutDate = ArrayList<String>()

        for (path in photos) {
            val imgFile = File(path)

            if (!Utils.isAlreadyDatestamped(imgFile)) {
                val imgFileWithDate = File(imgFile.parentFile.absolutePath + "/dtp-" + imgFile.name)

                if (!imgFileWithDate.exists()) {
                    photosWithoutDate.add(path)
                }
            }
        }

        return photosWithoutDate
    }

    fun getPhotosWithoutDate(context: Context, photos: ArrayList<String>, db: SQLiteDatabase?): ArrayList<String> {
        var db = db
        val firstTime = System.currentTimeMillis()
        var s = ""
        var startTime = System.currentTimeMillis()

        val photosMap = HashMap<String, Boolean>()
        var numPhotos = 0

        var elapsedTime = System.currentTimeMillis() - startTime
        s += "Creating ArrayList: " + elapsedTime + "\n"

        startTime = System.currentTimeMillis()

        for (photo in photos) {
            photosMap.put(photo, false) //false significa que la foto no está fechada
        }

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Filling HashMap: " + elapsedTime + "\n"

        //Comprobamos si el nombre de la imagen está en la base de datos
        if (db == null || !db.isOpen)
            db = DatabaseHelper(context).readableDatabase

        val searchQuery = "SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " + DatabaseHelper.TABLE_NAME

        startTime = System.currentTimeMillis()

        val cursor = db!!.rawQuery(searchQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val imageName = cursor.getString(0)

                if (photosMap.containsKey(imageName)) {
                    photosMap.put(imageName, true)
                    numPhotos++
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Iterating database: " + elapsedTime + "\n"

        startTime = System.currentTimeMillis()

        for (image in photos) {
            val imageFile = File(image)
            Folders.add(imageFile.parentFile.name,
                    imageFile.parentFile)

            if (PhotoUtils.incorrectFormat(image)) {
                photosMap.put(image, true)
                numPhotos++
            } else {
                if (PhotoUtils.getName(image).startsWith("dtp-")) {
                    photosMap.put(image, true)
                    numPhotos++
                } else if (photosMap.containsKey(PhotoUtils.getFileWithDate(image))) {
                    photosMap.put(image, true)
                    numPhotos++
                }
            }
        }

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Final for loop O(n): " + elapsedTime + "\n"

        startTime = System.currentTimeMillis()

        val photosWithoutDate = ArrayList<String>(numPhotos)

        for (photo in photos) {
            val photoFile = File(photo)

            if (photoFile.parentFile.name == "Date To Photo originals") {
                //Miramos a ver si hay una foto con su mismo nombre
                val nameParts = photoFile.name.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                val folderName = nameParts[0] //Si está fechada, estará en esta carpeta
                val photoName = nameParts[1] //Si está fechada, tendrá este nombre

                val folderFile = Folders.get(folderName)

                if ((!photosMap.containsKey(photoFile.path) || !photosMap[photoFile.path]!!) && !photosMap.containsKey(File(folderFile!!.path, photoName).path))
                    photosWithoutDate.add(photo)
            } else if (photosMap.containsKey(photo) && !photosMap[photo]!!) {
                photosWithoutDate.add(photo)
                Log.d("TAGP", photo)
            }
        }

        elapsedTime = System.currentTimeMillis() - startTime
        s += "Adding items to ArrayList: " + elapsedTime + "\n"

        val elapsedTimeTotal = System.currentTimeMillis() - firstTime
        s += "TOTAL: " + elapsedTimeTotal

        Utils.write(Environment.getExternalStorageDirectory().path + File.separator + "Download"
                + File.separator + "dtptimephotowithoutdate.txt", s)

        return photosWithoutDate
    }

    fun getImagesToProcess(context: Context, photos: ArrayList<String>, folderName: String): ArrayList<String> {
        val photosMap = HashMap<String, Boolean>()
        var numPhotos = 0

        for (path in photos) {
            if (PhotoUtils.getParentFolderName(path) == folderName) {
                photosMap.put(path, true)
                numPhotos++
            }
        }

        val imagesToProcess = ArrayList<String>(numPhotos)

        for (path in photos) {
            if (photosMap.containsKey(path) && photosMap[path]!!)
                imagesToProcess.add(path)
        }

        return imagesToProcess
    }

    fun getImagesToProcess(context: Context, photos: List<String>): ArrayList<String> {
        val foldersToProcess = getFoldersToProcess(context)
        val allFolders = PhotoUtils.getFolders(context)
        val foldersMap = HashMap<String, Boolean>()
        var numFolders = 0

        for (folderName in allFolders) {
            val process = contains(foldersToProcess, folderName)
            foldersMap.put(folderName, process)

            if (process)
                numFolders++
        }

        val imagesToProcess = ArrayList<String>(numFolders)

        for (path in photos) {
            val folderName = PhotoUtils.getParentFolderName(path)
            if (foldersMap.containsKey(folderName) && foldersMap[folderName]!!)
                imagesToProcess.add(path)
        }

        return imagesToProcess
    }

    fun containsInteger(integer: Int?, integers: Array<Int>): Boolean {
        for (element in integers) {
            if (integer == element)
                return true
        }

        return false
    }

    fun dpToPixels(dp: Float, resources: Resources): Float {
        val scale = resources.displayMetrics.density

        return dp * scale + 0.5f
    }

    fun isAlreadyDatestamped(imgFile: File): Boolean {
        //Primero comprobamos si su extensión no es jpg o si su nombre empieza por dtp-, en cuyo caso la consideramos como fechada

        if (!(imgFile.name.endsWith(".jpg") || imgFile.name.endsWith(".jpeg")) || imgFile.name.startsWith("dtp-"))
            return true

        val imgFileWithDate = File(imgFile.parentFile.absolutePath + "/dtp-" + imgFile.name)

        return if (imgFileWithDate.exists()) true else false

    }

    /*public static boolean isAlreadyDatestamped(Context context, File imgFile, SQLiteDatabase photosDb) {
        //Primero comprobamos si su extensión no es jpg o si su nombre empieza por dtp-, en cuyo caso la consideramos como fechada

        if(!(imgFile.getName().endsWith(".jpg") || imgFile.getName().endsWith(".jpeg")) || imgFile.getName().startsWith("dtp-"))
            return true;

        File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

        if(imgFileWithDate.exists())
            return true;

        //Si la condición anterior fue falsa, entonces buscamos su nombre en la base de datos

        if(photosDb == null || !photosDb.isOpen())
            photosDb = new DatabaseHelper(context, DatabaseHelper.DB_NAME, null, 1).getReadableDatabase();

        Cursor cursor = photosDb.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NAME + " WHERE path='" + imgFile.getAbsolutePath() + "' ", null);

        //Comprobamos si la consulta proporcionó resultados (dará, como mucho, 1 resultado)
        //Si hubo 1 resultado, retornamos true, y si no hubo ningún resultado, retornamos false

        boolean result = cursor.moveToFirst();

        cursor.close();

        return result;
    }*/

    fun isAlreadyDatestamped(context: Context, imgFile: File, photosDb: SQLiteDatabase?): Boolean {
        var photosDb = photosDb
        //Primero comprobamos si su extensión no es jpg o png o si su nombre empieza por dtp-, en cuyo caso la consideramos como fechada

        if (!(imgFile.name.toLowerCase().endsWith(".jpg") || imgFile.name.toLowerCase().endsWith(".jpeg") ||
                imgFile.name.toLowerCase().endsWith(".png")) || imgFile.name.startsWith("dtp-"))
            return true

        val imgFileWithDate = File(imgFile.parentFile.absolutePath + "/dtp-" + imgFile.name)

        if (imgFileWithDate.exists())
            return true

        //Comprobamos si el nombre de la imagen está en la base de datos
        if (photosDb == null || !photosDb.isOpen)
            photosDb = DatabaseHelper(context).readableDatabase

        val searchQuery = "SELECT * FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.PATH_COLUMN + "=?"

        val cursor = photosDb!!.rawQuery(searchQuery, arrayOf(imgFile.absolutePath))
        val result = cursor.moveToFirst()
        cursor.close()

        return result
    }

    private fun isDataOnDb(tableName: String, dbFieldName: String, dbFieldValue: String, db: SQLiteDatabase): Boolean {
        val query = "Select * from $tableName where $dbFieldName = $dbFieldValue"

        val cursor = db.rawQuery(query, null)

        var result = false

        if (cursor.count > 0) {
            result = true
        }

        cursor.close()

        return result
    }

    fun landscape(context: Context?): Boolean {

        if (context == null)
            return false

        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val rotation = display.rotation

        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
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

    fun startProcessPhotosService(context: Context, listener: ProgressChangedListener, selectedPaths: ArrayList<String>?) {
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
            intent.putStringArrayListExtra("cameraimages", selectedPaths)

        context.startService(intent)
    }

    fun startProcessPhotosURIService(context: Context, listener: ProgressChangedListener, selectedPaths: ArrayList<String>?) {
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
            intent.putStringArrayListExtra("cameraimages", selectedPaths)

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
        val imagesToProcess = PhotoUtils(context).cameraImages
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
        val imagesToProcess = PhotoUtils(context).cameraImages

        val db = DatabaseHelper(context).writableDatabase

        var progress = 0

        for (path in imagesToProcess) {
            try {
                val exifInterface = ExifInterface(path)

                val makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE)

                if (makeExif != null && makeExif.startsWith("dtp-")) {
                    val values = ContentValues()
                    values.put(DatabaseHelper.PATH_COLUMN, path)

                    db.insert(DatabaseHelper.TABLE_NAME, null, values)
                }

                progress++
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

    fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
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

    fun getPhotosWithoutDate2(context: Context, photos: List<String>, db: SQLiteDatabase?): Double {
        var db = db
        val photosWithoutDate = LinkedList(photos)
        val startTimeMillis = System.currentTimeMillis()

        //Comprobamos si el nombre de la imagen está en la base de datos
        if (db == null || !db.isOpen)
            db = DatabaseHelper(context).readableDatabase

        val searchQuery = "SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " + DatabaseHelper.TABLE_NAME

        val cursor = db!!.rawQuery(searchQuery, null)
        if (cursor.moveToFirst()) {
            do {
                photosWithoutDate.remove(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()

        val result = LinkedList(photosWithoutDate)

        for (i in photosWithoutDate.indices) {
            val image = photosWithoutDate[i]
            val imgFile = File(image)
            if (!(imgFile.name.toLowerCase().endsWith(".jpg") || imgFile.name.toLowerCase().endsWith(".jpeg") ||
                    imgFile.name.toLowerCase().endsWith(".png")) || imgFile.name.startsWith("dtp-"))
                result.remove(image)
            else {
                val imgFileWithDate = File(imgFile.parentFile.absolutePath + "/dtp-" + imgFile.name)

                if (imgFileWithDate.exists())
                    result.remove(image)
            }
        }

        val endTimeMillis = System.currentTimeMillis() - startTimeMillis

        return endTimeMillis / 1000.0
    }

    private fun saveListStringToSharedPreferences(context: Context, list: List<String>) {
        /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(TAG, json);
        editor.commit();*/
    }

    private fun readListStringFromSharedPreferences(context: Context, list: List<String>) {
        /*
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(TAG, null);
        Type type = new TypeToken<ArrayList<ArrayObject>>() {}.getType();
        ArrayList<ArrayObject> arrayList = gson.from(json, type);
         */
    }

    private fun contains(array: Array<String>, value: String): Boolean {
        for (element in array) {
            if (element == value)
                return true
        }

        return false
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

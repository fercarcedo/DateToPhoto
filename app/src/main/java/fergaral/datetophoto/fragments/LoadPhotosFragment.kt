package fergaral.datetophoto.fragments

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment

import java.io.File
import java.util.ArrayList

import fergaral.datetophoto.db.DatabaseHelper
import fergaral.datetophoto.utils.PhotoUtils
import fergaral.datetophoto.utils.Utils

/**
 * Headless fragment which takes care of loading the photos
 * which haven't already been datestamped
 * Created by fer on 10/08/15.
 */
class LoadPhotosFragment : Fragment() {

    private var mCallback: TaskCallbacks? = null
    private var loadPhotosTask: ImagesToProcessTask? = null
    private var selectedFolder: String? = null
    private var mContext: Context? = null

    interface TaskCallbacks {
        fun onPreExecute()

        fun onPostExecute(result: ArrayList<String>)
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)

        //La Activity debe implementar TaskCallbacks
        mCallback = activity as TaskCallbacks?

        if (activity != null && mContext == null)
        //Solo lo asignamos una vez
            mContext = activity.applicationContext
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Mantenemos la instancia del Fragment durante cambios de configuración
        retainInstance = true

        loadPhotosTask = ImagesToProcessTask()
        loadPhotosTask!!.execute()
    }

    override fun onDetach() {
        super.onDetach()

        //Asignamos a la callback null, para evitar que el GC no
        //pueda recolectar la Activity en desuso (p.ej., durate una rotación)
        mCallback = null
    }

    inner class ImagesToProcessTask : AsyncTask<Void, Void, ArrayList<String>>() {
        override fun onPreExecute() {
            super.onPreExecute()

            if (mCallback != null)
                mCallback!!.onPreExecute()
        }

        override fun doInBackground(vararg voids: Void): ArrayList<String> {
            val photosDb = DatabaseHelper(mContext!!).readableDatabase
            val cameraImages = PhotoUtils(mContext!!).cameraImages
            //Obtenemos las fotos sin fechar de entre las que hay que procesar, ya que es más rápido identificar las que hay
            //que procesar, que no las que están sin fechars
            var imagesToProcess: ArrayList<String>

            val startTime = System.currentTimeMillis()
            var s = ""

            if (selectedFolder == null) {
                val startTimeImagProcess = System.currentTimeMillis()

                imagesToProcess = Utils.getImagesToProcess(mContext!!, cameraImages)

                val elapsedImagProcess = System.currentTimeMillis() - startTimeImagProcess

                s += "getImagesToProcess: " + elapsedImagProcess + "\n"

                val startTimeWithoutDate = System.currentTimeMillis()

                imagesToProcess = Utils.getPhotosWithoutDate(mContext!!,
                        imagesToProcess,
                        photosDb)

                val elapedTimeWithoutDate = System.currentTimeMillis() - startTimeWithoutDate

                s += "getPhotosWithoutDate: " + elapedTimeWithoutDate + "\n"
            } else {
                val startTimeImagProcess = System.currentTimeMillis()

                imagesToProcess = Utils.getImagesToProcess(mContext!!, cameraImages, selectedFolder!!)

                val elapsedImagProcess = System.currentTimeMillis() - startTimeImagProcess

                s += "getImagesToProcess: " + elapsedImagProcess + "\n"

                val startTimeWithoutDate = System.currentTimeMillis()

                imagesToProcess = Utils.getPhotosWithoutDate(mContext!!,
                        imagesToProcess,
                        photosDb)

                val elapedTimeWithoutDate = System.currentTimeMillis() - startTimeWithoutDate

                s += "getPhotosWithoutDate: " + elapedTimeWithoutDate + "\n"
            }

            val elapsedTime = System.currentTimeMillis() - startTime

            s += "TOTAL: " + elapsedTime + "\n"

            Utils.write(Environment.getExternalStorageDirectory().path + File.separator + "Download" + File.separator + "dtpload.txt",
                    s)

            photosDb.close()

            return imagesToProcess
        }

        override fun onPostExecute(imagesToProcess: ArrayList<String>) {
            super.onPostExecute(imagesToProcess)

            if (mCallback != null)
                mCallback!!.onPostExecute(imagesToProcess)
        }
    }

    fun refresh() {
        load(null)
    }

    fun load(folderName: String?) {
        if (loadPhotosTask != null && !loadPhotosTask!!.isCancelled) {
            //Cancelamos la AsyncTask (el onPostExecute no se ejecutará)
            loadPhotosTask!!.cancel(true)
        }

        loadPhotosTask = ImagesToProcessTask()
        selectedFolder = folderName
        loadPhotosTask!!.execute()
    }
}

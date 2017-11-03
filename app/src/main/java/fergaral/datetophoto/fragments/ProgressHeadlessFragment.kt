package fergaral.datetophoto.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import java.util.ArrayList

import fergaral.datetophoto.activities.PhotosActivity
import fergaral.datetophoto.listeners.ProgressChangedListener
import fergaral.datetophoto.utils.MyResultReceiver
import fergaral.datetophoto.utils.ProgressListener
import fergaral.datetophoto.utils.Utils

/**
 * Created by fer on 21/09/15.
 */
class ProgressHeadlessFragment : Fragment(), ProgressChangedListener {
    private var mListener: ProgressListener? = null
    private var selectedPaths: ArrayList<String>? = null
    private var total: Int = 0
    private var searchPhotos: Boolean = false
    private var shareAction: Boolean = false
    private var connectToRunningService: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Mantenemos la instancia del Fragment durante cambios de configuración
        retainInstance = true

        val arguments = arguments
        searchPhotos = arguments!!.getBoolean(PhotosActivity.SEARCH_PHOTOS_KEY, false)

        if (arguments.containsKey(PhotosActivity.SELECTED_PATHS_KEY))
            selectedPaths = arguments.getStringArrayList(PhotosActivity.SELECTED_PATHS_KEY)

        if (arguments.containsKey(PhotosActivity.ACTION_SHARE_KEY))
            shareAction = arguments.getBoolean(PhotosActivity.ACTION_SHARE_KEY, false)

        if (arguments.containsKey(PhotosActivity.CONNECT_TO_RUNNING_SERVICE_KEY))
            connectToRunningService = arguments.getBoolean(PhotosActivity.CONNECT_TO_RUNNING_SERVICE_KEY)

        Log.d("TAG", "containsSearch: " + arguments.containsKey(PhotosActivity.SEARCH_PHOTOS_KEY))
        Log.d("TAG", "containsSelectedPaths: " + arguments.containsKey(PhotosActivity.SELECTED_PATHS_KEY))
        Log.d("TAG", "selectedPaths!=null" + (selectedPaths != null))

        if (!connectToRunningService) {
            if (!searchPhotos) {
                if (!shareAction)
                    Utils.startProcessPhotosService(activity!!, this, selectedPaths)
                else
                    Utils.startProcessPhotosURIService(activity!!, this, selectedPaths)
            } else
                Utils.searchForAlreadyProcessedPhotos(activity!!, this)
        } else {
            //We need to provide the running service another listener by sending a broadcast
            val intent = Intent(PhotosActivity.INTENT_QUERY_ACTION)
            val resultReceiver = MyResultReceiver(Handler())
            resultReceiver.setReceiver(this)

            intent.putExtra("dialogreceiver", resultReceiver)

            LocalBroadcastManager.getInstance(activity!!).sendBroadcast(
                    intent
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (targetFragment is ProgressListener)
            mListener = targetFragment as ProgressListener?
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun reportTotal(total: Int) {
        if (mListener != null)
            mListener!!.reportTotal(total)

        this.total = total
    }

    override fun onProgressChanged(actual: Int) {
        val totalDouble = total.toDouble()
        val progress = actual / totalDouble * 100

        if (mListener != null)
            mListener!!.onProgressChanged(progress.toInt(), actual)
    }

    override fun reportEnd(fromActionShare: Boolean) {
        if (mListener != null)
            mListener!!.reportEnd(fromActionShare)

        fragmentManager!!
                .beginTransaction()
                .remove(this)
                .commitAllowingStateLoss()
    }

    companion object {

        val SELECTED_PATHS_KEY = "selectedPaths"
    }
}

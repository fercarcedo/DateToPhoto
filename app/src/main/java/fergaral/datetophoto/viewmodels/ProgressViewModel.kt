package fergaral.datetophoto.viewmodels

import android.content.Intent
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import fergaral.datetophoto.DateToPhoto
import fergaral.datetophoto.activities.PhotosActivity
import fergaral.datetophoto.listeners.ProgressChangedListener
import fergaral.datetophoto.utils.MyResultReceiver
import fergaral.datetophoto.utils.Utils

/**
 * Created by fer on 21/09/15.
 */
class ProgressViewModel : ViewModel(), ProgressChangedListener {
    sealed class ProgressResult {
        class Progress(val total: Int, val progressPercent: Int = 0, val progress: Int = 0): ProgressResult()
        class Done(val fromActionShare: Boolean, val searchPhotos: Boolean = false) : ProgressResult()
    }

    private var total: Int = 0
    private var searchPhotos = false
    val progressData = MutableLiveData<ProgressResult>()

    fun start(searchPhotos: Boolean = false,
              selectedPaths: ArrayList<String>? = null,
              shareAction: Boolean = false,
              connectToRunningService: Boolean = false
    ) {
        total = 0
        this.searchPhotos = searchPhotos
        if (!connectToRunningService) {
            if (!searchPhotos) {
                if (!shareAction)
                    Utils.startProcessPhotosService(DateToPhoto.instance, this, selectedPaths)
                else
                    Utils.startProcessPhotosURIService(DateToPhoto.instance, this, selectedPaths)
            } else
                Utils.searchForAlreadyProcessedPhotos(DateToPhoto.instance, this)
        } else {
            //We need to provide the running service another listener by sending a broadcast
            val intent = Intent(PhotosActivity.INTENT_QUERY_ACTION)
            val resultReceiver = MyResultReceiver(Handler())
            resultReceiver.setReceiver(this)

            intent.putExtra("dialogreceiver", resultReceiver)

            LocalBroadcastManager.getInstance(DateToPhoto.instance).sendBroadcast(
                intent
            )
        }
    }

    override fun reportTotal(total: Int) {
        this.total = total
        progressData.value = ProgressResult.Progress(total = total)
    }

    override fun onProgressChanged(progress: Int) {
        val totalDouble = total.toDouble()
        val progressPercent = progress / totalDouble * 100
        progressData.value = ProgressResult.Progress(total, progressPercent.toInt(), progress)
    }

    override fun reportEnd(fromActionShare: Boolean) {
        progressData.value = ProgressResult.Done(fromActionShare, searchPhotos)
    }
}

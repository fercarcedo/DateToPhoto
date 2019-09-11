package fergaral.datetophoto.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fergaral.datetophoto.DateToPhoto
import fergaral.datetophoto.db.DatabaseHelper
import fergaral.datetophoto.utils.PhotoUtils
import fergaral.datetophoto.utils.Utils
import kotlinx.coroutines.*

class PhotosViewModel: ViewModel() {
    private val _imagesToProcess = MutableLiveData<LoadPhotosState>()
    val imagesToProcess: LiveData<LoadPhotosState> = _imagesToProcess
    private var lastJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        load(null)
    }

    fun load(folderName: String?) {
        lastJob?.cancel()
        _imagesToProcess.value = LoadPhotosState.Loading
        lastJob = viewModelScope.launch {
            val imagesToProcess = withContext(Dispatchers.IO) {
                val cameraImages = if (isActive) PhotoUtils(DateToPhoto.instance).getCameraImages(if (folderName == null) Utils.getFoldersToProcess(DateToPhoto.instance) else arrayOf(folderName))
                    else arrayListOf()
                val photosDb = DatabaseHelper(DateToPhoto.instance).readableDatabase
                val imagesToProcess = if (isActive) Utils.getPhotosWithoutDate(DateToPhoto.instance,
                        cameraImages,
                        photosDb) else cameraImages
                photosDb.close()
                imagesToProcess
            }
            if (isActive) {
                _imagesToProcess.value = LoadPhotosState.Loaded(imagesToProcess)
            }
        }
    }
}
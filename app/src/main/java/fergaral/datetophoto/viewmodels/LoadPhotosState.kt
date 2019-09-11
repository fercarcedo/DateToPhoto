package fergaral.datetophoto.viewmodels

import fergaral.datetophoto.utils.Image
import java.util.ArrayList

sealed class LoadPhotosState {
    object Loading: LoadPhotosState()
    class Loaded(val imagesToProcess: ArrayList<Image>): LoadPhotosState()
}
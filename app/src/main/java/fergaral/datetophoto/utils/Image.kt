package fergaral.datetophoto.utils

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Image(
        val name: String,
        val bucketName: String,
        val dateAdded: Long,
        val uri: Uri,
        val path: String? = null
) : Parcelable {
    override fun toString() = "$bucketName/$name"
}
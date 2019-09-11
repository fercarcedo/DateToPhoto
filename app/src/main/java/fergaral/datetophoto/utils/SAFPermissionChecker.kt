package fergaral.datetophoto.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import fergaral.datetophoto.DateToPhoto
import fergaral.datetophoto.fragments.SAFPermissionDialogFragment
import fergaral.datetophoto.fragments.SettingsFragment
import java.util.*

@TargetApi(Build.VERSION_CODES.Q)
object SAFPermissionChecker {
    fun check(activity: Activity, bucketName: String? = null, selectedFolders: Array<String>? = null) {
        check(bucketName, selectedFolders) { activity.startActivityForResult(it, REQUEST_CODE) }
    }

    fun check(fragment: Fragment, bucketName: String? = null, selectedFolders: Array<String>? = null) {
        check(bucketName, selectedFolders) { fragment.startActivityForResult(it, REQUEST_CODE) }
    }

    fun check(bucketName: String?, selectedFolders: Array<String>? = null, lambda: (Intent) -> Unit) {
        val folders = selectedFolders?.toSet() ?: PhotoUtils(DateToPhoto.instance).folders
        val imageFolderList = getImageFolderList(folders)
        for (folderVolume in imageFolderList) {
            if (!hasPermission(folderVolume.photoUri) && (bucketName == null || bucketName in folderVolume.folderPath)) {
                askForSAFPermission(lambda, folderVolume.folderPath, folderVolume.volumeName)
            }
        }
    }

    private fun hasPermission(imageUri: Uri) = try {
        MediaStore.getDocumentUri(DateToPhoto.instance, imageUri)
        true
    } catch (e: SecurityException) {
        false
    }

    @SuppressLint("InlinedApi")
    private fun getImageFolderList(folders: Collection<String>): Set<SettingsFragment.FolderVolume> {
        val bucketName = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        val projection = arrayOf(MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.VOLUME_NAME, MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val query = Array(folders.size) { "?" }.joinToString(",")
        val cursor = DateToPhoto.instance.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, "$bucketName IN ($query)", folders.toTypedArray(), null)
        val imageRelativePaths = mutableSetOf<SettingsFragment.FolderVolume>()
        cursor?.use {
            if (cursor.moveToFirst()) {
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val volumeNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.VOLUME_NAME)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                do {
                    imageRelativePaths.add(SettingsFragment.FolderVolume(
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)),
                            cursor.getString(relativePathColumn),
                            cursor.getString(volumeNameColumn),
                            cursor.getString(bucketNameColumn)
                    ))
                } while (it.moveToNext())
            }
        }
        return imageRelativePaths
    }

    private fun askForSAFPermission(lambda: (Intent) -> Unit, folderPath: String, volumeName: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        val scapedFolderPath = getScapedFolderPath(folderPath)
        val volumeNameForUri = getVolumeNameForUri(volumeName)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DocumentFile.fromTreeUri(DateToPhoto.instance, Uri.parse("content://com.android.externalstorage.documents/tree/${volumeNameForUri}%3A$scapedFolderPath"))!!.uri)
        lambda(intent)
    }

    private fun getScapedFolderPath(folderPath: String) = folderPath.replace("/", "%2F")

    private fun getVolumeNameForUri(volumeName: String) = if ("primary" in volumeName) {
        "primary"
    } else {
        volumeName.toUpperCase(Locale.ROOT)
    }

    fun showSAFPermissionDialogIfNecessary(fragment: Fragment, selectedFolders: Collection<String>? = null) {
        val folders = selectedFolders ?: PhotoUtils(DateToPhoto.instance).folders
        val imageFolderList = getImageFolderList(folders)
        val noPermissionFolders = imageFolderList.filter { !hasPermission(it.photoUri) }.map { it.bucketName }.toTypedArray()
        if (!noPermissionFolders.isEmpty()) {
            SAFPermissionDialogFragment.newInstance(noPermissionFolders).show(fragment.childFragmentManager, SAFPermissionDialogFragment.TAG)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            uri?.let { DateToPhoto.instance.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        }
    }

    private const val REQUEST_CODE = 2
}
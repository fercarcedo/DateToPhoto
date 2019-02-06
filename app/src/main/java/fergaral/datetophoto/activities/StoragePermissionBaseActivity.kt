package fergaral.datetophoto.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.widget.Button

import fergaral.datetophoto.R
import fergaral.datetophoto.utils.Utils

/**
 * Created by fer on 24/06/16.
 */
abstract class StoragePermissionBaseActivity : AppCompatActivity() {
    //Indicates whether we should open settings instead,
    //because the user clicked the remember my answer in the permission dialog
    private var shouldOpenSettings: Boolean = false

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            shouldOpenSettings = !ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (shouldOpenSettings) {
                //If it's the first time the user sees the permission dialog, shouldOpenSettings will
                //be true
                if (Utils.permissionNeverAsked(this))
                    shouldOpenSettings = false
            }
        } else {
            //We have permission, we start the app
            onPermissionGranted()
        }
    }

    fun requestPermissionClicked() {
        if (shouldOpenSettings) {
            //We take the user to the app's details screen
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            //We prompt the user for the permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQCODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Utils.setPermissionAsked(this)

        when (requestCode) {
            STORAGE_PERMISSION_REQCODE -> {
                //If result is cancelled, the resulting arrays are empty
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //We have permission, we start the app
                    onPermissionGranted()
                } else {
                    //Permission was rejected, so we need to update the shouldOpenSettings variable
                    shouldOpenSettings = !ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    abstract fun onPermissionGranted()

    companion object {
        private val STORAGE_PERMISSION_REQCODE = 3
    }
}

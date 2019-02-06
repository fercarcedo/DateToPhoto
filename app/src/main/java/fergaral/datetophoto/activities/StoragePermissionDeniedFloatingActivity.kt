package fergaral.datetophoto.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window

import fergaral.datetophoto.R

class StoragePermissionDeniedFloatingActivity : StoragePermissionBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_storage_permission_denied_floating)

        val permissionBtnView = findViewById<View>(R.id.permissionBtn)
        permissionBtnView.setOnClickListener { v -> requestPermissionClicked() }
    }

    override fun onPermissionGranted() {
        //We have permission, simply close the floating window
        finish()
    }
}

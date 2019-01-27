package fergaral.datetophoto.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

import fergaral.datetophoto.R
import fergaral.datetophoto.utils.Utils

class StoragePermissionDeniedActivity : StoragePermissionBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_permission_denied)

        val permissionBtn = findViewById<View>(R.id.permissionBtn) as Button

        permissionBtn?.setOnClickListener { v -> requestPermissionClicked(v) }
    }


    override fun onBackPressed() {
        finish() //Since it's the first screen of the app, we simply leave it
    }

    override fun onPermissionGranted() {
        startActivity(Intent(this, PhotosActivity::class.java))
    }
}

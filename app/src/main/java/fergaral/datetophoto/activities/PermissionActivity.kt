package fergaral.datetophoto.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by fer on 30/05/16.
 */
abstract class PermissionActivity : BaseActivity() {
    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //We need to start the permission denied activity
            startActivity(Intent(this, StoragePermissionDeniedActivity::class.java))
        }
    }
}

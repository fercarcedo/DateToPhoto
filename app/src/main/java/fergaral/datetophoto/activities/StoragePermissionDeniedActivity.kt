package fergaral.datetophoto.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

import fergaral.datetophoto.R

class StoragePermissionDeniedActivity : StoragePermissionBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_permission_denied)

        val container = findViewById<View>(R.id.container)
        container.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        handleContainerInsets(container)
        val permissionBtn = findViewById<View>(R.id.permissionBtn) as Button
        handlePermissionButtonInsets(permissionBtn)
        permissionBtn.setOnClickListener { requestPermissionClicked() }
    }

    private fun handleContainerInsets(container: View) {
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            v.updatePadding(
                    top = insets.systemWindowInsetTop,
                    left = insets.systemWindowInsetLeft,
                    right = insets.systemWindowInsetRight
            )
            insets
        }
    }

    private fun handlePermissionButtonInsets(permissionButton: Button) {
        ViewCompat.setOnApplyWindowInsetsListener(permissionButton) { v, insets ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.systemWindowInsetBottom
            }
            insets
        }
    }

    override fun onBackPressed() {
        finish() //Since it's the first screen of the app, we simply leave it
    }

    override fun onPermissionGranted() {
        startActivity(Intent(this, PhotosActivity::class.java))
    }
}

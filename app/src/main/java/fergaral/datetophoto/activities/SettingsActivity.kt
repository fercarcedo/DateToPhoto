package fergaral.datetophoto.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.transition.Fade
import android.view.MenuItem
import android.view.Window

import fergaral.datetophoto.R
import fergaral.datetophoto.fragments.SettingsFragment

class SettingsActivity : PermissionActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            window.enterTransition = Fade()
            window.exitTransition = Fade()
        }

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null)
            SettingsActivity.SHOULD_REFRESH = false

        val toolbar = findViewById<Toolbar>(R.id.my_settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction().replace(R.id.main_settings_content, SettingsFragment()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        PhotosActivity.SHOULD_REFRESH_GRID = SettingsActivity.SHOULD_REFRESH

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition()
        } else {
            finish()
        }
    }

    companion object {
        var SHOULD_REFRESH = false
    }
}

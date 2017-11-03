package fergaral.datetophoto.activities

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.transition.Fade
import android.view.MenuItem
import android.view.Window

import fergaral.datetophoto.R
import fergaral.datetophoto.fragments.AboutFragment

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            window.enterTransition = Fade()
            window.exitTransition = Fade()
        }

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, AboutFragment())
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

        PhotosActivity.SHOULD_REFRESH_GRID = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            finishAfterTransition()
        else
            finish()
    }
}

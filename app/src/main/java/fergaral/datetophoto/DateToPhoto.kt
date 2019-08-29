package fergaral.datetophoto

import android.app.Application
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import fergaral.datetophoto.utils.ThemeUtils

import fergaral.datetophoto.works.ProcessPhotosWorker

/**
 * Created by Fer on 07/10/2017.
 */

class DateToPhoto : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        ProcessPhotosWorker.schedule()
        readTheme()
    }

    private fun readTheme() {
        val themeValue = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_theme_key), getString(R.string.pref_theme_default)) as String
        ThemeUtils.changeTheme(themeValue)
    }

    companion object {
        lateinit var instance: DateToPhoto
            private set
    }
}

package fergaral.datetophoto.utils

import androidx.appcompat.app.AppCompatDelegate
import fergaral.datetophoto.DateToPhoto
import fergaral.datetophoto.R

object ThemeUtils {
    fun changeTheme(themeValue: String) {
        AppCompatDelegate.setDefaultNightMode(when (themeValue) {
            DateToPhoto.instance.getString(R.string.dark_value) -> AppCompatDelegate.MODE_NIGHT_YES
            DateToPhoto.instance.getString(R.string.set_by_battery_saver_value) -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            DateToPhoto.instance.getString(R.string.system_default_value) -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        })
    }
}
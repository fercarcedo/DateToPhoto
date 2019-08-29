package fergaral.datetophoto.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import kotlin.properties.Delegates

abstract class BaseActivity: AppCompatActivity() {
    private var currentNightMode by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentNightMode = AppCompatDelegate.getDefaultNightMode()
    }

    override fun onResume() {
        super.onResume()
        if (AppCompatDelegate.getDefaultNightMode() != currentNightMode) {
            recreate()
        }
    }
}
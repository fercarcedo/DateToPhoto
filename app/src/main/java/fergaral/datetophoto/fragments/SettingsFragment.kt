package fergaral.datetophoto.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import fergaral.datetophoto.R
import fergaral.datetophoto.utils.FoldersListPreference
import fergaral.datetophoto.utils.PhotoUtils
import fergaral.datetophoto.utils.ThemeUtils
import java.util.Arrays

private const val FIRST_USE_KEY = "firstuse"

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        val listPreference = findPreference<FoldersListPreference>(getString(R.string.pref_folderstoprocess_key))
        populateFoldersToProcess(listPreference)
        handleThemeChange()
    }

    private fun populateFoldersToProcess(listPreference: FoldersListPreference?) {
        val folderNames = PhotoUtils.getFolders(activity!!)
        val entries = folderNames.toTypedArray()

        Arrays.sort(entries) { lhs, rhs ->
            lhs.toLowerCase().compareTo(rhs.toLowerCase())
        }

        listPreference?.entries = entries
        listPreference?.entryValues = entries

        selectAllFoldersIfFirstUse(listPreference)
    }

    private fun selectAllFoldersIfFirstUse(listPreference: FoldersListPreference?) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val firstUse = sharedPreferences.getBoolean(FIRST_USE_KEY, true)

        if (firstUse) {
            listPreference?.selectAll()
            val editor = sharedPreferences.edit()
            editor.putBoolean(FIRST_USE_KEY, false)
            editor.apply()
        }
    }

    private fun handleThemeChange() {
        val themePreference = findPreference<ListPreference>(getString(R.string.pref_theme_key))
        themePreference?.setOnPreferenceChangeListener { preference, newValue ->
            ThemeUtils.changeTheme(newValue as String)
            true
        }
    }
}
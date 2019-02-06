package fergaral.datetophoto.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.preference.PreferenceFragmentCompat
import fergaral.datetophoto.R
import fergaral.datetophoto.utils.FoldersListPreference
import fergaral.datetophoto.utils.PhotoUtils
import java.util.Arrays

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        val listPreference = findPreference(getString(R.string.pref_folderstoprocess_key)) as FoldersListPreference
        val folderNames = PhotoUtils.getFolders(activity!!)
        val entries = folderNames.toTypedArray()

        Arrays.sort(entries) {
            lhs, rhs -> lhs.toLowerCase().compareTo(rhs.toLowerCase())
        }

        listPreference.entries = entries
        listPreference.entryValues = entries

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val firstUse = sharedPreferences.getBoolean("firstuse", true)

        if (firstUse) {
            listPreference.selectAll()
            val editor = sharedPreferences.edit()
            editor.putBoolean("firstuse", false)
            editor.apply()
        }
    }
}
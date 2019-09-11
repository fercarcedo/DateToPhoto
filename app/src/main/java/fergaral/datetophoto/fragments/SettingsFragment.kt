package fergaral.datetophoto.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import fergaral.datetophoto.DateToPhoto
import fergaral.datetophoto.R
import fergaral.datetophoto.utils.*
import java.util.*

private const val FIRST_USE_KEY = "firstuse"

class SettingsFragment : PreferenceFragmentCompat(), SAFPermissionDialogFragment.SAFPermissionDialogListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        val listPreference = findPreference<FoldersListPreference>(getString(R.string.pref_folderstoprocess_key))
        populateFoldersToProcess(listPreference)
        handleThemeChange()
        askForSAFPermissionIfNecessary(listPreference)
    }

    private fun populateFoldersToProcess(listPreference: FoldersListPreference?) {
        val folderNames = PhotoUtils.getFolders(activity!!)
        val entries = folderNames.toTypedArray()

        Arrays.sort(entries) { lhs, rhs ->
            lhs.toLowerCase(Locale.ROOT).compareTo(rhs.toLowerCase(Locale.ROOT))
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

    private fun askForSAFPermissionIfNecessary(listPreference: FoldersListPreference?) {
        val overwritePhotosPreference = findPreference<CheckBoxPreference>(getString(R.string.pref_overwrite_key))
        overwritePhotosPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (newValue as Boolean) {
                    SAFPermissionChecker.showSAFPermissionDialogIfNecessary(this)
                }
            }
            true
        }
        listPreference?.setOnPreferenceChangeListener { preference, newValue ->
            val values = newValue as Collection<String>
            if (Utils.overwritePhotos(DateToPhoto.instance)) {
                SAFPermissionChecker.showSAFPermissionDialogIfNecessary(this, values)
            }
            true
        }
    }

    override fun onCheckSAFPermission(folders: Array<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SAFPermissionChecker.check(this, selectedFolders = folders)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SAFPermissionChecker.onActivityResult(requestCode, resultCode, data)
        }
    }

    data class FolderVolume(
            val photoUri: Uri,
            val folderPath: String,
            val volumeName: String,
            val bucketName: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (other is FolderVolume) {
                return other.folderPath == folderPath && other.volumeName == volumeName
            }
            return false
        }

        override fun hashCode() = 31 * folderPath.hashCode() + volumeName.hashCode()
    }
}
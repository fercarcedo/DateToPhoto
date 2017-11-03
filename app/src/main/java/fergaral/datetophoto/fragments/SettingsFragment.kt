package fergaral.datetophoto.fragments

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat

import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator

import fergaral.datetophoto.R
import fergaral.datetophoto.utils.FoldersListPreference
import fergaral.datetophoto.utils.PhotoUtils

class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_general)

        val listPreference = findPreference(getString(R.string.pref_folderstoprocess_key)) as FoldersListPreference
        val folderNames = PhotoUtils.getFolders(activity)
        val entries = folderNames.toTypedArray<String>()

        Arrays.sort(entries, Comparator<String> { lhs, rhs -> lhs.toLowerCase().compareTo(rhs.toLowerCase()) })

        if (entries.size > 0) {
            listPreference.entries = entries
            listPreference.setEntryValues(entries)
        }

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
package fergaral.datetophoto.utils

/**
 * Created by Fer on 13/10/2017.
 */

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.MultiSelectListPreference
import fergaral.datetophoto.R

/**
 * A [Preference] that displays a list of entries as
 * a dialog and allows multiple selections
 *
 *
 * This preference will store a string into the SharedPreferences. This string will be the values selected
 * from the [.setEntryValues] array.
 *
 */
class FoldersListPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : MultiSelectListPreference(context, attrs) {

    init {
        setOnPreferenceChangeListener { _, _ ->
            summary = summary
            true
        }
    }

    override fun getSummary(): CharSequence {
        val foldersToProcess = Utils.getFoldersToProcess(context)
        val folders = PhotoUtils.getFolders(context)

        val numFolders = if (foldersToProcess.size == 1 && foldersToProcess[0] == "") 0 else foldersToProcess.size

        return when {
            folders.size == 0 -> context.getString(R.string.no_folders)
            numFolders != folders.size -> context.resources.getQuantityString(R.plurals.folders,
                numFolders, numFolders)
            else -> context.getString(R.string.allfolders)
        }
    }

    override fun setEntries(entries: Array<out CharSequence>?) {
        super.setEntries(entries)
        isEnabled = entries?.isNotEmpty() ?: false
    }

    fun selectAll() {
        values = entryValues.map { it.toString() }.toSet()
    }
}


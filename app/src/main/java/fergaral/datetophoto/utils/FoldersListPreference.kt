package fergaral.datetophoto.utils

/**
 * Created by Fer on 13/10/2017.
 */

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.util.AttributeSet

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice

import java.util.HashSet

import fergaral.datetophoto.R
import fergaral.datetophoto.activities.SettingsActivity

/**
 * A [Preference] that displays a list of entries as
 * a dialog and allows multiple selections
 *
 *
 * This preference will store a string into the SharedPreferences. This string will be the values selected
 * from the [.setEntryValues] array.
 *
 */
class FoldersListPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ListPreference(context, attrs) {

    private var mClickedDialogEntryIndices: MutableSet<Int>

    init {
        mClickedDialogEntryIndices = HashSet()
        setFoldersSummary()
    }

    override fun setEntries(entries: Array<CharSequence>) {
        super.setEntries(entries)

        mClickedDialogEntryIndices = HashSet()
    }

    override fun showDialog(state: Bundle?) {
        restoreCheckedEntries()

        val dialog = MaterialDialog(context)
                .title(R.string.pref_folderstoprocess_title)
                .positiveButton(R.string.accept)

        val entryValues = entryValues

        if (entryValues.size == 1 && entryValues[0] == "nofolders") {
            dialog.message(text = entries[0])
                    .show()
        } else {
            dialog.negativeButton(R.string.cancel)
                .listItemsMultiChoice(items = entries.map { it.toString() }, initialSelection = mClickedDialogEntryIndices.toIntArray()) { dialog, indices, items ->
                    onClick(null, DialogInterface.BUTTON_POSITIVE)
                    dialog.dismiss()

                    if (entryValues != null) {
                        val value = StringBuffer()
                        for (i in entryValues.indices) {
                            if (Utils.containsInteger(i, indices.toTypedArray())) {
                                value.append(entryValues[i]).append(SEPARATOR)
                            }
                        }

                        if (callChangeListener(value)) {
                            var strVal = value.toString()
                            if (strVal.isNotEmpty())
                                strVal = strVal.substring(0, strVal.length - SEPARATOR.length)
                            setValue(strVal)
                        }
                    }
                    setFoldersSummary()
                    SettingsActivity.SHOULD_REFRESH = true
                }
                .show()
        }
    }

    fun restoreCheckedEntries() {
        mClickedDialogEntryIndices = HashSet()

        val entryValues = entryValues

        val vals = parseStoredValue(value)
        if (vals != null) {
            for (j in vals.indices) {
                val value = vals[j].trim { it <= ' ' }
                for (i in entryValues.indices) {
                    val entry = entryValues[i]
                    if (entry == value) {
                        mClickedDialogEntryIndices.add(i)
                        break
                    }
                }
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (!positiveResult)
            return

        val entryValues = entryValues
        if (entryValues != null) {
            val value = StringBuffer()
            for (i in entryValues.indices) {
                if (mClickedDialogEntryIndices.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR)
                }
            }

            if (callChangeListener(value)) {
                var strVal = value.toString()
                if (strVal.isNotEmpty())
                    strVal = strVal.substring(0, strVal.length - SEPARATOR.length)
                setValue(strVal)
            }
        }
    }

    fun selectAll() {
        mClickedDialogEntryIndices = HashSet()

        for (i in 0 until entryValues.size)
            mClickedDialogEntryIndices.add(i)

        val entryValues = entryValues
        if (entryValues != null) {
            val value = StringBuffer()
            for (i in entryValues.indices) {
                if (mClickedDialogEntryIndices.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR)
                }
            }

            if (callChangeListener(value)) {
                var strVal = value.toString()
                if (strVal.isNotEmpty())
                    strVal = strVal.substring(0, strVal.length - SEPARATOR.length)
                setValue(strVal)
            }
        }

        setFoldersSummary()
    }

    private fun setFoldersSummary() {
        val foldersToProcess = Utils.getFoldersToProcess(context)
        val folders = PhotoUtils.getFolders(context)

        val numFolders = if (foldersToProcess.size == 1 && foldersToProcess[0] == "") 0 else foldersToProcess.size

        if (numFolders != folders.size) {
            summary = context.resources.getQuantityString(R.plurals.folders,
                    numFolders, numFolders)
        } else {
            setSummary(R.string.allfolders)
        }
    }

    fun parseStoredValue(value: CharSequence?): Array<String>? {
        if (value == null)
            return null

        return if ("" == value)
            null
        else
            (value as String).split(SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    companion object {
        //Need to make sure the SEPARATOR is unique and weird enough that it doesn't match one of the entries.
        //Not using any fancy symbols because this is interpreted as a regex for splitting strings.
        val SEPARATOR = "OV=I=XseparatorX=I=VO"
    }
}


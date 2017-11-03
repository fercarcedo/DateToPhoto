package fergaral.datetophoto.utils

/**
 * Created by Fer on 13/10/2017.
 */

import android.app.AlertDialog.Builder
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.util.AttributeSet

import com.afollestad.materialdialogs.MaterialDialog

import java.util.ArrayList
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

    private var mClickedDialogEntryIndices: MutableSet<Int>? = null

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

        val builder = MaterialDialog.Builder(context)
                .title(R.string.pref_folderstoprocess_title)
                .items(*entries)
                .positiveText(R.string.accept)

        val entryValues = entryValues

        if (entryValues.size == 1 && entryValues[0] == "nofolders") {
            builder.build().show()
        } else {
            builder.negativeText(R.string.cancel)
                    .itemsCallbackMultiChoice(mClickedDialogEntryIndices!!.toTypedArray<Int>()
                    ) { materialDialog, integers, charSequences ->
                        onClick(null, DialogInterface.BUTTON_POSITIVE)
                        materialDialog.dismiss()

                        val entryValues = getEntryValues()
                        if (entryValues != null) {
                            val value = StringBuffer()
                            for (i in entryValues.indices) {
                                if (Utils.containsInteger(i, integers)) {
                                    value.append(entryValues[i]).append(SEPARATOR)
                                }
                            }

                            if (callChangeListener(value)) {
                                var `val` = value.toString()
                                if (`val`.length > 0)
                                    `val` = `val`.substring(0, `val`.length - SEPARATOR.length)
                                setValue(`val`)
                            }
                        }

                        setFoldersSummary()

                        SettingsActivity.SHOULD_REFRESH = true

                        true
                    }.build().show()
        }
    }

    fun restoreCheckedEntries() {
        mClickedDialogEntryIndices = HashSet()

        val entryValues = entryValues

        val vals = parseStoredValue(value)
        if (vals != null) {
            for (j in vals.indices) {
                val `val` = vals[j].trim { it <= ' ' }
                for (i in entryValues.indices) {
                    val entry = entryValues[i]
                    if (entry == `val`) {
                        mClickedDialogEntryIndices!!.add(i)
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
                if (mClickedDialogEntryIndices!!.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR)
                }
            }

            if (callChangeListener(value)) {
                var `val` = value.toString()
                if (`val`.length > 0)
                    `val` = `val`.substring(0, `val`.length - SEPARATOR.length)
                setValue(`val`)
            }
        }
    }

    fun selectAll() {
        mClickedDialogEntryIndices = HashSet()

        for (i in 0 until entryValues.size)
            mClickedDialogEntryIndices!!.add(i)

        val entryValues = entryValues
        if (entryValues != null) {
            val value = StringBuffer()
            for (i in entryValues.indices) {
                if (mClickedDialogEntryIndices!!.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR)
                }
            }

            if (callChangeListener(value)) {
                var `val` = value.toString()
                if (`val`.length > 0)
                    `val` = `val`.substring(0, `val`.length - SEPARATOR.length)
                setValue(`val`)
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

    fun parseStoredValue(`val`: CharSequence?): Array<String>? {
        if (`val` == null)
            return null

        return if ("" == `val`)
            null
        else
            (`val` as String).split(SEPARATOR.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
    }

    companion object {
        //Need to make sure the SEPARATOR is unique and weird enough that it doesn't match one of the entries.
        //Not using any fancy symbols because this is interpreted as a regex for splitting strings.
        val SEPARATOR = "OV=I=XseparatorX=I=VO"
    }
}


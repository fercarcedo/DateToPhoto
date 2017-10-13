package fergaral.datetophoto.utils;

/**
 * Created by Fer on 13/10/2017.
 */

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.SettingsActivity;

/**
 * A {@link Preference} that displays a list of entries as
 * a dialog and allows multiple selections
 * <p>
 * This preference will store a string into the SharedPreferences. This string will be the values selected
 * from the {@link #setEntryValues(CharSequence[])} array.
 * </p>
 */
public class FoldersListPreference extends ListPreference {
    //Need to make sure the SEPARATOR is unique and weird enough that it doesn't match one of the entries.
    //Not using any fancy symbols because this is interpreted as a regex for splitting strings.
    public static final String SEPARATOR = "OV=I=XseparatorX=I=VO";

    private Set<Integer> mClickedDialogEntryIndices;

    public FoldersListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mClickedDialogEntryIndices = new HashSet<>();

        setFoldersSummary();
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);

        mClickedDialogEntryIndices = new HashSet<>();
    }

    public FoldersListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void showDialog(Bundle state) {
        restoreCheckedEntries();

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext())
                .title(R.string.pref_folderstoprocess_title)
                .items(getEntries())
                .positiveText(R.string.accept);

        CharSequence[] entryValues = getEntryValues();

        if (entryValues.length == 1 && entryValues[0].equals("nofolders")) {
            builder.build().show();
        } else {
            builder.negativeText(R.string.cancel)
                    .itemsCallbackMultiChoice(mClickedDialogEntryIndices.toArray(new Integer[mClickedDialogEntryIndices.size()]),
                            new MaterialDialog.ListCallbackMultiChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                                    onClick(null, DialogInterface.BUTTON_POSITIVE);
                                    materialDialog.dismiss();

                                    CharSequence[] entryValues = getEntryValues();
                                    if (entryValues != null) {
                                        StringBuffer value = new StringBuffer();
                                        for (int i = 0; i < entryValues.length; i++) {
                                            if (Utils.containsInteger(i, integers)) {
                                                value.append(entryValues[i]).append(SEPARATOR);
                                            }
                                        }

                                        if (callChangeListener(value)) {
                                            String val = value.toString();
                                            if (val.length() > 0)
                                                val = val.substring(0, val.length() - SEPARATOR.length());
                                            setValue(val);
                                        }
                                    }

                                    setFoldersSummary();

                                    SettingsActivity.SHOULD_REFRESH = true;

                                    return true;
                                }
                            }).build().show();
        }
    }

    public static String[] parseStoredValue(CharSequence val) {
        if (val == null)
            return null;

        if ("".equals(val))
            return null;
        else
            return ((String) val).split(SEPARATOR);
    }

    public void restoreCheckedEntries() {
        mClickedDialogEntryIndices = new HashSet<>();

        CharSequence[] entryValues = getEntryValues();

        String[] vals = parseStoredValue(getValue());
        if (vals != null) {
            for (int j = 0; j < vals.length; j++) {
                String val = vals[j].trim();
                for (int i = 0; i < entryValues.length; i++) {
                    CharSequence entry = entryValues[i];
                    if (entry.equals(val)) {
                        mClickedDialogEntryIndices.add(i);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (!positiveResult)
            return;

        CharSequence[] entryValues = getEntryValues();
        if (entryValues != null) {
            StringBuffer value = new StringBuffer();
            for (int i = 0; i < entryValues.length; i++) {
                if (mClickedDialogEntryIndices.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR);
                }
            }

            if (callChangeListener(value)) {
                String val = value.toString();
                if (val.length() > 0)
                    val = val.substring(0, val.length() - SEPARATOR.length());
                setValue(val);
            }
        }
    }

    public void selectAll() {
        mClickedDialogEntryIndices = new HashSet<>();

        for (int i = 0; i < getEntryValues().length; i++)
            mClickedDialogEntryIndices.add(i);

        CharSequence[] entryValues = getEntryValues();
        if (entryValues != null) {
            StringBuffer value = new StringBuffer();
            for (int i = 0; i < entryValues.length; i++) {
                if (mClickedDialogEntryIndices.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR);
                }
            }

            if (callChangeListener(value)) {
                String val = value.toString();
                if (val.length() > 0)
                    val = val.substring(0, val.length() - SEPARATOR.length());
                setValue(val);
            }
        }

        setFoldersSummary();
    }

    private void setFoldersSummary() {
        String[] foldersToProcess = Utils.getFoldersToProcess(getContext());
        ArrayList<String> folders = PhotoUtils.getFolders(getContext());

        int numFolders = (foldersToProcess.length == 1 && foldersToProcess[0].equals("")) ? 0 : foldersToProcess.length;

        if (numFolders != folders.size()) {
            setSummary(getContext().getResources().getQuantityString(R.plurals.folders,
                    numFolders, numFolders));
        } else {
            setSummary(R.string.allfolders);
        }
    }
}


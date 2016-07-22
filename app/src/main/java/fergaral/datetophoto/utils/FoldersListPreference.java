package fergaral.datetophoto.utils;


import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import fergaral.datetophoto.activities.PhotosActivity;
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
    private Context mContext;

    public FoldersListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mClickedDialogEntryIndices = new HashSet<>();
        mContext = context;

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
    protected void onPrepareDialogBuilder(@NonNull Builder builder) {
        /*CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length ) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        restoreCheckedEntries();
        builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean val) {
                        mClickedDialogEntryIndices[which] = val;
                    }
                });*/
    }

    @Override
    protected void showDialog(Bundle state) {
        restoreCheckedEntries();

        MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext)
                .title("Carpetas que se procesarán")
                .items(getEntries())
                .positiveText("Aceptar");

        CharSequence[] entryValues = getEntryValues();

        if(entryValues.length == 1 && entryValues[0].equals("nofolders")) {
            builder.build().show();
        }else {
            builder.negativeText("Cancelar")
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
        if(val == null)
            return null;
        
        if ( "".equals(val) )
            return null;
        else
            return ((String)val).split(SEPARATOR);
    }

    public void restoreCheckedEntries() {
        mClickedDialogEntryIndices = new HashSet<>();

        CharSequence[] entryValues = getEntryValues();

        String[] vals = parseStoredValue(getValue());
        if ( vals != null ) {
            for ( int j=0; j<vals.length; j++ ) {
                String val = vals[j].trim();
                for ( int i=0; i<entryValues.length; i++ ) {
                    CharSequence entry = entryValues[i];
                    if ( entry.equals(val) ) {
                        mClickedDialogEntryIndices.add(i);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
//        super.onDialogClosed(positiveResult);

        if(!positiveResult)
            return;

        CharSequence[] entryValues = getEntryValues();
        if (entryValues != null) {
            StringBuffer value = new StringBuffer();
            for ( int i=0; i<entryValues.length; i++ ) {
                if ( mClickedDialogEntryIndices.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR);
                }
            }

            if (callChangeListener(value)) {
                String val = value.toString();
                if ( val.length() > 0 )
                    val = val.substring(0, val.length()-SEPARATOR.length());
                setValue(val);
            }
        }
    }

    public void selectAll() {
        mClickedDialogEntryIndices = new HashSet<>();

        for(int i=0; i<getEntryValues().length; i++)
            mClickedDialogEntryIndices.add(i);

        CharSequence[] entryValues = getEntryValues();
        if (entryValues != null) {
            StringBuffer value = new StringBuffer();
            for ( int i=0; i<entryValues.length; i++ ) {
                if ( mClickedDialogEntryIndices.contains(i)) {
                    value.append(entryValues[i]).append(SEPARATOR);
                }
            }

            if (callChangeListener(value)) {
                String val = value.toString();
                if ( val.length() > 0 )
                    val = val.substring(0, val.length()-SEPARATOR.length());
                setValue(val);
            }
        }

        setFoldersSummary();
    }

    private void setFoldersSummary() {
        String[] foldersToProcess = Utils.getFoldersToProcess(mContext);
        ArrayList<String> folders = PhotoUtils.getFolders(mContext);

        if(foldersToProcess.length != 1) {
            if(foldersToProcess.length != folders.size())
                setSummary(foldersToProcess.length + " carpetas");
            else
                setSummary("Todas");
        }else{
            if(foldersToProcess[0].equals(""))
                setSummary("0 carpetas");
            else
                setSummary(foldersToProcess.length + " carpeta");
        }
    }
}
package fergaral.datetophoto.fragments;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;

import com.github.machinarius.preferencefragment.PreferenceFragment;

import java.util.ArrayList;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.MyActivity;
import fergaral.datetophoto.activities.PhotosActivity;
import fergaral.datetophoto.utils.FoldersListPreference;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.Utils;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

            /*ListPreference listPreference = (ListPreference) findPreference(getString(R.string.pref_folderstoprocess_key));

            HashSet<String> cameraImages = getCameraImages(getActivity());
            String[] entries = (String[]) cameraImages.toArray();
            String[] entryValues = (String[]) cameraImages.toArray();
            listPreference.setEntries(entries);
            listPreference.setEntryValues(entryValues);*/

        FoldersListPreference listPreference = (FoldersListPreference) findPreference(getString(R.string.pref_folderstoprocess_key));
        ArrayList<String> folderNames = PhotoUtils.getFolders(getActivity());

        String[] entries = new String[folderNames.size()];
        for(int i=0; i<folderNames.size(); i++)
        {
            entries[i] = folderNames.get(i);
        }
        //showNotification(String.valueOf(entries.length));
        listPreference.setEntries(entries);
        listPreference.setEntryValues(entries);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean firstUse = sharedPreferences.getBoolean("firstuse", true);

        if(firstUse)
        {
            listPreference.selectAll();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstuse", false);
            editor.apply();
        }
        //ArrayList<String> cameraImages = getCameraImages(getActivity());
        //Toast.makeText(getActivity(), String.valueOf(cameraImages.size()), Toast.LENGTH_LONG).show();
    }

    public void showNotification(String text) {
        Intent resultIntent = new Intent(getActivity(), MyActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                getActivity(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getActivity())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Date To Photo")
                .setContentText(text)
                .setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Activity.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notifBuilder.build());
    }

    public String getParentFolderPath(String path)
    {
        String[] pathSegments = path.split("/");
        int lastSegmentLength = pathSegments[pathSegments.length - 1].length() + 1;

        String parentFolderPath = path.substring(0, path.length() - lastSegmentLength);

        return parentFolderPath;
    }

}
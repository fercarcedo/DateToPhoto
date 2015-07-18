package fergaral.datetophoto.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import fergaral.datetophoto.R;
import fergaral.datetophoto.utils.FoldersListPreference;
import fergaral.datetophoto.utils.PhotoUtils;

/**
 * Created by fer on 15/06/15.
 */
public class TutorialFragment3 extends Fragment {

    private Set<Integer> selectedPositions;
    private ArrayList<String> folderNames;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tutorial3, container, false);

        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.tutorial_foldersprocess_lv);
        Button selectAllFoldersBtn = (Button) rootView.findViewById(R.id.tutorial_foldersprocess_btn1);

        selectedPositions = new HashSet<>();

        folderNames = PhotoUtils.getFolders(getActivity());

        selectAllFoldersIfFirstUse(folderNames);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String foldersToProcessStr = prefs.getString(getString(R.string.pref_folderstoprocess_key), "");

        final ArrayList<FolderItem> folderItems = new ArrayList<FolderItem>();

        for(int i=0; i< folderNames.size(); i++) {
            if(foldersToProcessStr.contains(folderNames.get(i))) {
                selectedPositions.add(i);
            }
        }

        for(String folderName : folderNames) {
            folderItems.add(new FolderItem(folderName, foldersToProcessStr.contains(folderName)));
        }

        final FoldersRecyclerViewAdapter adapter = new FoldersRecyclerViewAdapter(folderItems);

        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        selectAllFoldersBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                for(int i=0; i<folderNames.size(); i++)
                    selectedPositions.add(i);

                saveSelectedFolders();

                //Modificamos los FolderItems para que estén todos seleccionados
                for(FolderItem item : folderItems)
                    item.setSelected(true);

                //Notificamos al adaptador de la RecyclerView de que sus datos han cambiado
                adapter.notifyDataSetChanged();
            }
        });

        return rootView;
    }

    private void selectAllFolders(ArrayList<String> folderNames) {
        StringBuilder stringBuilder = new StringBuilder();

        for(String folderName : folderNames) {
            stringBuilder.append(folderName).append(FoldersListPreference.SEPARATOR);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.pref_folderstoprocess_key), stringBuilder.toString());
        editor.apply();
    }

    private void selectAllFoldersIfFirstUse(ArrayList<String> folderNames) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean firstUse = prefs.getBoolean("firstuse", true);

        if(firstUse) {
            selectAllFolders(folderNames);
        }
    }

    private void saveSelectedFolders() {
        StringBuilder stringBuilder = new StringBuilder();

        for(int position : selectedPositions) {
            stringBuilder.append(folderNames.get(position)).append(FoldersListPreference.SEPARATOR);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.pref_folderstoprocess_key), stringBuilder.toString());
        editor.apply();
    }

    public class FoldersRecyclerViewAdapter extends RecyclerView.Adapter<FoldersRecyclerViewAdapter.ViewHolder> {

        private ArrayList<FolderItem> folderItems;
        private View.OnClickListener onClickListener;

        public FoldersRecyclerViewAdapter(ArrayList<FolderItem> folderItems) {
            this.folderItems = folderItems;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.tutorial_lv_item, viewGroup, false);

            FoldersRecyclerViewAdapter.ViewHolder viewHolder = new FoldersRecyclerViewAdapter.ViewHolder(itemView);

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            viewHolder.bindItem(folderItems.get(position));
        }

        @Override
        public int getItemCount() {
            return folderItems.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private CheckBox checkBox;
            private TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);

                checkBox = (CheckBox) itemView.findViewById(R.id.tutorial_foldersprocess_lv_checkbox);
                textView = (TextView) itemView.findViewById(R.id.tutorial_foldersprocess_lv_textview);
            }

            public void bindItem(final FolderItem item) {
                textView.setText(item.getFolderName());
                checkBox.setChecked(item.isSelected());

                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox checkBox = (CheckBox) v;

                        if(checkBox.isChecked()) {
                            //Añadimos el nombre de la carpeta al Set de posiciones seleccionadas
                            selectedPositions.add(folderNames.indexOf(item.getFolderName()));
                        }else{
                            //Eliminamos el nombre de la carpeta
                            selectedPositions.remove(folderNames.indexOf(item.getFolderName()));
                        }

                        item.setSelected(checkBox.isChecked());
                        saveSelectedFolders();
                    }
                });
            }
        }
    }

    public class FolderItem {
        private String folderName;
        private boolean selected;

        public FolderItem(String folderName, boolean selected) {
            this.folderName = folderName;
            this.selected = selected;
        }

        public String getFolderName() {
            return folderName;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {this.selected = selected; }
    }
}

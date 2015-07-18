package fergaral.datetophoto.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.DiskSpaceUsageActivity;
import fergaral.datetophoto.utils.Utils;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;

public class DiskSpaceUsageFragment extends Fragment {

    public DiskSpaceUsageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_disk_space_usage, container, false);
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.my_toolbar);
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();

        if(toolbar != null)
            appCompatActivity.setSupportActionBar(toolbar);

        appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tvphotoswithoutdate = (TextView) rootView.findViewById(R.id.tvphotoswithoutdateusage);
        TextView tvphotoswithdate = (TextView) rootView.findViewById(R.id.tvphotoswithdateusage);

        int[] diskspaceusage = Utils.getDiskSpaceUsage(getActivity());
        tvphotoswithoutdate.setText("Photos without date: " + diskspaceusage[1] + " MB");
        tvphotoswithdate.setText("Photos with date: " + diskspaceusage[0] + " MB");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Confirmación");
        builder.setMessage("¿Estás seguro de que quieres borrar todas las fotos con fecha de tu dispositivo? Esta acción no se puede deshacer");
        builder.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ((DiskSpaceUsageActivity) getActivity()).deleteImagesWithDate();
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog alertDialog = builder.create();

        Button btn1 = (Button) rootView.findViewById(R.id.btndeletephotoswithdate);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.show();
            }
        });

        PieChartView piechart = (PieChartView) rootView.findViewById(R.id.piechart);
        piechart.setChartRotationEnabled(false);

        PieChartData data = new PieChartData();

        List<SliceValue> sliceValues = new ArrayList<SliceValue>();

        SliceValue photoswithdatevalue = new SliceValue(diskspaceusage[0], Color.rgb(76, 175, 80));
        photoswithdatevalue.setLabel("Fotos fechadas");
        SliceValue photoswithoutdatevalue = new SliceValue(diskspaceusage[1], Color.rgb(205, 220, 57));
        photoswithoutdatevalue.setLabel("Fotos sin fechar");

        sliceValues.add(photoswithdatevalue);
        sliceValues.add(photoswithoutdatevalue);

        data.setHasLabels(true);
        data.setValues(sliceValues);

        piechart.setPieChartData(data);

        return rootView;
    }
}
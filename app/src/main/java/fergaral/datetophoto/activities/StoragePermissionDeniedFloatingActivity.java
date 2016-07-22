package fergaral.datetophoto.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import fergaral.datetophoto.R;

public class StoragePermissionDeniedFloatingActivity extends StoragePermissionBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_storage_permission_denied_floating);

        View permissionBtnView = findViewById(R.id.permissionBtn);

        if(permissionBtnView != null)
            permissionBtnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissionClicked(v);
            }
        });
    }

    @Override
    public void onPermissionGranted() {
        //We have permission, simply close the floating window
        finish();
    }
}

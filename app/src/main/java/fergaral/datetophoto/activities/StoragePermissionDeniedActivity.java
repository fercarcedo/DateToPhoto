package fergaral.datetophoto.activities;

import android.Manifest;
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
import android.widget.Button;

import fergaral.datetophoto.R;
import fergaral.datetophoto.utils.Utils;

public class StoragePermissionDeniedActivity extends StoragePermissionBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_permission_denied);

        Button permissionBtn = (Button) findViewById(R.id.permissionBtn);

        if(permissionBtn != null) {
            permissionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestPermissionClicked(v);
                }
            });
        }
    }


    @Override
    public void onBackPressed() {
        finish(); //Since it's the first screen of the app, we simply leave it
    }

    @Override
    public void onPermissionGranted() {
        startActivity(new Intent(this, PhotosActivity.class));
    }
}

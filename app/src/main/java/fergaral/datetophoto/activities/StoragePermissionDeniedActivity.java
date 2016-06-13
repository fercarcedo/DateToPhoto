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

public class StoragePermissionDeniedActivity extends AppCompatActivity {

    //Indicates whether we should open settings instead,
    //because the user clicked the remember my answer in the permission dialog
    private boolean shouldOpenSettings;
    private static final int STORAGE_PERMISSION_REQCODE = 3;

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
    protected void onResume() {
        super.onResume();

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            shouldOpenSettings = !ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(shouldOpenSettings) {
                //If it's the first time the user sees the permission dialog, shouldOpenSettings will
                //be true
                if(Utils.permissionNeverAsked(this))
                    shouldOpenSettings = false;
            }
        }else{
            //We have permission, we start the app
            startActivity(new Intent(this, PhotosActivity.class));
        }
    }

    public void requestPermissionClicked(View view) {
        if(shouldOpenSettings) {
            //We take the user to the app's details screen
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }else{
            //We prompt the user for the permission
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                    STORAGE_PERMISSION_REQCODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Utils.setPermissionAsked(this);

        switch(requestCode) {
            case STORAGE_PERMISSION_REQCODE: {
                //If result is cancelled, the resulting arrays are empty
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //We have permission, we start the app
                    startActivity(new Intent(this, PhotosActivity.class));
                }else {
                    //Permission was rejected, so we need to update the shouldOpenSettings variable
                    shouldOpenSettings = !ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        finish(); //Since it's the first screen of the app, we simply leave it
    }
}

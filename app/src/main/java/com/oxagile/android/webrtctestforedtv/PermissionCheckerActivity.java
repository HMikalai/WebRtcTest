package com.oxagile.android.webrtctestforedtv;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class PermissionCheckerActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1234;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_checker);

        Button button = findViewById(R.id.button_permission_request);
        if (button != null) {
            button.setOnClickListener(v -> requestPermissions());
        }

        requestPermissions();
    }

    private void requestPermissions() {
        if (checkPermission(Manifest.permission.CAMERA) && checkPermission(Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            navigateToCallActivity();
        }
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                navigateToCallActivity();
            }
        }
    }

    private void navigateToCallActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

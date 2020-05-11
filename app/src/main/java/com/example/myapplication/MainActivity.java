package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.example.myapplication.core.cs.DataRes;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE,
        }, 1000);

        findViewById(R.id.v_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebActivity.start(MainActivity.this);
            }
        });

        findViewById(R.id.v_check).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataRes.preStart(Config.TEST_URL);
            }
        });

        DataRes.init(MainActivity.this);
    }
}

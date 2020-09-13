package com.mrap.vicam;

import android.app.Activity;
import android.os.Bundle;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.markers.DefaultAutoFocusMarker;

public class MainActivity extends Activity {

    CameraView camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.camera);
        camera.setAutoFocusMarker(new DefaultAutoFocusMarker());
    }

    @Override
    protected void onResume() {
        super.onResume();

        camera.open();
    }

    @Override
    protected void onPause() {
        super.onPause();

        camera.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        camera.destroy();
    }
}
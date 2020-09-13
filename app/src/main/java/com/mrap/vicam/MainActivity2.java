package com.mrap.vicam;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.FrameLayout;

public class MainActivity2 extends Activity {

    FrameLayout camPreview;
    ViSurfaceView viSurfaceView;
    Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        camPreview = findViewById(R.id.camPreview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open();
        viSurfaceView = new ViSurfaceView(this, camera);
        camPreview.removeAllViews();
        camPreview.addView(viSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.release();
    }
}
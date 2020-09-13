package com.mrap.vicam;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity2 extends Activity {

    FrameLayout camPreview;
    ViSurfaceView viSurfaceView;
    Camera camera = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        camPreview = findViewById(R.id.camPreview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            //getActionBar().hide();
        }
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            viSurfaceView = new ViSurfaceView(this, camera);
            camPreview.removeAllViews();
            camPreview.addView(viSurfaceView);
        } catch (Exception ex) { }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null)
            camera.release();
        camera = null;
    }
}
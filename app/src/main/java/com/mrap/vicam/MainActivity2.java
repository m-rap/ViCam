package com.mrap.vicam;

import android.Manifest;
import android.app.Activity;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import android.util.Log;
//import android.content.Context;
//import android.content.ContextWrapper;

public class MainActivity2 extends Activity {

    FrameLayout camPreview;
    ViSurfaceView viSurfaceView;
    Camera camera = null;
    final int PERMISSION_REQUEST_CODE = 0;

    final String TAG = "MainActivity2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        camPreview = findViewById(R.id.camPreview);
        camPreview.removeAllViews();
    }

    private void requestPermissions() {
        //Activity activity = null;
        //Context context = getContext();
        //while (context instanceof ContextWrapper) {
        //    if (context instanceof Activity) {
        //        activity = (Activity) context;
        //    }
        //    context = ((ContextWrapper)context).getBaseContext();
        //}

        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        //if (activity != null) {
            //activity.requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        //s}
    }

    boolean camGranted = false;
    boolean recAudioGranted = false;

    @Override
    protected void onResume() {
        super.onResume();

        camGranted = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        recAudioGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        
        if (!camGranted) {
            requestPermissions();
            return;
        }

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
package com.mrap.vicam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicHistogram;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** A basic Camera preview class */
public class ViSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    final static String TAG = "ViSurfaceView";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    Bitmap bmp = null;
    ScheduledExecutorService ses = null;
    byte[] nv21 = null;
    RenderScript rs = null;
    ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = null;
    ScriptIntrinsicHistogram histogramIntinsic = null;

    final Object lock = new Object();
    final int[] hData = new int[256];

    Runnable calcHsvTask = new Runnable() {
        @Override
        public void run() {
//            byte[] localNv21;
//            synchronized (lock) {
//                if (nv21 == null) {
//                    return;
//                }
//                localNv21 = nv21;
//            }

            Log.v(TAG, "calcHsv Start");

            int w = getWidth(), h = getHeight();

            if (bmp == null) {
                bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            }

            if (rs == null) {
                rs = RenderScript.create(getContext());
            }
            if (yuvToRgbIntrinsic == null) {
                yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
            }
            if (histogramIntinsic == null) {
                histogramIntinsic = ScriptIntrinsicHistogram.create(rs, Element.U8_4(rs));
            }

            Log.v(TAG, "allocating io");

            Type yuvType;
            Allocation in;

            //in.copyFrom(localNv21);
            synchronized (lock) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length).create();
                in = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT);
                in.copyFrom(nv21);
            }

            Type rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h).create();
            Allocation out = Allocation.createTyped(rs, rgbaType, Allocation.USAGE_SCRIPT);

            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);

            out.copyTo(bmp);

            Log.v(TAG, "copied to bmp");

            Allocation hIn = Allocation.createFromBitmap(rs, bmp);
            //Type i32Type = new Type.Builder(rs, Element.I32(rs)).create();
            //Allocation hOut = Allocation.createTyped(rs, i32Type, Allocation.USAGE_SCRIPT);
            Allocation hOut = Allocation.createSized(rs, Element.I32(rs), 256);

            histogramIntinsic.setOutput(hOut);
            histogramIntinsic.forEach(hIn);

            hOut.copyTo(hData);

            Log.v(TAG, "copied to histogram data");

            int area = w * h;
            float sumV = 0, sumV2 = 0;
//            for (int y = 0; y < h; y++) {
//                for (int x = 0; x < w; x++) {
//                    int color = bmp.getPixel(x, y);
//                    Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
//                    sumV += hsv[2];
//                }
//            }
            StringBuilder sb = new StringBuilder("h samples ");
            for (int i = 0; i < hData.length; i++) {
                sumV2 += hData[i];
                if (i % (hData.length / 5) == 0) {
                    sb.append(hData[i]).append(" ");
                }
            }
            float avgV = sumV / area, avgV2 = sumV2 / 256;

            //Log.i(TAG, new StringBuilder("sum v ").append(sumV).append(" avg v ").append(avgV).toString());
            //Log.i(TAG, new StringBuilder("sum v 2 ").append(sumV2).append(" avg v 2 ").append(avgV2).toString());
            Log.i(TAG, sb.toString());

            in.destroy();
            out.destroy();

            yuvType.destroy();
            rgbaType.destroy();

            hIn.destroy();
            hOut.destroy();

            //i32Type.destroy();

            Log.v(TAG, "task ended");
        }
    };

    public ViSurfaceView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        Log.i(TAG, "sufrace created");
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            if (ses == null) {
                ses = Executors.newSingleThreadScheduledExecutor();
                ses.scheduleWithFixedDelay(calcHsvTask, 0, 1, TimeUnit.MICROSECONDS);
            }
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        Log.i(TAG, "surface destroyed");
        resetRes();
    }

    void resetRes() {
        if (ses != null) {
            ses.shutdownNow();
//            try {
//                ses.awaitTermination(1000, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        ses = null;
        bmp = null;
        if (yuvToRgbIntrinsic != null) {
            yuvToRgbIntrinsic.destroy();
        }
        yuvToRgbIntrinsic = null;
        if (histogramIntinsic != null) {
            histogramIntinsic.destroy();
        }
        histogramIntinsic = null;
        if (rs != null) {
            rs.destroy();
        }
        rs = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        Log.i(TAG, "surface changed");

        resetRes();
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

            if (ses == null) {
                ses = Executors.newSingleThreadScheduledExecutor();
                ses.scheduleWithFixedDelay(calcHsvTask, 0, 1, TimeUnit.MICROSECONDS);
            }
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        synchronized (lock) {
            nv21 = data;
            Log.v(TAG, "updated nv21");
        }
    }
}

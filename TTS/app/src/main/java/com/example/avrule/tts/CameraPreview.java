package com.example.avrule.tts;

import android.content.Context;
import android.graphics.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by avrule on 27/2/18.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private android.hardware.Camera mCamera;

    public CameraPreview(Context context, android.hardware.Camera camera){
        super(context);
        mCamera = camera;

        //Install a SurfaceHolder.Callback so we get notified when the underlying
        //surface is created and destroyed
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder){
        //The surface has been created, now tell the camera where to draw the preview

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("this is tag","Error setting camera preview: "+e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder){
        //empty. Take care of relearing the Camera preview in your activity

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //if your preview can change or rotate, take care of those events here.
        //Make sure to stop the preview before resizing or reformatting it.

        if(mHolder.getSurface()==null){
            //preview surface does not exist
            return;
        }

        //stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            //ignore:tried to stop a non existent preview
        }

        //set preview size and make any resize, rotate or
        //reformatting changes here

        //start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        }catch (Exception e){
            Log.d("mcamera err","Error starting camera preview:"+e.getMessage());
        }
    }
}

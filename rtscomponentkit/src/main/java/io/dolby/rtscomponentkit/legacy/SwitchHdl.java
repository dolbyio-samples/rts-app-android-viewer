package io.dolby.rtscomponentkit.legacy;

import android.util.Log;

import com.millicast.VideoSource;

/**
 * Implementation of VideoSource's camera switch listener.
 * This handles camera switch events that allows us to know the outcome and details of camera switching.
 */
class SwitchHdl implements VideoSource.SwitchCameraHandler {
    public static final String TAG = "SwitchHdl";
    private String logTag = "[Video][Source][Cam][Switch][Hdl] ";

    @Override
    public void onCameraSwitchDone(boolean b) {
        Log.d(TAG, logTag + "Done: " + b);
    }

    @Override
    public void onCameraSwitchError(String s) {
        Log.d(TAG, logTag + "Error: " + s);
    }
}

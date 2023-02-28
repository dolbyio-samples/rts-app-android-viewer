package io.dolby.rtscomponentkit.legacy;

import static io.dolby.rtscomponentkit.legacy.MCStates.PublisherState.CONNECTED;
import static io.dolby.rtscomponentkit.legacy.MCStates.PublisherState.DISCONNECTED;
import static io.dolby.rtscomponentkit.legacy.MCStates.PublisherState.PUBLISHING;
import static io.dolby.rtscomponentkit.legacy.Utils.logD;

import com.millicast.Publisher;

import org.webrtc.RTCStatsReport;

/**
 * Implementation of Publisher's Listener.
 * This handles events sent to the Publisher being listened to.
 */
public class PubListener implements Publisher.Listener {
    public static final String TAG = "PubListener";

    private MillicastManager mcMan;
    private String logTagClass = "[Pub][Ltn]";

    public PubListener() {
        mcMan = MillicastManager.getSingleInstance();
    }

    @Override
    public void onPublishing() {
        mcMan.setPubState(PUBLISHING);
//        setUI();
        String logTag = logTagClass + "[On] ";
//        makeSnackbar(logTag, "OK. Publish started.", mcMan.getFragmentPub());
    }

    @Override
    public void onPublishingError(String s) {
        String logTag = logTagClass + "[Error] ";
//        makeSnackbar(logTag, "Publish Error:" + s, mcMan.getFragmentPub());
    }

    @Override
    public void onConnected() {
        String logTag = logTagClass + "[Con][On] ";
        mcMan.setPubState(CONNECTED);
//        setUI();
//        makeSnackbar(logTag, "Connected", mcMan.getFragmentPub());
        mcMan.startPub();
    }

    @Override
    public void onConnectionError(String reason) {
        String logTag = logTagClass + "[Con][Error] ";
        mcMan.setPubState(DISCONNECTED);
//        setUI();
//        makeSnackbar(logTag, "Connection FAILED! " + reason, mcMan.getFragmentPub());
    }

    @Override
    public void onSignalingError(String s) {
        String logTag = logTagClass + "[Sig][Error] ";
//        makeSnackbar(logTag, "Signaling Error:" + s, mcMan.getFragmentPub());
    }

    @Override
    public void onStatsReport(RTCStatsReport statsReport) {
        String logTag = logTagClass + "[Stat] ";
        String log = statsReport.toString();
        Utils.logD(TAG, log, logTag);
    }

    @Override
    public void onViewerCount(int count) {
        String logTag = logTagClass + "[Viewer] ";
        Utils.logD(TAG, logTag + "Count: " + count + ".");
    }

    @Override
    public void onActive() {
        String logTag = logTagClass + "[Viewer][Active] ";
        Utils.logD(TAG, logTag + "A viewer has subscribed to our stream.");
    }

    @Override
    public void onInactive() {
        String logTag = logTagClass + "[Viewer][Active][In] ";
        Utils.logD(TAG, logTag + "No viewers are currently subscribed to our stream.");
    }

    /**
     * Set UI states if containing view is available.
     */
//    private void setUI() {
//        mcMan.getMainActivity().runOnUiThread(() -> {
//            PublishFragment publishFragment = mcMan.getFragmentPub();
//            if (publishFragment != null) {
//                publishFragment.setUI();
//            }
//        });
//    }
}

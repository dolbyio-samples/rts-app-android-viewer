package io.dolby.rtscomponentkit.legacy;

/**
 * Values for Millicast related constants.
 * Fill in the desired values for these constants as required.
 * These values can be edited in the Millicast Settings UI.
 * The UI also provides a way to reload the file values here.
 */
public final class Constants {

    // Set the following as default values if desired.
    public static final String ACCOUNT_ID = "cnVTbw";
    public static final String STREAM_NAME_PUB = "ld9mukfi";
    public static final String STREAM_NAME_SUB = "ld9mukfi";
    public static final String TOKEN_PUB = "c68420e4efbdbc5591525046b74866cd964bd50385b2d041bc548a83da99deca";
    // If a Subscribe Token is not required, set it as an empty string.
    public static final String TOKEN_SUB = "";
    public static final String SOURCE_ID_PUB = "";
    public static final boolean SOURCE_ID_PUB_ENABLED = false;
    public static final String URL_PUB = "https://director.millicast.com/api/director/publish";
    public static final String URL_SUB = "https://director.millicast.com/api/director/subscribe";
    
    // Ricoh Constants
    public static final String ACTION_MAIN_CAMERA_CLOSE = "com.theta360.plugin.ACTION_MAIN_CAMERA_CLOSE";
    public static final String ACTION_MAIN_CAMERA_OPEN = "com.theta360.plugin.ACTION_MAIN_CAMERA_OPEN";
    public static final String ACTION_FINISH_PLUGIN = "com.theta360.plugin.ACTION_FINISH_PLUGIN";
    public static final String ACTION_ERROR_OCCURED = "com.theta360.plugin.ACTION_ERROR_OCCURED";
    public static final String PACKAGE_NAME = "packageName";
    public static final String EXIT_STATUS = "exitStatus";
    public static final String MESSAGE = "message";

    public static final String ACTION_LED_SHOW = "com.theta360.plugin.ACTION_LED_SHOW";
    public static final String ACTION_LED_BLINK = "com.theta360.plugin.ACTION_LED_BLINK";
    public static final String ACTION_LED_HIDE = "com.theta360.plugin.ACTION_LED_HIDE";
    public static final String TARGET = "target";
    public static final String COLOR = "color";
    public static final String PERIOD = "period";

    public static final String ACTION_AUDIO_SHUTTER = "com.theta360.plugin.ACTION_AUDIO_SHUTTER";
    public static final String ACTION_AUDIO_SH_OPEN = "com.theta360.plugin.ACTION_AUDIO_SH_OPEN";
    public static final String ACTION_AUDIO_SH_CLOSE = "com.theta360.plugin.ACTION_AUDIO_SH_CLOSE";
    public static final String ACTION_AUDIO_MOVSTART = "com.theta360.plugin.ACTION_AUDIO_MOVSTART";
    public static final String ACTION_AUDIO_MOVSTOP = "com.theta360.plugin.ACTION_AUDIO_MOVSTOP";
    public static final String ACTION_AUDIO_SELF = "com.theta360.plugin.ACTION_AUDIO_SELF";
    public static final String ACTION_AUDIO_WARNING = "com.theta360.plugin.ACTION_AUDIO_WARNING";

    public static final String ACTION_WLAN_OFF = "com.theta360.plugin.ACTION_WLAN_OFF";
    public static final String ACTION_WLAN_AP = "com.theta360.plugin.ACTION_WLAN_AP";
    public static final String ACTION_WLAN_CL = "com.theta360.plugin.ACTION_WLAN_CL";

    public static final String ACTION_DATABASE_UPDATE = "com.theta360.plugin.ACTION_DATABASE_UPDATE";
    public static final String TARGETS = "targets";

    public static final String ACTION_OLED_IMAGE_BLINK = "com.theta360.plugin.ACTION_OLED_IMAGE_BLINK";
    public static final String BITMAP = "bitmap";
    public static final String ACTION_OLED_TEXT_SHOW = "com.theta360.plugin.ACTION_OLED_TEXT_SHOW";
    public static final String TEXT_MIDDLE = "text-middle";
    public static final String TEXT_BOTTOM = "text-bottom";
    public static final String ACTION_OLED_HIDE = "com.theta360.plugin.ACTION_OLED_HIDE";

}

package com.dimagi.biometric;

public class Constants {
    public static final String CASE_ID_PARAM = "case_id";
    public static final String BIOMETRIC_TYPE_PARAM = "biometric_type";
    public static final String TEMPLATE_PARAM = "template";

    public enum initStatus {
        FAIL,
        SUCCESS,
        NO_NETWORK
    }

    // Input capture parameter defaults
    public static final int DEFAULT_PITCH_PARAM = 15;
    public static final int DEFAULT_YAW_PARAM = 15;
    public static final int DEFAULT_ROLL_PARAM = 15;
    public static final float DEFAULT_MASK_PARAM = 0.5f;
    public static final float DEFAULT_SUNGLASSES_PARAM = 0.5f;
    public static final float DEFAULT_EYES_CLOSED_PARAM = 0.8f;
    public static final int DEFAULT_BRISQUE_PARAM = 60;
    public static final float DEFAULT_IMAGE_CENTER_TOLERANCE_PARAM = 10.0f;
    public static final int DEFAULT_TIMEOUT_SECS_PARAM = 30;
    public static final boolean DEFAULT_AUTO_CAPTURE_ENABLED_PARAM = true;
    public static final float DEFAULT_DETECTOR_THRESHOLD_PARAM = 0.9f;
}

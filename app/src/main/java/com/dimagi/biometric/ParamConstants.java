package com.dimagi.biometric;

import ai.tech5.finger.utils.SegmentationMode;

public class ParamConstants {

    public static final String PITCH_NAME = "pitch";
    public static final int PITCH_DEFAULT = 15;
    public static final String YAW_NAME = "yaw";
    public static final int YAW_DEFAULT = 15;
    public static final String ROLL_NAME = "roll";
    public static final int ROLL_DEFAULT = 15;
    public static final String MASK_NAME = "mask";
    public static final float MASK_DEFAULT = 0.5f;
    public static final String SUNGLASSES_NAME = "sunglasses";
    public static final float SUNGLASSES_DEFAULT = 0.5f;
    public static final String EYES_CLOSED_NAME = "eyes_closed";
    public static final float EYES_CLOSED_DEFAULT = 0.8f;
    public static final String BRISQUE_NAME = "brisque";
    public static final int BRISQUE_DEFAULT = 60;
    public static final String IMAGE_CENTER_TOLERANCE_NAME = "image_center_tolerance";
    public static final float IMAGE_CENTER_TOLERANCE_DEFAULT = 10.0f;
    public static final String AUTO_CAPTURE_ENABLED_NAME = "auto_capture_enabled";
    public static final boolean AUTO_CAPTURE_ENABLED_DEFAULT = true;
    public static final String DETECTOR_THRESHOLD_NAME = "detector_threshold";
    public static final float DETECTOR_THRESHOLD_DEFAULT = 0.9f;
    public static final String SEGMENTATION_MODE_NAME = "segmentation_mode";
    public static final SegmentationMode SEGMENTATION_MODE_DEFAULT = SegmentationMode.SEGMENTATION_MODE_LEFT_AND_RIGHT_THUMBS;
    public static final String TIMEOUT_SECS_NAME = "timeout_secs";
    public static final int TIMEOUT_SECS_DEFAULT = 30;
    public static final String TEMPLATE_PROP_NAME = "template_prop_name";
    public static final String TEMPLATE_PROP_NAME_DEFAULT = "bio_template";
    public static final String ACCEPTANCE_THRESHOLD_NAME = "acceptance_threshold";
    public static final float ACCEPTANCE_THRESHOLD_DEFAULT = 4.0f;
}

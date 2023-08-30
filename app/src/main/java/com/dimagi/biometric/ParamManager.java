package com.dimagi.biometric;

import ai.tech5.finger.utils.SegmentationMode;

/**
 * Data class used for getting and setting various input parameters. Validation is done on setter
 * methods to ensure that the values stays within constraints.
 */
public class ParamManager {

    private int pitch = ParamConstants.PITCH_DEFAULT;
    private int yaw = ParamConstants.YAW_DEFAULT;
    private int roll = ParamConstants.ROLL_DEFAULT;
    private float mask = ParamConstants.MASK_DEFAULT;
    private float sunglasses = ParamConstants.SUNGLASSES_DEFAULT;
    private float eyesClosed = ParamConstants.EYES_CLOSED_DEFAULT;
    private int brisque = ParamConstants.BRISQUE_DEFAULT;
    private float imageCenterTolerance = ParamConstants.IMAGE_CENTER_TOLERANCE_DEFAULT;
    private boolean autoCaptureEnabled = ParamConstants.AUTO_CAPTURE_ENABLED_DEFAULT;
    private float detectorThreshold = ParamConstants.DETECTOR_THRESHOLD_DEFAULT;
    private int timeoutSecs = ParamConstants.TIMEOUT_SECS_DEFAULT;
    private SegmentationMode segmentationMode = ParamConstants.SEGMENTATION_MODE_DEFAULT;

    private int clampValue(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private float clampValue(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public ParamManager() {}

    public void setPitch(int pitch) {
        this.pitch = clampValue(pitch, -90, 90);
    }

    public int getPitch() {
        return pitch;
    }

    public void setYaw(int yaw) {
        this.yaw = clampValue(yaw, -90, 90);
    }

    public int getYaw() {
        return yaw;
    }

    public void setRoll(int roll) {
        this.roll = clampValue(roll, -90, 90);
    }

    public int getRoll() {
        return roll;
    }

    public void setMask(float mask) {
        this.mask = clampValue(mask, 0, 1);
    }

    public float getMask() {
        return mask;
    }

    public void setSunglasses(float sunglasses) {
        this.sunglasses = clampValue(sunglasses, 0, 1);
    }

    public float getSunglasses() {
        return sunglasses;
    }

    public void setEyesClosed(float eyesClosed) {
        this.eyesClosed = clampValue(eyesClosed, 0, 1);
    }

    public float getEyesClosed() {
        return eyesClosed;
    }

    public void setBrisque(int brisque) {
        this.brisque = clampValue(brisque, 0, 100);
    }

    public int getBrisque() {
        return brisque;
    }

    public void setImageCenterTolerance(float imageCenterTolerance) {
        this.imageCenterTolerance = clampValue(imageCenterTolerance, 2, 50);
    }

    public float getImageCenterTolerance() {
        return imageCenterTolerance;
    }

    public void setAutoCaptureEnabled(boolean autoCaptureEnabled) {
        this.autoCaptureEnabled = autoCaptureEnabled;
    }

    public boolean getAutoCaptureEnabled() {
        return autoCaptureEnabled;
    }

    public void setDetectorThreshold(float detectorThreshold) {
        this.detectorThreshold = clampValue(detectorThreshold,0, 1);
    }

    public float getDetectorThreshold() {
        return detectorThreshold;
    }

    public void setTimeoutSecs(int timeoutSecs) {
        this.timeoutSecs = clampValue(timeoutSecs, 1, 60);
    }

    public int getTimeoutSecs() {
        return timeoutSecs;
    }

    public void setSegmentationMode(String mode) {
        if (mode == null) {
            return;
        }
        switch (mode) {
            case "left_thumb":
                segmentationMode = SegmentationMode.SEGMENTATION_MODE_LEFT_THUMB;
                break;
            case "right_thumb":
                segmentationMode = SegmentationMode.SEGMENTATION_MODE_RIGHT_THUMB;
                break;
            case "left_index":
                segmentationMode = SegmentationMode.SEGMENTATION_MODE_LEFT_INDEX;
                break;
            case "right_index":
                segmentationMode = SegmentationMode.SEGMENTATION_MODE_RIGHT_INDEX;
                break;
            case "right_hand":
                segmentationMode = SegmentationMode.SEGMENTATION_MODE_RIGHT_SLAP;
                break;
            case "left_hand":
                segmentationMode = SegmentationMode.SEGMENTATION_MODE_LEFT_SLAP;
                break;
            case "both_thumbs":
            default:
                segmentationMode = SegmentationMode.SEGMENTATION_MODE_LEFT_AND_RIGHT_THUMBS;
        }
    }

    public SegmentationMode getSegmentationMode() {
        return segmentationMode;
    }
}

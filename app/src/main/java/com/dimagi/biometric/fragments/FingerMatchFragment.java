package com.dimagi.biometric.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.dimagi.biometric.ParamManager;
import com.dimagi.biometric.ParamConstants;
import com.dimagi.biometric.viewmodels.FingerMatchViewModel;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.MatcherCommon;
import ai.tech5.finger.utils.CaptureMode;
import ai.tech5.finger.utils.Finger;
import ai.tech5.finger.utils.FingerCaptureResult;
import ai.tech5.finger.utils.ImageConfiguration;
import ai.tech5.finger.utils.ImageType;
import ai.tech5.finger.utils.T5FingerCaptureController;
import ai.tech5.finger.utils.T5FingerCapturedListener;

public class FingerMatchFragment extends BaseMatchFragment implements T5FingerCapturedListener {
    private static final String TAG = "BIOMETRIC";

    protected FingerMatchViewModel fingerMatchViewModel;

    public FingerMatchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fingerMatchViewModel = new ViewModelProvider(requireActivity()).get(FingerMatchViewModel.class);
    }

    @Override
    protected void handleStartCapture() {
        T5FingerCaptureController fingerCaptureController = T5FingerCaptureController.getInstance();
        fingerCaptureController.setLicense("");

        fingerCaptureController.showElipses(true);
        fingerCaptureController.setLivenessCheck(true);

        ParamManager params = getParams();
        fingerCaptureController.setDetectorThreshold(params.getDetectorThreshold());
        fingerCaptureController.setSegmentationMode(params.getSegmentationMode());
        fingerCaptureController.setCaptureMode(CaptureMode.CAPTURE_MODE_SELF);
        fingerCaptureController.setIsGetQuality(true);

        ImageConfiguration segmentedFingersConfiguration = new ImageConfiguration();
        segmentedFingersConfiguration.setImageType(ImageType.IMAGE_TYPE_PNG);
//            segmentedFingersConfiguration.setIsCropImage(true);
//            segmentedFingersConfiguration.setCroppedImageWidth(512);
//            segmentedFingersConfiguration.setCroppedImageHeight(512);
        //0->Black color padding; 255->white color padding
        segmentedFingersConfiguration.setPaddingColor(255);
        fingerCaptureController.setSegmentedFingerImagesConfig(segmentedFingersConfiguration);

        ImageConfiguration slapConfig = new ImageConfiguration();
        slapConfig.setImageType(ImageType.IMAGE_TYPE_BMP);
        slapConfig.setCompressionRatio(10);
        slapConfig.setIsCropImage(false);

        fingerCaptureController.setSlapImagesConfig(slapConfig);
        fingerCaptureController.setTimeoutInSecs(params.getTimeoutSecs());
        fingerCaptureController.captureFingers(requireContext(), this);
    }

    @Override
    protected void handleCancelCapture() {
        fingerMatchViewModel.setIsCancelled(true);
    }

    @Override
    public void onSuccess(FingerCaptureResult fingerCaptureResult) {
        List<BioCommon.MatcherTemplate> fingerTemplates = new ArrayList<>();
        for (Finger res : fingerCaptureResult.fingers) {
            int fingerPos = res.pos;
            byte[] image = res.fingerImage;
            BioCommon.MatcherTemplate template = fingerMatchViewModel.createTemplate(image, fingerPos, Common.ImageFormat.PNG);
            if (template == null) {
                handleErrorMessage("Failed to capture finger image: Could not convert image to template");
                return;
            }
            fingerTemplates.add(template);
        }
        MatcherCommon.Record record = fingerMatchViewModel.createRecord(fingerTemplates);
        fingerMatchViewModel.setActiveRecord(record);
    }

    @Override
    public void onFailure(String s) {
        handleErrorMessage("Failed to capture finger image: " + s);
        Log.e(TAG, "Error capturing finger! Reason: " + s);
    }

    @Override
    public void onCancelled() {}

    @Override
    public void onTimedout() {
        handleErrorMessage("Failed to capture finger image: Timed out");
    }

    @Override
    protected ParamManager getParams() {
        Intent intent = requireActivity().getIntent();
        ParamManager params = new ParamManager();

        params.setTimeoutSecs(safeParseInteger(
                intent.getStringExtra(ParamConstants.TIMEOUT_SECS_NAME),
                ParamConstants.TIMEOUT_SECS_DEFAULT
        ));
        params.setDetectorThreshold(safeParseFloat(
                intent.getStringExtra(ParamConstants.DETECTOR_THRESHOLD_NAME),
                ParamConstants.DETECTOR_THRESHOLD_DEFAULT
        ));
        params.setSegmentationMode(intent.getStringExtra(ParamConstants.SEGMENTATION_MODE_NAME));
        return params;
    }
}

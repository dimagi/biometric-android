package com.dimagi.biometric.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.dimagi.biometric.ParamManager;
import com.dimagi.biometric.ParamConstants;
import com.dimagi.biometric.viewmodels.FaceMatchViewModel;
import com.phoenixcapture.camerakit.FaceBox;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.MatcherCommon;
import ai.tech5.pheonix.capture.controller.AirsnapFaceThresholds;
import ai.tech5.pheonix.capture.controller.FaceCaptureController;
import ai.tech5.pheonix.capture.controller.FaceCaptureListener;

public class FaceMatchFragment extends BaseMatchFragment implements FaceCaptureListener {
    private static final String TAG = "BIOMETRIC";

    protected FaceMatchViewModel faceMatchViewModel;

    public FaceMatchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        faceMatchViewModel = new ViewModelProvider(requireActivity()).get(FaceMatchViewModel.class);
    }

    @Override
    protected void handleStartCapture() {
        ParamManager params = getParams();

        FaceCaptureController controller = FaceCaptureController.getInstance();
        controller.setUseBackCamera(params.getUseBackCamera());
        controller.setAutoCapture(params.getAutoCaptureEnabled());
        controller.setCaptureTimeoutInSecs(params.getTimeoutSecs());

        AirsnapFaceThresholds thresholds = new AirsnapFaceThresholds();
        thresholds.setPITCH_THRESHOLD(params.getPitch());
        thresholds.setYAW_THRESHOLD(params.getYaw());
        thresholds.setRollThreshold(params.getRoll());
        thresholds.setMASK_THRESHOLD(params.getMask());
        thresholds.setSUNGLASS_THRESHOLD(params.getSunglasses());
        thresholds.setEYE_CLOSE_THRESHOLD(params.getEyesClosed());
        thresholds.setBRISQUE_THRESHOLD(params.getBrisque());
        thresholds.setFaceCentreToImageCentreTolerance(params.getImageCenterTolerance());

        controller.setAirsnapFaceThresholds(thresholds);
        controller.startFaceCapture("", requireContext(), this);
    }

    @Override
    protected void handleCancelCapture() {
        faceMatchViewModel.setIsCancelled(true);
    }

    @Override
    public void onFaceCaptured(byte[] faceImageBytes, FaceBox faceBox) {
        BioCommon.MatcherTemplate template = faceMatchViewModel.createTemplate(faceImageBytes, 0, Common.ImageFormat.JPEG);
        if (template == null) {
            handleErrorMessage("Failed to capture face image: Could not convert image to template");
            return;
        }

        List<BioCommon.MatcherTemplate> templateList = new ArrayList<>();
        templateList.add(template);

        MatcherCommon.Record record = faceMatchViewModel.createRecord(templateList);
        faceMatchViewModel.setActiveRecord(record);
    }

    @Override
    public void OnFaceCaptureFailed(String errorMessage) {
        handleErrorMessage("Failed to capture face image: " + errorMessage);
        Log.e(TAG, "Face capture failed! Reason: " + errorMessage);
    }

    @Override
    public void onCancelled() {
    }

    @Override
    public void onTimedout(byte[] faceImage) {
        handleErrorMessage("Failed to capture face image: Timed out");
    }

    @Override
    protected ParamManager getParams() {
        Intent intent = requireActivity().getIntent();
        ParamManager params = new ParamManager();

        params.setPitch(safeParseInteger(
                intent.getStringExtra(ParamConstants.PITCH_NAME), ParamConstants.PITCH_DEFAULT
        ));
        params.setYaw(safeParseInteger(
                intent.getStringExtra(ParamConstants.YAW_NAME), ParamConstants.YAW_DEFAULT
        ));
        params.setRoll(safeParseInteger(
                intent.getStringExtra(ParamConstants.ROLL_NAME), ParamConstants.ROLL_DEFAULT
        ));
        params.setMask(safeParseFloat(
                intent.getStringExtra(ParamConstants.MASK_NAME),
                ParamConstants.MASK_DEFAULT
        ));
        params.setSunglasses(safeParseFloat(
                intent.getStringExtra(ParamConstants.SUNGLASSES_NAME),
                ParamConstants.SUNGLASSES_DEFAULT
        ));
        params.setEyesClosed(safeParseFloat(
                intent.getStringExtra(ParamConstants.EYES_CLOSED_NAME),
                ParamConstants.EYES_CLOSED_DEFAULT
        ));
        params.setBrisque(safeParseInteger(
                intent.getStringExtra(ParamConstants.BRISQUE_NAME), ParamConstants.BRISQUE_DEFAULT
        ));
        params.setImageCenterTolerance(safeParseFloat(
                intent.getStringExtra(ParamConstants.IMAGE_CENTER_TOLERANCE_NAME),
                ParamConstants.IMAGE_CENTER_TOLERANCE_DEFAULT
        ));
        params.setTimeoutSecs(safeParseInteger(
                intent.getStringExtra(ParamConstants.TIMEOUT_SECS_NAME), ParamConstants.TIMEOUT_SECS_DEFAULT
        ));
        params.setAutoCaptureEnabled(safeParseBool(
                intent.getStringExtra(ParamConstants.AUTO_CAPTURE_ENABLED_NAME), ParamConstants.AUTO_CAPTURE_ENABLED_DEFAULT
        ));
        params.setUseBackCamera(safeParseBool(
                intent.getStringExtra(ParamConstants.USE_BACK_CAMERA_NAME), ParamConstants.USE_BACK_CAMERA_DEFAULT
        ));
        return params;
    }
}

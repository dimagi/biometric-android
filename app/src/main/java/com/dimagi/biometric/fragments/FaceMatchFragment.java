package com.dimagi.biometric.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.dimagi.biometric.viewmodels.FaceMatchViewModel;
import com.phoenixcapture.camerakit.FaceBox;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.MatcherCommon;
import ai.tech5.pheonix.capture.controller.FaceCaptureController;
import ai.tech5.pheonix.capture.controller.FaceCaptureListener;

public class FaceMatchFragment extends BaseMatchFragment implements FaceCaptureListener{
    private static final String TAG = "BIOMETRIC";

    protected FaceMatchViewModel faceMatchViewModel;

    public FaceMatchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        faceMatchViewModel = new ViewModelProvider(requireActivity()).get(FaceMatchViewModel.class);
    }

    @Override
    protected void handleStartCapture() {
        FaceCaptureController controller = FaceCaptureController.getInstance();
        controller.setUseBackCamera(false);
        controller.setAutoCapture(true);

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
}

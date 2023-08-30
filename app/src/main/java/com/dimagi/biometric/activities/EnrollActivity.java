package com.dimagi.biometric.activities;

import com.dimagi.biometric.OmniMatchUtil;

import org.commcare.commcaresupportlibrary.BiometricUtils;
import org.commcare.commcaresupportlibrary.identity.IdentityResponseBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.FingerCommon;
import Tech5.OmniMatch.MatcherCommon;

public class EnrollActivity extends BaseActivity {
    @Override
    protected void onCaptureSuccess(MatcherCommon.Record activeRecord) {
        Map<BiometricUtils.BiometricIdentifier, byte[]> templateDataList = new HashMap<>();
        if (biometricType == BioCommon.BioType.Face) {
            byte[] templateData = activeRecord.getFace().getTemplateData().getData().toByteArray();
            templateDataList.put(BiometricUtils.BiometricIdentifier.FACE, templateData);
        } else {
            for (BioCommon.MatcherTemplate template : activeRecord.getNnFingersList()) {
                byte[] templateData = template.getTemplateData().getData().toByteArray();
                templateDataList.put(
                        OmniMatchUtil.getBiometricIdentifierFromPosition(FingerCommon.NistFingerPosition.values()[template.getPosition()]),
                        templateData
                );
            }
        }
        String randId = UUID.randomUUID().toString();
        IdentityResponseBuilder.registrationResponse(randId, templateDataList).finalizeResponse(this);
    }

    @Override
    protected void onCaptureCancelled() {
        Map<BiometricUtils.BiometricIdentifier, byte[]> templateDataList = new HashMap<>();
        IdentityResponseBuilder.registrationResponse(caseId, templateDataList).finalizeResponse(this);
    }

    @Override
    protected ArrayList<String> validateRequiredParams() {
        return new ArrayList<>();
    }
}

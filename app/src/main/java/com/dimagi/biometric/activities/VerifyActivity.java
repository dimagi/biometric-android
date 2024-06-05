package com.dimagi.biometric.activities;

import com.dimagi.biometric.R;

import org.commcare.commcaresupportlibrary.identity.IdentityResponseBuilder;
import org.commcare.commcaresupportlibrary.identity.model.MatchResult;
import org.commcare.commcaresupportlibrary.identity.model.MatchStrength;

import java.util.ArrayList;

import Tech5.OmniMatch.MatcherCommon;

public class VerifyActivity extends BaseActivity {
    @Override
    protected void onCaptureSuccess(MatcherCommon.Record activeRecord) {
        templateRecord = parseBiometricTemplates();
        if (templateRecord != null) {
            templateViewModel.insertRecord(templateRecord, caseId);
        }
        float score = templateViewModel.verifyRecord(activeRecord, caseId);
        finalizeResponse(score);
    }

    @Override
    protected void onCaptureCancelled() {
        finalizeResponse(0f);
    }

    private void finalizeResponse(float score) {
        int confidencePercentage = (int)(score * 100);
        MatchStrength matchStrength = getMatchStrength(score);
        IdentityResponseBuilder.verificationResponse(
                caseId, new MatchResult(confidencePercentage, matchStrength)
        ).finalizeResponse(this);
    }
}

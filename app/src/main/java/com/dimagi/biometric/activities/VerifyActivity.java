package com.dimagi.biometric.activities;

import android.os.Bundle;

import org.commcare.commcaresupportlibrary.identity.IdentityResponseBuilder;
import org.commcare.commcaresupportlibrary.identity.model.MatchResult;
import org.commcare.commcaresupportlibrary.identity.model.MatchStrength;

import Tech5.OmniMatch.MatcherCommon;

public class VerifyActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MatcherCommon.Record caseRecord = getRecordFromIntent();
        if (caseRecord != null && caseId != null) {
            templateViewModel.insertRecord(caseRecord, caseId);
        }
    }

    @Override
    protected void onCaptureSuccess(MatcherCommon.Record activeRecord) {
        float score = templateViewModel.verifyRecord(activeRecord, caseId);
        IdentityResponseBuilder.verificationResponse(
                caseId, new MatchResult((int) (score * 100), getMatchStrength(score))
        ).finalizeResponse(this);
    }

    @Override
    protected void onCaptureCancelled() {
        IdentityResponseBuilder.verificationResponse(
                caseId, new MatchResult(0, MatchStrength.ONE_STAR)
        ).finalizeResponse(this);
    }

    @Override
    protected ArrayList<String> validateRequiredParams() {
        ArrayList<String> errors = new ArrayList<>();
        if (caseId == null) {
            errors.add(getText(R.string.missing_case_id).toString());
        }
        if (templateStr == null) {
            errors.add(getText(R.string.missing_template_str).toString());
        }
        return errors;
    }
}

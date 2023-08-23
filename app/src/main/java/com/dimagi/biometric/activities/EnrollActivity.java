package com.dimagi.biometric.activities;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.MatcherCommon;

public class EnrollActivity extends BaseActivity {
    @Override
    protected void onCaptureSuccess(MatcherCommon.Record activeRecord) {
        // TODO: Need to add in position to returned data
        List<byte[]> templateDataList = new ArrayList<>();
        if (biometricType == BioCommon.BioType.Face) {
            byte[] templateData = activeRecord.getFace().getTemplateData().getData().toByteArray();
            templateDataList.add(templateData);
        } else {
            for (BioCommon.MatcherTemplate template : activeRecord.getNnFingersList()) {
                byte[] templateData = template.getTemplateData().getData().toByteArray();
                templateDataList.add(templateData);
            }
        }

        // TODO: Return to CC with template(s). This is pending required changes to the CC Support Library
        // IdentityResponseBuilder.registrationResponse(caseId).finalizeResponse(this);
    }

    @Override
    protected void onCaptureCancelled() {
        // TODO: Return to CC with cancelled output. This is pending required changes to the CC Support Library
        // IdentityResponseBuilder.registrationResponse(caseId).finalizeResponse(this);
    }
}

package com.dimagi.biometric.activities;

import com.dimagi.biometric.ParamConstants;
import com.dimagi.biometric.R;

import org.commcare.commcaresupportlibrary.CaseUtils;
import org.commcare.commcaresupportlibrary.identity.IdentityResponseBuilder;
import org.commcare.commcaresupportlibrary.identity.model.IdentificationMatch;
import org.commcare.commcaresupportlibrary.identity.model.MatchResult;
import org.commcare.commcaresupportlibrary.identity.model.MatchStrength;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Matcher;
import Tech5.OmniMatch.MatcherCommon;

public class SearchActivity extends BaseActivity {

    @Override
    protected void onCaptureSuccess(MatcherCommon.Record activeRecord) {
        fetchCaseData();

        Matcher.RecordsResult result = templateViewModel.identifyRecord(activeRecord, 10.0f, 5);
        ArrayList<IdentificationMatch> identifications = getIdentificationsFromResult(result);
        IdentityResponseBuilder.identificationResponse(identifications).finalizeResponse(this);
    }

    @Override
    protected void onCaptureCancelled() {
        ArrayList<IdentificationMatch> identifications = new ArrayList<>();
        IdentityResponseBuilder.identificationResponse(identifications).finalizeResponse(this);
    }

    private void fetchCaseData() {
        List<String> caseIds = CaseUtils.getCaseIds(this);
        for (String caseId : caseIds) {
            byte[] templateData = CaseUtils.getCaseProperty(this, caseId, TEMPLATE_PARAM).getBytes();
            // TODO: Need to get position from case data
            int position = 0;
            BioCommon.MatcherTemplate template = templateViewModel.bytesToTemplate(templateData, position);
            List<BioCommon.MatcherTemplate> templateList = new ArrayList<>();
            templateList.add(template);
            MatcherCommon.Record record = templateViewModel.createRecord(templateList);
            templateViewModel.insertRecord(record, caseId);
        }
    }

    private ArrayList<IdentificationMatch> getIdentificationsFromResult(Matcher.RecordsResult result) {
        ArrayList<IdentificationMatch> identifications = new ArrayList<>();
        if (result == null) {
            return identifications;
        }

        for (MatcherCommon.RecordResult resultItem : result.getResultsList()) {
            MatcherCommon.Candidate candidate = resultItem.getCandidate();
            String matchGuid = candidate.getUid();
            float score;
            if (biometricType == BioCommon.BioType.Face) {
                score = candidate.getScores().getFace().getScore();
            } else {
                score = candidate.getScores().getFinger().getScore();
            }
            MatchStrength matchStrength = getMatchStrength(score);
            identifications.add(new IdentificationMatch(matchGuid, new MatchResult((int)(score * 100), matchStrength)));
        }
        return identifications;
    }

    @Override
    protected ArrayList<String> validateRequiredParams() {
        ArrayList<String> errors = new ArrayList<>();
        if (caseId == null) {
            errors.add(getText(R.string.missing_case_id).toString());
        }
        return errors;
    }
}

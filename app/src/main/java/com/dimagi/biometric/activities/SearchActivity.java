package com.dimagi.biometric.activities;

import com.dimagi.biometric.ParamConstants;
import com.dimagi.biometric.R;

import org.commcare.commcaresupportlibrary.CaseUtils;
import org.commcare.commcaresupportlibrary.identity.IdentityResponseBuilder;
import org.commcare.commcaresupportlibrary.identity.model.IdentificationMatch;
import org.commcare.commcaresupportlibrary.identity.model.MatchResult;
import org.commcare.commcaresupportlibrary.identity.model.MatchStrength;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;


import java.util.ArrayList;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Matcher;
import Tech5.OmniMatch.MatcherCommon;

public class SearchActivity extends BaseActivity {
    private String templatePropName = ParamConstants.TEMPLATE_PROP_NAME_DEFAULT;
    private float acceptanceThreshold = ParamConstants.ACCEPTANCE_THRESHOLD_DEFAULT;
    private final String TAG = "BIOMETRIC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String propName = intent.getStringExtra(ParamConstants.TEMPLATE_PROP_NAME);
        if (propName != null) {
            templatePropName = propName;
        }
        acceptanceThreshold = intent.getFloatExtra(
                ParamConstants.ACCEPTANCE_THRESHOLD_NAME, ParamConstants.ACCEPTANCE_THRESHOLD_DEFAULT
        );
    }

    @Override
    protected void onCaptureSuccess(MatcherCommon.Record activeRecord) {
        fetchCaseData();
        Matcher.RecordsResult result = templateViewModel.identifyRecord(activeRecord, acceptanceThreshold, 5);
        ArrayList<IdentificationMatch> identifications = getIdentificationsFromResult(result);
        IdentityResponseBuilder.identificationResponse(identifications).finalizeResponse(this);
    }

    @Override
    protected void onCaptureCancelled() {
        finishWorkflow(new ArrayList<>());
    }

    private void fetchCaseData() {
        Cursor caseCursor = CaseUtils.getCaseMetaData(this);
        while (caseCursor.moveToNext()) {
            int colIndex = caseCursor.getColumnIndex(CASE_ID_PARAM);
            if (colIndex < 0) {
                continue;
            }
            String caseIdProp = caseCursor.getString(colIndex);
            String rawTemplateStr = CaseUtils.getCaseProperty(this, caseIdProp, templatePropName);
            if (rawTemplateStr == null) {
                continue;
            }

            try {
                MatcherCommon.Record record = templateViewModel.getRecordFromTemplateStr(rawTemplateStr);
                templateViewModel.insertRecord(record, caseIdProp);
            } catch (NullPointerException e) {
                Log.e(TAG, "Null pointer exception on creating record: " + e);
            }
        }
    }

    private void finishWorkflow(ArrayList<IdentificationMatch> identifications) {
        if (identifications.size() == 0) {
            identifications.add(
                    new IdentificationMatch(caseId, new MatchResult(0, MatchStrength.ONE_STAR))
            );
        }
        IdentityResponseBuilder.identificationResponse(identifications).finalizeResponse(this);
    }

    private ArrayList<IdentificationMatch> getIdentificationsFromResult(Matcher.RecordsResult result) {
        ArrayList<IdentificationMatch> identifications = new ArrayList<>();
        if (result == null) {
            return identifications;
        }

        for (MatcherCommon.RecordResult resultItem : result.getResultsList()) {
            String matchGuid = resultItem.getCandidate().getUid();
            float score;
            if (biometricType == BioCommon.BioType.Face) {
                score = resultItem.getCandidate().getScores().getFace().getScore();
            } else {
                score = resultItem.getCandidate().getScores().getFinger().getScore();
            }
            MatchStrength matchStrength = getMatchStrength(score);
            int confidencePercentage = (int)(score * 100);
            identifications.add(
                    new IdentificationMatch(matchGuid, new MatchResult(confidencePercentage, matchStrength))
            );
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

package com.dimagi.biometric.activities;

import com.dimagi.biometric.OmniMatchUtil;
import com.dimagi.biometric.ParamConstants;
import com.dimagi.biometric.R;

import org.commcare.commcaresupportlibrary.CaseUtils;
import org.commcare.commcaresupportlibrary.identity.BiometricIdentifier;
import org.commcare.commcaresupportlibrary.identity.IdentityResponseBuilder;
import org.commcare.commcaresupportlibrary.identity.model.IdentificationMatch;
import org.commcare.commcaresupportlibrary.identity.model.MatchResult;
import org.commcare.commcaresupportlibrary.identity.model.MatchStrength;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import java.util.ArrayList;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.FingerCommon;
import Tech5.OmniMatch.Matcher;
import Tech5.OmniMatch.MatcherCommon;

public class SearchActivity extends BaseActivity {
    private float acceptanceThreshold = ParamConstants.ACCEPTANCE_THRESHOLD_DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        acceptanceThreshold = intent.getFloatExtra(
                ParamConstants.ACCEPTANCE_THRESHOLD_NAME, ParamConstants.ACCEPTANCE_THRESHOLD_DEFAULT
        );
    }

    @Override
    protected void onCaptureSuccess(MatcherCommon.Record activeRecord) {
        ArrayList<BiometricIdentifier> bioIds = getBiometricIdentifiers(activeRecord);
        fetchCaseData(bioIds);
        Matcher.RecordsResult result = templateViewModel.identifyRecord(activeRecord, acceptanceThreshold, 5);
        ArrayList<IdentificationMatch> identifications = getIdentificationsFromResult(result);
        IdentityResponseBuilder.identificationResponse(identifications).finalizeResponse(this);
    }

    @Override
    protected void onCaptureCancelled() {
        ArrayList<IdentificationMatch> identifications = new ArrayList<>();
        identifications.add(new IdentificationMatch(caseId, new MatchResult(0, MatchStrength.ONE_STAR)));
        IdentityResponseBuilder.identificationResponse(identifications).finalizeResponse(this);
    }

    /**
     * This function retrieves which biometric identifiers were used for capture and returns this as
     * an array. This is used to identify which case properties to look for when fetching biometric case data
     */
    private ArrayList<BiometricIdentifier> getBiometricIdentifiers(MatcherCommon.Record record) {
        ArrayList<BiometricIdentifier> biometricIdentifiers = new ArrayList<>();
        if (biometricType == BioCommon.BioType.Face) {
            biometricIdentifiers.add(BiometricIdentifier.FACE);
        } else {
            for (BioCommon.MatcherTemplate fingerTemplate : record.getNnFingersList()) {
                BiometricIdentifier bioId = OmniMatchUtil.getBiometricIdentifierFromPosition(FingerCommon.NistFingerPosition.values()[fingerTemplate.getPosition()]);
                biometricIdentifiers.add(bioId);
            }
        }
        return biometricIdentifiers;
    }

    private void fetchCaseData(ArrayList<BiometricIdentifier> bioIds) {
        Cursor caseCursor = CaseUtils.getCaseMetaData(this);
        while (caseCursor.moveToNext()) {
            int colIndex = caseCursor.getColumnIndex(CASE_ID_PARAM);
            if (colIndex < 0) {
                continue;
            }
            String caseIdProp = caseCursor.getString(colIndex);
            processCaseData(caseIdProp, bioIds);
        }
    }

    /**
     * Go through cases in CommCare and insert records, retrieving only templates that were used in capture
     */
    private void processCaseData(String caseIdProp, ArrayList<BiometricIdentifier> bioIds) {
        ArrayList<BioCommon.MatcherTemplate> templateList = new ArrayList<>();
        for (BiometricIdentifier bioId : bioIds) {
            String templateStr = CaseUtils.getCaseProperty(this, caseIdProp, bioId.getCalloutResponseKey());
            if (templateStr == null || templateStr.equals("")) {
                continue;
            }

            BioCommon.MatcherTemplate template = templateViewModel.getMatcherTemplateFromStr(templateStr, bioId);
            if (template != null) {
                templateList.add(template);
            }
        }

        if (templateList.size() > 0) {
            MatcherCommon.Record record = templateViewModel.createRecord(templateList);
            templateViewModel.insertRecord(record, caseIdProp);
        }
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

package com.dimagi.biometric.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dimagi.biometric.OmniMatchUtil;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.List;

import Tech5.OmniMatch.AuthMatcher;
import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.FingerCommon;
import Tech5.OmniMatch.JNI.Matchers.AuthMatcherInstance;
import Tech5.OmniMatch.JNI.Matchers.AuthMatcherNative;
import Tech5.OmniMatch.JNI.Matchers.MatcherInstance;
import Tech5.OmniMatch.JNI.Matchers.MatcherNative;
import Tech5.OmniMatch.JNI.OmniMatchException;
import Tech5.OmniMatch.JNI.TemplateCreators.TemplateCreatorNNInstance;
import Tech5.OmniMatch.JNI.TemplateCreators.TemplateCreatorNNNative;
import Tech5.OmniMatch.Matcher;
import Tech5.OmniMatch.MatcherCommon;
import Tech5.OmniMatch.TemplateCreatorNn;

public class FingerMatchViewModel extends BaseTemplateViewModel {
    private static final String TAG = "BIOMETRIC";

    private TemplateCreatorNNNative templateCreatorNNNative = null;
    private TemplateCreatorNNInstance templateCreatorNNInstance = null;
    private AuthMatcherNative authMatcherNative = null;
    private AuthMatcherInstance authMatcherInstance = null;

    private MatcherNative matcherNative = null;
    private MatcherInstance matcherInstance = null;


    public FingerMatchViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    public void init() {
        try {
            authMatcherNative = new AuthMatcherNative();
            AuthMatcher.AuthMatcherConfiguration authMatcherConfiguration = AuthMatcher.AuthMatcherConfiguration.newBuilder()
                    .setAlgorithms(BioCommon.Algorithms.newBuilder().setFinger(FingerCommon.FingerAlgorithm.ChiFinger_100Light)
                    .build()).setDebugMode(true).build();
            authMatcherInstance = authMatcherNative.CreateInstance(authMatcherConfiguration);

            // TODO: Need to find a way to make gallery size configurable. CommCare could have more cases
            matcherNative = new MatcherNative();
            Matcher.MatcherConfiguration matcherConfiguration = Matcher.MatcherConfiguration.newBuilder()
                    .setAlgorithms(BioCommon.Algorithms.newBuilder()
                    .setFinger(FingerCommon.FingerAlgorithm.ChiFinger_100Light).build())
                    .setBase(Common.BaseConfiguration.newBuilder()
                    .setThreadsNumber(1).setDebugMode(true).build()).setMaxGallerySize(1000)
                    .setIdentifyMode(Matcher.IdentifyMode.FastIdentify).setCheckDoubles(false).build();
            matcherInstance = matcherNative.CreateInstance(matcherConfiguration);

            templateCreatorNNNative = new TemplateCreatorNNNative();
            TemplateCreatorNn.TemplateCreatorNnConfiguration templateCreatorNnConfiguration = TemplateCreatorNn.TemplateCreatorNnConfiguration.newBuilder()
                    .setAlgorithm(BioCommon.Algorithm.newBuilder()
                    .setFinger(FingerCommon.FingerAlgorithm.ChiFinger_100Light))
                    .setBase(Common.BaseConfiguration.newBuilder().setDebugMode(false).setThreadsNumber(10).build())
                    .setBatch(Common.BatchConfiguration.newBuilder().setBatchSize(10).build())
                    .setBioType(BioCommon.BioType.Finger)
                    .build();
            templateCreatorNNInstance = templateCreatorNNNative.CreateInstance(templateCreatorNnConfiguration);
        } catch (OmniMatchException ex) {
            Log.e(TAG, "Failed to init finger view model: " + ex.getResultCode());
        }

        omniMatchUtil = new OmniMatchUtil();
    }

    public void insertRecord(MatcherCommon.Record record, String id) {
        try {
            omniMatchUtil.insertRecord(matcherNative, matcherInstance, record, id);
        } catch (OmniMatchException ex) {
            Log.e(TAG, "Failed to insert finger record: " + ex.getResultCode());
        }
    }

    @Override
    public float verifyRecord(MatcherCommon.Record record, String id) {
        try {
            Matcher.RecordsResult result = omniMatchUtil.verifyRecord(matcherNative, matcherInstance, record, id);
            return result.getResultsList().get(0).getCandidate().getScores().getFinger().getScore();
        } catch (OmniMatchException ex) {
            Log.e(TAG, "Failed to verify finger record: " + ex.getResultCode());
        } catch (IOException ex) {
            Log.e(TAG, "IO exception on verify finger record");
        }
        return 0.0f;
    }

    @Override
    public Matcher.RecordsResult identifyRecord(MatcherCommon.Record record, float threshold, int maxCandidates) {
        try {
            return omniMatchUtil.identifyRecord(matcherNative, matcherInstance, record, threshold, maxCandidates);
        } catch(OmniMatchException ex) {
            Log.e(TAG, "Failed to identify finger record: " + ex.getResultCode());
        } catch (IOException ex) {
            Log.e(TAG, "IO exception on identify finger record");
        }
        return null;
    }

    @Override
    public BioCommon.MatcherTemplate createTemplate(byte[] image, int position, Common.ImageFormat imageFormat) {
        try {
            return omniMatchUtil.createFingerTemplate(templateCreatorNNNative, templateCreatorNNInstance, image, position, imageFormat);
        } catch (OmniMatchException | InvalidProtocolBufferException ex) {
            Log.e(TAG, "Failed to create finger template");
        }
        return null;
    }

    @Override
    public MatcherCommon.Record createRecord(List<BioCommon.MatcherTemplate> templates) {
        return omniMatchUtil.createFingerRecord(templates);
    }

    @Override
    public BioCommon.MatcherTemplate bytesToTemplate(byte[] templateData, int position) {
        if (omniMatchUtil == null) {
            return null;
        }
        return omniMatchUtil.bytesToTemplate(templateData, position);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            if (authMatcherNative != null) {
                authMatcherNative.DeleteInstance(authMatcherInstance);
            }
            if (matcherNative != null) {
                matcherNative.DeleteInstance(matcherInstance);
            }
            if (templateCreatorNNNative != null) {
                templateCreatorNNNative.DeleteInstance(templateCreatorNNInstance);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing finger view model");
        }
    }
}

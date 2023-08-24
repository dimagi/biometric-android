package com.dimagi.biometric.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.List;

import Tech5.OmniMatch.AuthMatcher;
import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.FaceCommon;
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

public class FaceMatchViewModel extends BaseTemplateViewModel {
    private static final String TAG = "BIOMETRIC";

    private TemplateCreatorNNNative templateCreatorNNNative = null;
    private TemplateCreatorNNInstance templateCreatorNNInstance = null;
    private MatcherNative matcherNative = null;
    private MatcherInstance matcherInstance = null;
    private AuthMatcherInstance authMatcherInstance = null;
    private AuthMatcherNative authMatcherNative = null;

    public FaceMatchViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    public void init() {
        try {
            authMatcherNative = new AuthMatcherNative();
            AuthMatcher.AuthMatcherConfiguration authMatcherConfiguration = AuthMatcher.AuthMatcherConfiguration
                    .newBuilder().setAlgorithms(BioCommon.Algorithms.newBuilder()
                    .setFace(FaceCommon.FaceAlgorithm.DetFace_100Light).build()).setDebugMode(true).build();
            authMatcherInstance = authMatcherNative.CreateInstance(authMatcherConfiguration);

            matcherNative = new MatcherNative();
            Matcher.MatcherConfiguration matcherConfiguration = Matcher.MatcherConfiguration.newBuilder()
                    .setAlgorithms(BioCommon.Algorithms.newBuilder()
                    .setFace(FaceCommon.FaceAlgorithm.DetFace_100Light).build())
                    .setBase(Common.BaseConfiguration.newBuilder()
                    .setThreadsNumber(1).setDebugMode(true).build()).setMaxGallerySize(1000)
                    .setIdentifyMode(Matcher.IdentifyMode.FastIdentify).setCheckDoubles(false).build();
            matcherInstance = matcherNative.CreateInstance(matcherConfiguration);

            templateCreatorNNNative = new TemplateCreatorNNNative();
            TemplateCreatorNn.TemplateCreatorNnConfiguration templateCreatorNnFaceLightConfiguration
                    = TemplateCreatorNn.TemplateCreatorNnConfiguration.newBuilder()
                    .setAlgorithm(BioCommon.Algorithm.newBuilder().setFace(FaceCommon.FaceAlgorithm.DetFace_100Light))
                    .setBase(Common.BaseConfiguration.newBuilder().setDebugMode(true).setThreadsNumber(10).build())
                    .setBatch(Common.BatchConfiguration.newBuilder().setBatchSize(10).build())
                    .setBioType(BioCommon.BioType.Face)
                    .build();
            templateCreatorNNInstance = templateCreatorNNNative.CreateInstance(templateCreatorNnFaceLightConfiguration);
        } catch (OmniMatchException ex) {
            Log.e(TAG, "Failed to init face view model: " + ex.getResultCode());
        }

        omniMatchViewModel = new OmniMatchViewModel();
    }

    @Override
    public void insertRecord(MatcherCommon.Record record, String id) {
        try {
            omniMatchViewModel.insertRecord(matcherNative, matcherInstance, record, id);
        } catch (OmniMatchException ex) {
            Log.e(TAG, "Failed to insert finger record: " + ex.getResultCode());
        }
    }

    @Override
    public float verifyRecord(MatcherCommon.Record record, String id) {
        try {
            Matcher.RecordsResult result = omniMatchViewModel.verifyRecord(matcherNative, matcherInstance, record, id);
            return result.getResultsList().get(0).getCandidate().getScores().getFace().getScore();
        } catch (OmniMatchException ex) {
            Log.e(TAG, "Failed to verify face record: " + ex.getResultCode());
        } catch (IOException ex) {
            Log.e(TAG, "IO exception on verify face record");
        }
        return 0.0f;
    }

    @Override
    public Matcher.RecordsResult identifyRecord(MatcherCommon.Record record, float threshold, int maxCandidates) {
        try {
            return omniMatchViewModel.identifyRecord(matcherNative, matcherInstance, record, threshold, maxCandidates);
        } catch(OmniMatchException ex) {
            Log.e(TAG, "Failed to identify face record: " + ex.getResultCode());
        } catch (IOException ex) {
            Log.e(TAG, "IO exception on identify face record");
        }
        return null;
    }

    @Override
    public BioCommon.MatcherTemplate createTemplate(byte[] image, int position, Common.ImageFormat imageFormat) {
        try {
            return omniMatchViewModel.createFaceTemplate(templateCreatorNNNative, templateCreatorNNInstance, image, imageFormat);
        } catch (OmniMatchException | InvalidProtocolBufferException ex) {
            Log.e(TAG, "Error creating face template");
        }
        return null;
    }

    @Override
    public MatcherCommon.Record createRecord(List<BioCommon.MatcherTemplate> templates) {
        if (templates.size() > 0) {
            return omniMatchViewModel.createFaceRecord(templates.get(0));
        }
        return null;
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
            if (templateCreatorNNNative != null) {
                templateCreatorNNNative.DeleteInstance(templateCreatorNNInstance);
            }
            if (matcherNative != null) {
                matcherNative.DeleteInstance(matcherInstance);
            }
            if (authMatcherNative != null) {
                authMatcherNative.DeleteInstance(authMatcherInstance);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing face view model");
        }
    }
}

package com.dimagi.biometric;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.commcare.commcaresupportlibrary.identity.BiometricIdentifier;

import java.io.IOException;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.FingerCommon;
import Tech5.OmniMatch.JNI.Matchers.MatcherInstance;
import Tech5.OmniMatch.JNI.Matchers.MatcherNative;
import Tech5.OmniMatch.JNI.OmniMatchException;
import Tech5.OmniMatch.JNI.TemplateCreators.TemplateCreatorNNInstance;
import Tech5.OmniMatch.JNI.TemplateCreators.TemplateCreatorNNNative;
import Tech5.OmniMatch.Matcher;
import Tech5.OmniMatch.MatcherCommon;
import Tech5.OmniMatch.TemplateCreatorCommon;

public class OmniMatchUtil {

    private Common.Image deserializeImage(byte[] image, Common.ImageFormat imageFormat, String batchIdentifier) {
        Common.Image.Builder builder = Common.Image.newBuilder();
        if (batchIdentifier != null) {
            builder.setBatchIdentifier(Common.BatchIdentifier.newBuilder().setId(batchIdentifier).build());
        }
        return builder.setBytes(ByteString.copyFrom(image)).setFormat(imageFormat).build();
    }

    public BioCommon.MatcherTemplate bytesToTemplate(byte[] templateData, int position) {
        ByteString templateBytes = ByteString.copyFrom(templateData);
        BioCommon.Template template = BioCommon.Template.newBuilder().setData(templateBytes).setQuality(100).build();
        return createMatcherTemplate(template, position);
    }

    public MatcherCommon.Record createFingerRecord(List<BioCommon.MatcherTemplate> fingerTemplates) {
        MatcherCommon.Record.Builder matcherCommonRecordBuilder = MatcherCommon.Record.newBuilder();
        if (fingerTemplates != null && !fingerTemplates.isEmpty()) {
            for (BioCommon.MatcherTemplate fingerNNTemplate : fingerTemplates) {
                matcherCommonRecordBuilder.addNnFingers(fingerNNTemplate);
            }
        }
        return matcherCommonRecordBuilder.build();
    }

    public MatcherCommon.Record createFaceRecord(BioCommon.MatcherTemplate faceTemplate) {
        MatcherCommon.Record.Builder matcherCommonRecordBuilder = MatcherCommon.Record.newBuilder();
        if (faceTemplate != null) {
            matcherCommonRecordBuilder.setFace(faceTemplate);
        }
        return matcherCommonRecordBuilder.build();
    }

    public boolean insertRecord(MatcherNative matcherNative, MatcherInstance matcherInstance, MatcherCommon.Record record, String id) throws OmniMatchException {
        Matcher.InsertRecordRequest insertRecordRequest = Matcher.InsertRecordRequest.newBuilder()
                .setRecord(record)
                .setId(id)
                .build();
        Common.ResultCode resultCode = matcherNative.InsertRecord(matcherInstance, insertRecordRequest);
        return Common.ResultCode.Success.getNumber() == resultCode.getNumber();
    }

    public Matcher.RecordsResult verifyRecord(MatcherNative matcherNative, MatcherInstance matcherInstance,
                                              MatcherCommon.Record record, String id) throws OmniMatchException, IOException {

        Matcher.VerifyRecord1to1Request verifyRecord1to1Request = Matcher.VerifyRecord1to1Request.newBuilder()
                .setRecord(record)
                .setRecordGalleryId(id)
                .build();
        return matcherNative.VerifyRecordOneToOne(matcherInstance, verifyRecord1to1Request);
    }

    /**
     * @param threshold A value in the range of 0-20, with 20 being a complete match.
     */
    public Matcher.RecordsResult identifyRecord(MatcherNative matcherNative, MatcherInstance matcherInstance,
                                                MatcherCommon.Record record, float threshold, int maxCandidates) throws OmniMatchException, IOException {
        float validThreshold = Math.max(0.0f, Math.min(20.0f, threshold));
        Matcher.IdentifyRecordRequest identifyRecordRequest = Matcher.IdentifyRecordRequest.newBuilder()
                .setRecord(record)
                .setMatchingParameters(MatcherCommon.MatchingParameters.newBuilder().setFingerThreshold(validThreshold).setFaceThreshold(validThreshold))
                .setMaxCandidates(maxCandidates)
                .build();
        return matcherNative.IdentifyRecord(matcherInstance, identifyRecordRequest);
    }

    public BioCommon.MatcherTemplate createFingerTemplate(TemplateCreatorNNNative templateCreatorNNNative,
                                                    TemplateCreatorNNInstance templateCreatorNNInstance,
                                                    byte[] image, int position, Common.ImageFormat imageFormat) throws OmniMatchException, InvalidProtocolBufferException {
        TemplateCreatorCommon.CreateTemplateRequest createTemplateRequest = TemplateCreatorCommon.CreateTemplateRequest.newBuilder()
                .addImages(deserializeImage(image, imageFormat, String.valueOf(position)))
                .setDoSegmentation(false)
                .build();

        TemplateCreatorCommon.CreateTemplateResponse createTemplateResponse
                = templateCreatorNNNative.CreateTemplate(templateCreatorNNInstance, createTemplateRequest);

        if (createTemplateResponse.getResultCode() == Common.ResultCode.Success) {
            TemplateCreatorCommon.CreateTemplateResult createTemplateResult = createTemplateResponse.getResultsList().get(0);
            BioCommon.Template template = createTemplateResult.getTemplateResult().getTemplate();
            return createMatcherTemplate(template, position);
        }
        return null;
    }

    public BioCommon.MatcherTemplate createFaceTemplate(TemplateCreatorNNNative templateCreatorNNNative,
                                                  TemplateCreatorNNInstance templateCreatorNNInstance,
                                                  byte[] image, Common.ImageFormat imageFormat) throws OmniMatchException, InvalidProtocolBufferException {

        Common.Image faceImage = deserializeImage(image, imageFormat, null);
        TemplateCreatorCommon.CreateTemplateRequest createTemplateRequest = TemplateCreatorCommon
                .CreateTemplateRequest.newBuilder()
                .addImages(faceImage)
                .setDoSegmentation(false)
                .build();

        TemplateCreatorCommon.CreateTemplateResponse createTemplateResponse = templateCreatorNNNative
                .CreateTemplate(templateCreatorNNInstance, createTemplateRequest);
        if ((createTemplateResponse.getResultCode() == Common.ResultCode.Success) && (createTemplateResponse.getResultsCount() == 1)) {
            BioCommon.Template template = createTemplateResponse.getResults(0).getTemplateResult().getTemplate();
            return createMatcherTemplate(template, 0);
        }
        return null;
    }

    protected BioCommon.MatcherTemplate createMatcherTemplate(BioCommon.Template template, int position) {
        BioCommon.MatcherTemplate.Builder matcherTemplateBuilder = BioCommon.MatcherTemplate
                .newBuilder().setTemplateData(template).setPosition(position);
        return matcherTemplateBuilder.build();
    }

    public static BiometricIdentifier getBiometricIdentifierFromPosition(FingerCommon.NistFingerPosition fingerPosition) {
        switch (fingerPosition) {
            case LeftIndexFinger:
                return BiometricIdentifier.LEFT_INDEX_FINGER;
            case LeftMiddleFinger:
                return BiometricIdentifier.LEFT_MIDDLE_FINGER;
            case LeftRingFinger:
                return BiometricIdentifier.LEFT_RING_FINGER;
            case LeftLittleFinger:
                return BiometricIdentifier.LEFT_PINKY_FINGER;
            case LeftThumb:
                return BiometricIdentifier.LEFT_THUMB;
            case RightIndexFinger:
                return BiometricIdentifier.RIGHT_INDEX_FINGER;
            case RightMiddleFinger:
                return BiometricIdentifier.RIGHT_MIDDLE_FINGER;
            case RightRingFinger:
                return BiometricIdentifier.RIGHT_RING_FINGER;
            case RightLittleFinger:
                return BiometricIdentifier.RIGHT_PINKY_FINGER;
            case RightThumb:
                return BiometricIdentifier.RIGHT_THUMB;
            default:
                return BiometricIdentifier.FACE;
        }
    }

    /**
     * Gets the finger position that OmniMatch uses to index fingers. This is necessary as the ordinals
     * from BiometricIdentifier are different and so, using these will cause a position error when using OmniMatch
     */
    public static int getOmniPosition(BiometricIdentifier identifier) {
        if (identifier == BiometricIdentifier.FACE) {
            return 0;
        }
        return identifier.ordinal() + 1;
    }
}

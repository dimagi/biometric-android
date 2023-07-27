package com.dimagi.biometric.viewmodels;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.JNI.Matchers.MatcherInstance;
import Tech5.OmniMatch.JNI.Matchers.MatcherNative;
import Tech5.OmniMatch.JNI.OmniMatchException;
import Tech5.OmniMatch.JNI.TemplateCreators.TemplateCreatorNNInstance;
import Tech5.OmniMatch.JNI.TemplateCreators.TemplateCreatorNNNative;
import Tech5.OmniMatch.Matcher;
import Tech5.OmniMatch.MatcherCommon;
import Tech5.OmniMatch.TemplateCreatorCommon;

public class OmniMatchViewModel {

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

    public byte[] templateToBytes(BioCommon.MatcherTemplate template) {
        return template.getTemplateData().getData().toByteArray();
    }

    public MatcherCommon.Record createFingerRecord(List<BioCommon.MatcherTemplate> fingerTemplates) {
        MatcherCommon.Record.Builder matcherCommonRecordBuilder = MatcherCommon.Record.newBuilder();
        if (fingerTemplates != null && fingerTemplates.size() > 0) {
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

    public Matcher.RecordsResult verifyRecord(MatcherNative matcherNative, MatcherInstance matcherInstance, MatcherCommon.Record record,
                                              String id) throws OmniMatchException, IOException {
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
            String batchIdentifier = createTemplateResult.getTemplateResult().getBatchIdentifier().getId();
            int fingerPosition = Integer.parseInt(batchIdentifier);
            BioCommon.Template template = createTemplateResult.getTemplateResult().getTemplate();
            return createMatcherTemplate(template, fingerPosition);
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
}

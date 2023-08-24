package com.dimagi.biometric.viewmodels;

import android.app.Application;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dimagi.biometric.OmniMatchUtil;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.Common;
import Tech5.OmniMatch.JNI.OmniMatchException;
import Tech5.OmniMatch.Matcher;
import Tech5.OmniMatch.MatcherCommon;


public abstract class BaseTemplateViewModel extends AndroidViewModel {

    public abstract void init();

    public abstract void insertRecord(MatcherCommon.Record record, String id);
    public abstract float verifyRecord(MatcherCommon.Record record, String id);
    public abstract Matcher.RecordsResult identifyRecord(MatcherCommon.Record record, float threshold, int maxCandidates);

    public abstract BioCommon.MatcherTemplate createTemplate(byte[] image, int position, Common.ImageFormat imageFormat) throws OmniMatchException, InvalidProtocolBufferException;

    public abstract MatcherCommon.Record createRecord(List<BioCommon.MatcherTemplate> templates);
    protected final MutableLiveData<MatcherCommon.Record> activeRecord = new MutableLiveData<>();
    protected OmniMatchUtil omniMatchUtil = null;

    protected final String POS_DELIM = "--";
    protected final String TEMPLATE_DELIM = "---";

    public void setActiveRecord(MatcherCommon.Record record) {
        activeRecord.postValue(record);
    }

    public LiveData<MatcherCommon.Record> getActiveRecord() { return activeRecord; }

    protected final MutableLiveData<Boolean> isCaptureCancelled = new MutableLiveData<>();

    public void setIsCancelled(boolean isCancelled) {
        isCaptureCancelled.postValue(isCancelled);
    }

    public LiveData<Boolean> isCaptureCancelled() {
        return isCaptureCancelled;
    }

    public BaseTemplateViewModel(@NonNull Application application) {
        super(application);
    }

    public abstract BioCommon.MatcherTemplate bytesToTemplate(byte[] templateData, int position);

    public MatcherCommon.Record getRecordFromTemplateStr(String rawTemplateStr) {
        // TODO: Update this function once necessary Support Library changes are done
        if (rawTemplateStr == null) {
            return null;
        }

        String[] templatesStr = rawTemplateStr.split(TEMPLATE_DELIM);
        List<BioCommon.MatcherTemplate> templateList = new ArrayList<>();
        for (String templateStr : templatesStr) {
            String[] templateInfo = templateStr.split(POS_DELIM); // Split as template string and template position
            if (templateInfo.length < 2) {
                continue;
            }
            byte[] templateData = Base64.decode(templateInfo[0], Base64.DEFAULT);
            BioCommon.MatcherTemplate templateObj = bytesToTemplate(templateData, Integer.parseInt(templateInfo[1]));
            templateList.add(templateObj);
        }
        return createRecord(templateList);
    }

    public String getTemplateStrFromRecord(MatcherCommon.Record record, BioCommon.BioType bioType) {
        // TODO: Update this function once necessary Support Library changes are done
        StringBuilder encodedTemplate = new StringBuilder();
        if (bioType == BioCommon.BioType.Face) {
            byte[] templateData = record.getFace().getTemplateData().getData().toByteArray();
            String templateStr = Base64.encodeToString(templateData, Base64.DEFAULT) + POS_DELIM + record.getFace().getPosition();
            encodedTemplate.append(templateStr).append(TEMPLATE_DELIM);
        } else {
            for (BioCommon.MatcherTemplate template : record.getNnFingersList()) {
                int position = template.getPosition();
                byte[] templateData = template.getTemplateData().getData().toByteArray();
                String templateStr = Base64.encodeToString(templateData, Base64.DEFAULT) + POS_DELIM + position;
                encodedTemplate.append(templateStr).append(TEMPLATE_DELIM);
            }
        }
        return encodedTemplate.toString();
    }
}

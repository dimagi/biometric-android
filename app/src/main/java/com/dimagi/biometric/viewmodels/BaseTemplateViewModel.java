package com.dimagi.biometric.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.protobuf.InvalidProtocolBufferException;

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
    protected OmniMatchViewModel omniMatchViewModel = null;

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

    public BioCommon.MatcherTemplate bytesToTemplate(byte[] templateData, int position) {
        return omniMatchViewModel.bytesToTemplate(templateData, position);
    }

    public byte[] templateToBytes(BioCommon.MatcherTemplate template) {
        return omniMatchViewModel.templateToBytes(template);
    }

}

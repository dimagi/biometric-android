package com.dimagi.biometric.activities;

import static com.dimagi.biometric.Constants.BIOMETRIC_TYPE_PARAM;
import static com.dimagi.biometric.Constants.CASE_ID_PARAM;
import static com.dimagi.biometric.Constants.TEMPLATE_PARAM;


import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;

import com.dimagi.biometric.fragments.FaceMatchFragment;
import com.dimagi.biometric.fragments.FingerMatchFragment;
import com.dimagi.biometric.viewmodels.BaseTemplateViewModel;
import com.dimagi.biometric.viewmodels.FaceMatchViewModel;
import com.dimagi.biometric.viewmodels.FingerMatchViewModel;
import com.dimagi.biometric.viewmodels.LicenseViewModel;
import com.dimagi.biometric.R;

import org.commcare.commcaresupportlibrary.identity.model.MatchStrength;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.MatcherCommon;


public abstract class BaseActivity extends AppCompatActivity {

    protected BioCommon.BioType biometricType;
    protected String caseId;

    private LicenseViewModel licenseViewModel;
    protected BaseTemplateViewModel templateViewModel;

    protected abstract void onCaptureSuccess(MatcherCommon.Record activeRecord);
    protected abstract void onCaptureCancelled();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        getIntentParams();
        initTemplateViewModel();
        initLicenseViewModel();
    }

    protected void loadFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.mainContainer, fragment);
        ft.commit();
    }

    private void getIntentParams() {
        Intent intent = getIntent();
        String bioTypeStr = intent.getStringExtra(BIOMETRIC_TYPE_PARAM);
        if (bioTypeStr != null && bioTypeStr.equals("face")) {
            biometricType = BioCommon.BioType.Face;
        } else {
            biometricType = BioCommon.BioType.Finger;
        }
        caseId = intent.getStringExtra(CASE_ID_PARAM);
    }

    private void initTemplateViewModel() {
        if (biometricType == BioCommon.BioType.Face) {
            templateViewModel = new ViewModelProvider(BaseActivity.this).get(FaceMatchViewModel.class);
        } else {
            templateViewModel = new ViewModelProvider(BaseActivity.this).get(FingerMatchViewModel.class);
        }

        templateViewModel.getActiveRecord().observe(BaseActivity.this, activeRecord -> {
            if (activeRecord == null) {
                return;
            }
            onCaptureSuccess(activeRecord);
        });
        templateViewModel.isCaptureCancelled().observe(BaseActivity.this, isCaptureCancelled -> {
            if (isCaptureCancelled) {
                onCaptureCancelled();
            }
        });
    }

    private void initLicenseViewModel() {
        licenseViewModel = new ViewModelProvider(BaseActivity.this).get(LicenseViewModel.class);
        licenseViewModel.isSDKInitialized().observe(BaseActivity.this, isSdkInitialized -> {
            if (!isSdkInitialized) {
                return;
            }
            templateViewModel.init();

            // If SDK is initialized, then start up appropriate fragment
            setContentView(R.layout.activity_main);
            Fragment matchFragment;
            if (biometricType == BioCommon.BioType.Face) {
                matchFragment = new FaceMatchFragment();
            } else {
                matchFragment = new FingerMatchFragment();
            }
            loadFragment(matchFragment);
        });

        licenseViewModel.initSDK(BaseActivity.this);
    }

    protected MatchStrength getMatchStrength(float score) {
        if (score == 1.0f) {
            return MatchStrength.FIVE_STARS;
        } else if (score >= 0.8f) {
            return MatchStrength.FOUR_STARS;
        } else if (score >= 0.6f) {
            return MatchStrength.THREE_STARS;
        } else if (score >= 0.4f) {
            return MatchStrength.TWO_STARS;
        }
        return MatchStrength.ONE_STAR;
    }

    protected MatcherCommon.Record getRecordFromIntent() {
        Intent intent = getIntent();
        byte[] caseTemplateData = intent.getByteArrayExtra(TEMPLATE_PARAM);
        if (caseTemplateData != null && caseTemplateData.length > 0) {
            // TODO: Need to get position from intent
            int position = 0;
            BioCommon.MatcherTemplate template = templateViewModel.bytesToTemplate(caseTemplateData, position);
            List<BioCommon.MatcherTemplate> templateList = new ArrayList<>();
            templateList.add(template);
            return templateViewModel.createRecord(templateList);
        }
        return null;
    }
}


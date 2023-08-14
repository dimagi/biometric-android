package com.dimagi.biometric.activities;

import static com.dimagi.biometric.Constants.BIOMETRIC_TYPE_PARAM;
import static com.dimagi.biometric.Constants.CASE_ID_PARAM;
import static com.dimagi.biometric.Constants.TEMPLATE_PARAM;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dimagi.biometric.Constants;
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

    final private String MATCH_FRAGMENT_TAG = "matchFragment";

    private LicenseViewModel licenseViewModel;
    protected BaseTemplateViewModel templateViewModel;

    protected abstract void onCaptureSuccess(MatcherCommon.Record activeRecord);
    protected abstract void onCaptureCancelled();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Fragment frag = getSupportFragmentManager().findFragmentByTag(MATCH_FRAGMENT_TAG);
            if (frag != null) {
                restoreState(savedInstanceState);
                return;
            }
        }

        setContentView(R.layout.splash);
        Button retryButton = findViewById(R.id.retry_init_button);
        retryButton.setOnClickListener(v -> {
            toggleRetryButton(false);
            licenseViewModel.initSDK(this);
        });

        getIntentParams();
        initTemplateViewModel();
        initLicenseViewModel();
    }

    protected void loadFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.mainContainer, fragment, MATCH_FRAGMENT_TAG);
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
        licenseViewModel.getStatusMessage().observe(BaseActivity.this, message -> {
            if (message == null) {
                return;
            }
            TextView textStatus = findViewById(R.id.txt_status);
            if (textStatus != null) {
                textStatus.setText(message);
            }
        });

        licenseViewModel.getStatus().observe(this, status -> {
            if (status == Constants.initStatus.SUCCESS) {
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
            }
            else if (status == Constants.initStatus.FAIL || status == Constants.initStatus.NO_NETWORK) {
                toggleRetryButton(true);
            }
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

    private void toggleRetryButton(boolean isVisible) {
        Button retryButton = findViewById(R.id.retry_init_button);
        ProgressBar progressBar = findViewById(R.id.progress_circular);
        if (retryButton == null || progressBar == null) {
            return;
        }

        if (isVisible) {
            retryButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            retryButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CASE_ID_PARAM, caseId);
        outState.putSerializable(BIOMETRIC_TYPE_PARAM, biometricType);
    }

    private void restoreState(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        caseId = savedInstanceState.getString(CASE_ID_PARAM);
        biometricType = (BioCommon.BioType)savedInstanceState.getSerializable(BIOMETRIC_TYPE_PARAM);
        initTemplateViewModel();
    }
}


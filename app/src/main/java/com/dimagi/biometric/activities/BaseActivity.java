package com.dimagi.biometric.activities;

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

import com.dimagi.biometric.OmniMatchUtil;
import com.dimagi.biometric.fragments.FaceMatchFragment;
import com.dimagi.biometric.fragments.FingerMatchFragment;
import com.dimagi.biometric.viewmodels.BaseTemplateViewModel;
import com.dimagi.biometric.viewmodels.FaceMatchViewModel;
import com.dimagi.biometric.viewmodels.FingerMatchViewModel;
import com.dimagi.biometric.viewmodels.LicenseViewModel;
import com.dimagi.biometric.R;

import org.commcare.commcaresupportlibrary.identity.BiometricIdentifier;
import org.commcare.commcaresupportlibrary.identity.model.MatchStrength;

import java.util.ArrayList;
import java.util.List;

import Tech5.OmniMatch.BioCommon;
import Tech5.OmniMatch.MatcherCommon;


public abstract class BaseActivity extends AppCompatActivity {

    final protected String CASE_ID_PARAM = "case_id";
    final protected String BIOMETRIC_TYPE_PARAM = "biometric_type";
    final protected String TEMPLATE_PARAM = "template";
    final protected String PROJECT_ID_PARAM = "project_id";
    protected BioCommon.BioType biometricType;
    protected String caseId;
    protected String templateStr;
    protected MatcherCommon.Record templateRecord = null;

    private final String MATCH_FRAGMENT_TAG = "matchFragment";

    private LicenseViewModel licenseViewModel;
    protected BaseTemplateViewModel templateViewModel;
    protected String projectId;

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
        insertFragmentArgs(fragment);
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
        templateStr = intent.getStringExtra(TEMPLATE_PARAM);
        projectId = intent.getStringExtra(PROJECT_ID_PARAM);
    }

    protected MatcherCommon.Record parseBiometricTemplates() {
        Intent intent = getIntent();
        List<BioCommon.MatcherTemplate> templateList = new ArrayList<>();
        if (biometricType == BioCommon.BioType.Face) {
            String templateStr = intent.getStringExtra(BiometricIdentifier.FACE.getCalloutResponseKey());
            BioCommon.MatcherTemplate faceTemplate = templateViewModel.getMatcherTemplateFromStr(templateStr, BiometricIdentifier.FACE);
            templateList.add(faceTemplate);
        } else {
            for (BiometricIdentifier bioId : BiometricIdentifier.values()) {
                if (bioId == BiometricIdentifier.FACE) {
                    continue;
                }
                String templateStr = intent.getStringExtra(bioId.getCalloutResponseKey());
                if (templateStr != null) {
                    byte[] templateData = Base64.decode(templateStr, Base64.DEFAULT);
                    int position = OmniMatchUtil.getOmniPosition(bioId);
                    BioCommon.MatcherTemplate template = templateViewModel.bytesToTemplate(templateData, position);
                    templateList.add(template);
                }
            }
        }

        if (templateList.size() > 0) {
            return templateViewModel.createRecord(templateList);
        }
        return null;
    }

    private void insertFragmentArgs(Fragment fragment) {
        ArrayList<String> errors = validateRequiredParams();
        if (errors.size() > 0) {
            Bundle args = new Bundle();
            String errorStr = createErrorStr(errors);
            args.putString("errors", errorStr);
            fragment.setArguments(args);
        }
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
            if (status == LicenseViewModel.initStatus.SUCCESS) {
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
            } else {
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

    private String createErrorStr(ArrayList<String> errors) {
        StringBuilder errorStr = new StringBuilder();
        for (String error : errors) {
            errorStr.append(error).append("\n");
        }
        return errorStr.toString();
    }

    private ArrayList<String> validateRequiredParams() {
        ArrayList<String> errors = new ArrayList<>();
        if (caseId == null) {
            errors.add(getText(R.string.missing_case_id).toString());
        }
        if (projectId == null) {
            errors.add(getText(R.string.missing_project_id).toString());
        }
        return errors;
    }
}


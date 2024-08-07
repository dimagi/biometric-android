package com.dimagi.biometric.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dimagi.biometric.ParamManager;
import com.dimagi.biometric.R;

import java.util.ArrayList;
import java.util.List;

import static com.dimagi.biometric.activities.BaseActivity.ERROR_MESSAGES_BUNDLE_KEY;

/**
 * A base class to contain common functionality for both the face and finger match fragments.
 */
public abstract class BaseMatchFragment extends Fragment {
    private static final String TAG = "BIOMETRIC";

    private final String PREV_RATIONALE_KEY = "_prev_rationale";
    private final String DIALOG_STATE_KEY = "dialog_state";

    private DialogState dialogState = DialogState.NONE;

    private enum DialogState {
        NONE,
        RATIONALE,
        SETTINGS
    }

    private final String COMMCARE_READ_PERMISSION = "org.commcare.dalvik.provider.cases.read";
    private final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            COMMCARE_READ_PERMISSION
    };

    protected abstract void handleStartCapture();
    protected abstract void handleCancelCapture();
    protected abstract ParamManager getParams();

    public BaseMatchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        Button cancelButton = view.findViewById(R.id.cancel_capture_button);
        cancelButton.setOnClickListener(v -> handleCancelCapture());
        if (savedInstanceState != null) {
            restoreDialog(savedInstanceState);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        handleStartButtonValidation(args, view);
    }

    private void handleStartButtonValidation(Bundle args, @NonNull View view) {
        String errors = null;
        if (args != null) {
            errors = args.getString(ERROR_MESSAGES_BUNDLE_KEY);
        }
        Button startButton = view.findViewById(R.id.start_capture_button);
        if (errors == null) {
            startButton.setOnClickListener(v -> handleStartClick());
        } else {
            startButton.setEnabled(false);
            handleErrorMessage(errors);
        }
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
        if (!isGranted.containsValue(false)) {
            handleStartCapture();
        } else {
            for (String permission : PERMISSIONS) {
                if (isPermissionDeniedPermanently(permission)) {
                    requestOpenAppSettings();
                    break;
                }
            }
        }
    });

    private void showPermissionAlertDialog() {
        dialogState = DialogState.RATIONALE;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setTitle(getText(R.string.permission_required));
        alertDialogBuilder.setMessage(getText(R.string.permission_rationale));
        alertDialogBuilder.setPositiveButton(getText(R.string.confirm), (dialog, which) -> requestPermissionLauncher.launch(PERMISSIONS));
        alertDialogBuilder.setNegativeButton(getText(R.string.cancel), null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setOnDismissListener((dialog) -> dialogState = DialogState.NONE);
        alertDialog.show();
    }

    private void requestOpenAppSettings() {
        dialogState = DialogState.SETTINGS;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setTitle(getText(R.string.go_to_settings));
        alertDialogBuilder.setMessage(getText(R.string.permission_settings));
        alertDialogBuilder.setPositiveButton(getText(R.string.confirm), (dialogInterface, i) -> navigateToSettings());
        alertDialogBuilder.setNegativeButton(getText(R.string.cancel), null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setOnDismissListener((dialog) -> dialogState = DialogState.NONE);
        alertDialog.show();
    }

    private void navigateToSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        requireContext().startActivity(intent);
    }

    protected void handleErrorMessage(String error) {
        try {
            TextView errorText = requireView().findViewById(R.id.error_text);
            errorText.setText(error);
        } catch (NullPointerException | IllegalStateException e) {
            Log.e(TAG, "Exception trying to create main menu error message: " + e);
        }
    }

    /**
     * After the second denial shouldShowRequestPermissionRationale will be false, so we need to
     * save that this was true to only show the dialog asking the user to navigate
     * to the app settings after they have denied the permission twice.
     */
    private void savePreviousRationale(String permission) {
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String key = permission + PREV_RATIONALE_KEY;
        editor.putBoolean(key, true);
        editor.apply();
    }

    private boolean isPermissionDeniedPermanently(String permission) {
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        String key = permission + PREV_RATIONALE_KEY;
        boolean prevRationale = sharedPref.getBoolean(key, false);
        boolean hasRationale = shouldShowRequestPermissionRationale(permission);
        return !hasRationale && prevRationale;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(DIALOG_STATE_KEY, dialogState);
    }

    private void restoreDialog(Bundle savedInstanceState) {
        dialogState = (DialogState)savedInstanceState.getSerializable(DIALOG_STATE_KEY);
        if (dialogState == null) {
            return;
        }
        if (dialogState == DialogState.RATIONALE) {
            showPermissionAlertDialog();
        } else if (dialogState == DialogState.SETTINGS) {
            requestOpenAppSettings();
        }
    }

    private List<String> checkPermissions() {
        List<String> deniedPermissions = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if(ContextCompat.checkSelfPermission(
                    requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
                deniedPermissions.add(permission);
            }
        }
        return deniedPermissions;
    }

    private void handleStartClick() {
        List<String> deniedPermissions = checkPermissions();
        if (deniedPermissions.isEmpty()) {
            handleStartCapture();
        } else {
            boolean allPermissionsDenied = true;
            for (String permission : deniedPermissions) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    savePreviousRationale(permission);
                }
                if (!isPermissionDeniedPermanently(permission)) {
                    allPermissionsDenied = false;
                }
            }

            if (allPermissionsDenied) {
                requestOpenAppSettings();
            } else {
                showPermissionAlertDialog();
            }
        }
    }

    protected Integer safeParseInteger(String num, int defaultVal) {
        int outVal = defaultVal;
        try {
            outVal = Integer.parseInt(num);
        } catch (NumberFormatException | NullPointerException ignored) {}
        return outVal;
    }

    protected Float safeParseFloat(String num, float defaultVal) {
        float outVal = defaultVal;
        try {
            outVal = Float.parseFloat(num);
        } catch (NumberFormatException | NullPointerException ignored) {}
        return outVal;
    }

    protected Boolean safeParseBool(String val, boolean defaultVal) {
        boolean outVal = defaultVal;
        try {
            if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
                outVal = Boolean.parseBoolean(val);
            }
        } catch (NullPointerException ignored) {}
        return outVal;
    }
}

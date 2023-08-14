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

import com.dimagi.biometric.R;

/**
 * A base class to contain common functionality for both the face and finger match fragments.
 */
public abstract class BaseMatchFragment extends Fragment {
    private static final String TAG = "BIOMETRIC";

    private final String PREV_RATIONALE_KEY = "prev_rationale";

    protected abstract void handleStartCapture();
    protected abstract void handleCancelCapture();

    public BaseMatchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        Button startButton = view.findViewById(R.id.start_capture_button);
        startButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                handleStartCapture();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    savePreviousRationale();
                }
                if (!isPermissionDeniedPermanently()) {
                    showPermissionAlertDialog();
                }
            }
        });
        Button cancelButton = view.findViewById(R.id.cancel_capture_button);
        cancelButton.setOnClickListener(v -> handleCancelCapture());
        return view;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            handleStartCapture();
        } else if (isPermissionDeniedPermanently()) {
            requestOpenAppSettings();
        }
    });

    private void showPermissionAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setTitle(getText(R.string.camera_permission_required));
        alertDialogBuilder.setMessage(getText(R.string.camera_permission_rationale));
        alertDialogBuilder.setPositiveButton(getText(R.string.confirm), (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
        alertDialogBuilder.setNegativeButton(getText(R.string.cancel), null);
        alertDialogBuilder.create().show();
    }

    private void requestOpenAppSettings() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setTitle(getText(R.string.go_to_settings));
        alertDialogBuilder.setMessage(getText(R.string.camera_permission_settings));
        alertDialogBuilder.setPositiveButton(getText(R.string.confirm), (dialogInterface, i) -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            requireContext().startActivity(intent);
        });
        alertDialogBuilder.setNegativeButton(getText(R.string.cancel), null);
        alertDialogBuilder.create().show();
    }

    protected void handleErrorMessage(String error) {
        try {
            TextView errorText = requireView().findViewById(R.id.error_text);
            errorText.setText(error);
        } catch (NullPointerException ex) {
            Log.e(TAG, "Null pointer on trying to set error message");
        }
    }

    /**
     * After the second denial shouldShowRequestPermissionRationale will be false, so we need to
     * save that this was true to only show the dialog asking the user to navigate
     * to the app settings after they have denied the permission twice.
     */
    private void savePreviousRationale() {
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREV_RATIONALE_KEY, true);
        editor.apply();
    }

    private boolean isPermissionDeniedPermanently() {
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean prevRationale = sharedPref.getBoolean(PREV_RATIONALE_KEY, false);
        boolean hasRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
        return !hasRationale && prevRationale;
    }
}
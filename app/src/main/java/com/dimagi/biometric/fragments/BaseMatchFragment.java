package com.dimagi.biometric.fragments;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dimagi.biometric.R;

/**
 * A base class to contain common functionality for both the face and finger match fragments.
 */
public abstract class BaseMatchFragment extends Fragment {
    private static final String TAG = "BIOMETRIC";

    private Toast permissionToast;

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
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showPermissionAlertDialog();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
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
        } else {
            showPermissionToast();
        }
    });

    private void showPermissionAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setMessage(getText(R.string.camera_permission_rationale));
        alertDialogBuilder.setPositiveButton(getText(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        alertDialogBuilder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showPermissionToast() {
        CharSequence text = getText(R.string.camera_permission_required);
        if (permissionToast != null) {
            permissionToast.cancel();
        }
        permissionToast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT);
        permissionToast.show();
    }

    protected void handleErrorMessage(String error) {
        try {
            TextView errorText = getView().findViewById(R.id.error_text);
            errorText.setText(error);
        } catch (NullPointerException ex) {
            Log.e(TAG, "Null pointer on trying to set error message");
        }

    }
}
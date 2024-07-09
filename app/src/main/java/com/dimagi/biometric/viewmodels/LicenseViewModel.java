package com.dimagi.biometric.viewmodels;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dimagi.biometric.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import Tech5.OmniMatch.JNI.Android.AndroidNative;
import Tech5.OmniMatch.JNI.CoreNative;
import Tech5.OmniMatch.JNI.OmniMatchException;

public class LicenseViewModel extends AndroidViewModel {

    private static final String TAG = "BIOMETRIC";
    private static final String PROJECT_ID_FOR_TESTING = "TEST";

    private final Application application;

    private final MutableLiveData<initStatus> status = new MutableLiveData<>();

    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    public enum initStatus {
        FAIL,
        SUCCESS,
        NO_NETWORK,
        NO_VALID_LICENSE,
    }

    public LicenseViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<initStatus> getStatus() { return status; }

    public void initSDK(Context context, String projectId) {

        new Thread(() -> {
            statusMessage.postValue(application.getResources().getString(R.string.omnimatch_initializing));
            boolean isSDKInitialized = loadLicense(context, projectId);
            if (isSDKInitialized) {
                status.postValue(initStatus.SUCCESS);
            } else {
                status.postValue(initStatus.NO_VALID_LICENSE);
                statusMessage.postValue(application.getResources().getString(R.string.license_validation_failed));
            }
        }).start();
    }

    private boolean loadLicense(Context context, String projectId) {
        int resultCode;
        try {
            CoreNative coreNative = new CoreNative();

            Log.d(TAG, "version : " + coreNative.GetVersion().getValue());
            AndroidNative<Context> androidNative = new AndroidNative<>();
            resultCode = androidNative.SetLicense(context, "");
            if (resultCode < 0) {
                if (!isNetworkAvailable()) {
                    statusMessage.postValue(application.getResources().getString(R.string.connection_failed));
                    status.postValue(initStatus.NO_NETWORK);
                    return false;
                }

                String url = "https://pheonix-lic.tech5.tech/license/" + application.getApplicationContext().getPackageName() + getUrlDomainSuffix(projectId) + "/" + Math.abs(resultCode);
                String token = sendHttpRequest(url);
                if (token == null) {
                    throw new IOException();
                }
                androidNative.SetLicense(context, token);
                resultCode = androidNative.SetLicense(context, token);
            }
        } catch (OmniMatchException | IOException e) {
            Log.e(TAG, "Exception loading license");
            statusMessage.postValue(application.getResources().getString(R.string.omnimatch_failed));
            status.postValue(initStatus.FAIL);
            resultCode = -1;
        }

        return (resultCode == 0);
    }

    private String getUrlDomainSuffix(String projectId) {
        if (projectId == null || PROJECT_ID_FOR_TESTING.equalsIgnoreCase(projectId)) {
            return "";
        } else {
            return "." + projectId.toLowerCase();
        }
    }

    private String sendHttpRequest(String urlString) throws IOException {

        HttpURLConnection urlConnection;
        StringBuilder result = new StringBuilder();

        URL url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();

        int code = urlConnection.getResponseCode();
        if (code == 200) {
            try (InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) result.append(line);
                return result.toString();
            } catch (IOException e) {
                Log.e(TAG, "error doing HTTP request for license:  " + e.getLocalizedMessage());
            }
        }

        return null;
    }

    public Boolean isNetworkAvailable() {
        ConnectivityManager conManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conManager.getActiveNetworkInfo();
        try {
            // Device may be connected to a network but without Internet, so we need to ping a site to verify an
            // actual Internet connection
            InetAddress ipAddr = InetAddress.getByName("commcarehq.org");
            return netInfo != null && netInfo.isConnectedOrConnecting() && !ipAddr.toString().equals("");
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
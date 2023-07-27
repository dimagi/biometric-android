package com.dimagi.biometric.viewmodels;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import Tech5.OmniMatch.JNI.Android.AndroidNative;
import Tech5.OmniMatch.JNI.CoreNative;
import Tech5.OmniMatch.JNI.OmniMatchException;

public class LicenseViewModel extends AndroidViewModel {


    private static final String TAG = "BIOMETRIC";


    private final Application application;

    private final MutableLiveData<Boolean> isSDKInitialized = new MutableLiveData<>();

    public LicenseViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }


    public LiveData<Boolean> isSDKInitialized() {
        return isSDKInitialized;
    }

    public void initSDK(Context context) {

        new Thread(() -> {
            boolean isSDKInitialized = loadLicense(context);
            this.isSDKInitialized.postValue(isSDKInitialized);
        }).start();
    }


    private boolean loadLicense(Context context) {
        int resultCode;
        try {
            CoreNative coreNative = new CoreNative();

            Log.d(TAG, "version : " + coreNative.GetVersion().getValue());

            AndroidNative<Context> androidNative = new AndroidNative<>();

            resultCode = androidNative.SetLicense(context, "");
            Log.d(TAG, "set License result code " + resultCode);
            if (resultCode < 0) {
                String url = "https://pheonix-lic.tech5.tech/license/" + application.getApplicationContext().getPackageName() + "/" + Math.abs(resultCode);

                Log.d(TAG, "sending  license request to " + url);

                String token = sendHttpRequest(url);

                androidNative.SetLicense(context, token);
                resultCode = androidNative.SetLicense(context, token);

                Log.d(TAG, "token from server" + token);

                Log.d(TAG, "set License result code2 " + resultCode);

            }
        } catch (OmniMatchException | IOException ome) {
            Log.e(TAG, "Exception loading license");
            resultCode = -1;
        }

        return (resultCode == 0);
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

        return result.toString();

    }


//        public Boolean isNetworkAvailable() {
//            ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
//
//            Network nw = connectivityManager.getActiveNetwork();
//            if (nw == null) return false;
//            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
//            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
//
//        }
}
# Android Biometric Application

Biometric Android application for integrating Tech5's biometric technology with CommCare. This application supports three different biometric workflows:

- Enrollment (com.dimagi.biometric.ENROLL)
- Verification (com.dimagi.biometric.VERIFY)
- Identification (com.dimagi.biometric.SEARCH)

Please note the following:

- The biometric application will require an Internet connection the first time it is opened. This is to retrieve the Tech5 license. Thereafter, the biometric application is capable of running in an offline state.
- The biometric application is reliant on CommCare making the necessary app callout and passing in the required input parameters. As such, the biometric app is currently not functional as an independent app and should be started from CommCare.

## Setup

### Prerequisites

To set up an Android dev environment for the biometric app, do the following:

- Install [Android Studio](https://developer.android.com/sdk/index.html).
- Install Java 17 if you don't have it yet. For ease of test suite setup OpenJDK is preferred over Oracle's version of Java.

### [](https://github.com/dimagi/biometric-android#dependencies)Dependencies

For the biometric application to work correctly, the necessary Tech5 dependencies will need to be set up first. This will require the following:

- Download the Tech5 OmniMatch SDK (link available on 1Password under "Tech5 OmniMatch SDK".
- From the sample app of the downloaded SDK, copy the following folders to the biometric app project's root directory:
    - AirsnapFaceCore
    - AirsnapFaceUI
    - AirsnapFinger
    - AirsnapFingerUI
    - OmniMatch

The app `build.gradle` and `settings.gradle` files are already set up to add the Tech5 dependencies to the project, so no further action is required after adding the necessary dependency folders.

## Building

To build the biometric app and get it running on your phone, plug in an android phone that

- is [in developer mode has USB debugging enabled](https://developer.android.com/tools/device.html#setting-up).
- doesn't have the biometric app installed on it.

In Android Studio, hit the build button (a green "play" symbol in the toolbar). The first build will take a minute. Then it'll ask you what device to run it on.

- Make sure your screen is unlocked (or else you'll see [something like this](https://gist.github.com/dannyroberts/6d8d57ff4d5f9a1b70a5)).
- select your device.

# Android Biometric Application

Biometric Android application for integrating Tech5's biometric technology with CommCare. This application supports three different biometric workflows:

- Enrollment (com.dimagi.biometric.ENROLL)
- Verification (com.dimagi.biometric.VERIFY)
- Identification (com.dimagi.biometric.SEARCH)

Please note the following:

- The biometric application will require an Internet connection the first time it is opened. This is to retrieve the Tech5 license. Thereafter, the biometric application is capable of running in an offline state.
- The biometric application is reliant on CommCare making the necessary app callout and passing in the required input parameters. As such, the biometric app is currently not functional as an independent app and should be started from CommCare.

## Code Structure

There are three main activities for carrying out the different workflows. Specifically, these are the `EnrollActivity`, `VerifyActivity`, and `SearchActivity` classes. These three activities all inherit from the `BaseActivity` class, which holds shared functionality for setting up the activities (such as initializing the Tech5 SDK license and creating the relevant fragment). The sub-class activities themselves only contain code necessary for handling the captured biometric data and returning the relevant data back to CommCare, based on the selected workflow.

For the UI component, two fragments, `FaceMatchFragment` and `FingerMatchFragment`, have been created for both face and finger biometric capture, respectively. Both of these fragments inherit from the `BaseMatchFragment` class, which holds common functionality for camera permission checking. These fragments are responsibile for starting the relevant Tech5 capture UI and handling the results of this capture. The latter involves converting the captured image to a record and letting the activity know that the capture was successful.

To faciliate communication between the activities and fragments, view models have been set up for both the face and finger fragments. These view models contain the necessary functions to convert captured images into records, as well as carry out various functions such as inserting or matching records. Both view models inherit from `BaseTemplateViewModel`. This is done so that they can be referenced and used in `BaseActivity` without explicitely knowing whether `FaceMatchViewModel` or `FingerMatchViewModel` was instantiated.

## Setup

### Prerequisites

To set up an Android dev environment for the biometric app, do the following:

- Install [Android Studio](https://developer.android.com/sdk/index.html).
- Install Java 18 if you don't have it yet. For ease of test suite setup OpenJDK is preferred over Oracle's version of Java.

### [](https://github.com/dimagi/biometric-android#dependencies)Dependencies

For the biometric application to work correctly, the necessary Tech5 dependencies will need to be set up first. These dependencies are responsible for the capture UI, creating biometric templates, and matching templates with each other. To set up the Tech5 dependencies:

- Download the Tech5 OmniMatch SDK (link available on 1Password under "Tech5 OmniMatch SDK".
- Create a folder `app/libs` to store Tech5 `.aar` files
- In your `local.properties` file define a property `TECH5_SDK_DIR=libs`   
- From the sample app of the downloaded SDK, copy the .aar files from the following folders to your `app/libs` folder - 
    - AirsnapFaceCore
    - AirsnapFaceUI
    - AirsnapFinger
    - AirsnapFingerUI
    - OmniMatch

## Building

To build the biometric app and get it running on your phone, plug in an android phone that

- is [in developer mode has USB debugging enabled](https://developer.android.com/tools/device.html#setting-up).
- doesn't have the biometric app installed on it.

In Android Studio, hit the build button (a green "play" symbol in the toolbar). The first build will take a minute. Then it'll ask you what device to run it on.

- Make sure your screen is unlocked (or else you'll see [something like this](https://gist.github.com/dannyroberts/6d8d57ff4d5f9a1b70a5)).
- select your device.

## Code Style Settings
In order to comply with code style guidelines we follow, please use [Commcare Coding Style file](https://github.com/dimagi/commcare-android/blob/master/.android_studio_settings/codestyles/CommCare%20Coding%20Style.xml) and [Commcare Inspection Profile](https://github.com/dimagi/commcare-android/blob/master/.android_studio_settings/inspection/CommCare%20Inpsection%20Profile.xml) as your respective code style and inpection profile in Android Studio. To add these files into Android studio, please do the following:
1. Navigate to File -> Settings -> Editor -> Inspections. You will then click on the gear icon next to "Profile" and import `CommCareInspectionProfile.xml`
2. Navigate to File -> Settings -> Editor -> Code Style. You will then click on the gear icon next to "Scheme" and import `CommCareCodingStyle.xml`

## Firebase Integration
This is an optional step to set up Firebase Crashlytics for the dev environment. A `google-services.json` file will need to be added to the project root's `app` folder. This file can be retrieved from Dimagi's Firebase console.

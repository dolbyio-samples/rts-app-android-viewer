# Dolby.io Realtime Streaming Monitor - Android TV App

## Readme

This project demonstrates what a simple Realtime Streaming monitor experience is like when built for an Android TV device.

| Use cases              | Features                                                         | Tech Stack      |
| ---------------------- | ---------------------------------------------------------------- |-----------------|
| Monitor a video stream | Start a stream monitoring with a stream name and account ID pair | Kotlin, Android |

## Pre-requisites

This setup guide is validated on both Intel/M1-based MacBook Pro running macOS 13.2.1.

### Android

* Android Studio Electric Eel | 2022.1.1 Patch 1
* Android/Google TV (4K) API 31 simulator
* Android/Google TV (4K) API 33 simulator
* Chomecast with Google TV 4K 2020

### Other

* A [Dolby.io](https://dashboard.dolby.io/signup/) account
* Start a video streaming broadcasting, see [here](https://docs.dolby.io/streaming-apis/docs/how-to-broadcast-in-dashboard)
* The Stream name and Account ID pair from the video streaming above

### How to get a Dolby.io account

To setup your Dolby.io account, go to the [Dolby.io dashboard](https://dashboard.dolby.io/signup/) and complete the form. After confirming your email address, you will be logged in.

## Cloning the repo

The Dolby.io Realtime Streaming Monitor sample app is hosted in an Android Studio project. There are two Swift Package libraries under it.

Get the code by cloning this repo using git.

```bash
git clone git@github.com:dolbyio-samples/rts-app-android-viewer.git
```

## Setup

For public usage, nothing needs to be done, the sdk will be retrieved through public maven repository.
Internal development with untested & candidate releases needs to make 3 modifications inside
`~/.gradle/gradle.properties` :

```bash
DOLBY_INTERNAL_MAVEN_URL=url_of_maven_repository
DOLBY_INTERNAL_MAVEN_USER=both_username
DOLBY_INTERNAL_MAVEN_PASSWORD=and_password
```

## Building and running the app

Start the Android Studio, and open the folder of the project root.

From the top of the Android Studio, click Build -> Select Build Variant, select the correct Active Build Variant for the app. The `tvDebug` is selected for the Android TV app target.

From the top of the Android Studio, select the actual target to be run on.

Click on the `Start` ► button on top of the Android Studio to start running and debugging the app.

> **_Info:_** To run on a real device, you need to connect it to the Android Studio with the [ADB](https://developer.android.com/studio/command-line/adb#:~:text=Connect%20to%20a%20device%20over%20Wi-Fi%20%28Android%2011%2B%29,and%20port%20number%20from%20step%205.%20See%20More.)
>  
> ### Connecting a real Chromecast device
> 
* Power up the Chromecast device and connect it to a display unit.
* Navigate to "Settings > System > About > Android TV OS build" and click on this options 7 times to turn on "Developers option"
* You should see a "You are now a developer!"
* Connect your local host and chromecast device in same wifi network
* Navigate to "System > About > Status"
* Make a note of the Chromecast device IP address as "chromecast-ip-address"
* Open a terminal on your local host machine
* Enter the command "adb connect chromecast-ip-address:5555"
* Click "Yes" button in a pop up displayed to enable debugging of the chromecast using your computer
* Verify the chromecast device is connected using command "adb devices"

## Known Issues

The known issues of this sample app can be found [here](KNOWN-ISSUES.md).

## License

The Dolby.io Realtime Streaming Monitor sample and its repository are licensed under the [MIT License](https://github.com/dolbyio-samples/rts-app-android-viewer/blob/main/LICENSE).

## More resources

Looking for more sample apps and projects? Head to the [Project Gallery](https://docs.dolby.io/communications-apis/page/gallery).

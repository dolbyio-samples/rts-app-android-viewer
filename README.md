# Dolby.io Realtime Streaming Monitor - Android TV App

## Readme

This project demonstrates what a simple Realtime Streaming monitor experience is like when built for an Android TV device.

| Use cases              | Features                                                         | Tech Stack       |
| ---------------------- | ---------------------------------------------------------------- | ---------------- |
| Monitor a video stream | Start a stream monitoring with a stream name and account ID pair | Kotlin, Android |

## Pre-requisites

This setup guide is validated on both Intel/M1-based MacBook Pro running macOS 13.2.1.

### Android

* Android Studio Electric Eel | 2022.1.1 Patch 1
* Android TV (1080p) API 13 simulator
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

Add the following properties (with values) into your local.properties file under project root:

The username of a working GitHub account user.

```bash
githubUsername=[GitHub_username]
```

The GitHub's user's Personal Access Token (PAT) with the read:packages scope.

```bash
githubPat=[GitHub_Personal_Access_Token]
```

For more information on how to create and use a PAT
> **_Info:_** [Creating a personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) and [Working with the Gardle registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)

## Building and running the app

Start the Android Studio, and open the folder of the project root.

From the top of the Android Studio, click Build -> Select Build Variant, select the correct Active Build Variant for the app. The `tvDebug` is selected for the Android TV app target.

From the top of the Android Studio, select the actual target to be run on.

> **_Info:_** To run on a real device, you need to connect it to the Android Studio with the [ADB](https://developer.android.com/studio/command-line/adb#:~:text=Connect%20to%20a%20device%20over%20Wi-Fi%20%28Android%2011%2B%29,and%20port%20number%20from%20step%205.%20See%20More.)

Click on the `Start` ► button on top of the Android Studio to start running and debugging the app.

## Known Issues

The known issues of this sample app can be found [here](KNOWN-ISSUES.md).

## License

The Dolby.io Realtime Streaming Monitor sample and its repository are licensed under the MIT License.

## More resources

Looking for more sample apps and projects? Head to the [Project Gallery](https://docs.dolby.io/communications-apis/page/gallery).

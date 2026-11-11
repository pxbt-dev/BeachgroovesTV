# BeachGrooves Radio for Android TV

[![BeachGrooves: World off, music on!](docs/header.png)](https://beachgrooves.com/)

A small Android app that plays the live stream of [BeachGrooves Radio](https://beachgrooves.com/) ("World off, music on!") on an Android TV box. It also runs on phones.

Open the app and the radio starts. There's one big play/pause button, the current show name (taken from the stream), and a status line (Connecting…, ● LIVE, Paused, Connection lost, reconnecting…).

## Using it

| Remote key | What it does |
|---|---|
| OK / play-pause | Pause or resume the radio |
| Home | Leave the app; the radio keeps playing in the background |
| Back, twice within 2 s | Stop the radio and close the app |

The remote's play/pause key works from anywhere, even when the app isn't on screen, because playback runs in a media session.

## How it works

The app has two classes in `app/src/main/java/com/beachgrooves/tv/`:

- **`PlaybackService.kt`**: a Media3 `MediaSessionService` that plays the Shoutcast stream with ExoPlayer.
  - Pausing a live stream disconnects it. When you press play again it reconnects at the live point, so you don't hear old buffered audio.
  - After a network error it tries to reconnect every 5 seconds.
  - The show name comes from the stream's ICY metadata.
  - A network wake lock keeps Wi-Fi and the CPU awake while it plays.
- **`MainActivity.kt`**: the full-screen "now playing" page. It connects to the service through a `MediaController` and handles the remote keys.

The stream URL is set in `PlaybackService.STREAM_URL`:

```
https://stream.beachgrooves.com:9000/stream
```

## Building

You need JDK 17 or newer and the Android SDK (API 37). Android Studio comes with both. Gradle 9.8 is pulled in by the wrapper.

```sh
./gradlew assembleRelease      # Windows: gradlew.bat assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

The release build is signed with the debug key so you can sideload it without setting up a keystore. Before publishing to the Play Store, set up proper release signing in `app/build.gradle.kts`.

## Installing on a TV box

Turn on developer options and network (or USB) debugging on the box, then run:

```sh
adb connect <box-ip>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

The app shows up in the TV launcher with its banner. On a phone it shows up in the normal app drawer.

## Details

| | |
|---|---|
| Package | `com.beachgrooves.tv` |
| Min / target SDK | 26 (Android 8.0) / 37 |
| Dependencies | AndroidX Media3 1.8.0 (`media3-exoplayer`, `media3-session`) |
| Permissions | `INTERNET`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` |

## Troubleshooting

If the app crashes on a box where logcat isn't available, read the crash report with:

```sh
adb shell dumpsys dropbox --print data_app_crash
```

# 30nama for Android TV / Google TV

An Android TV app: an unofficial client for 30nama.com.

![30nama TV](docs/images/poster.png)

> **Disclaimer:** This is an unofficial, community project. It is not affiliated with, endorsed by, or supported by 30nama. You need your own valid 30nama subscription to use it. No content is bundled with this app.

This is an Android TV port of [**msharifi99/30nama-tizenos**](https://github.com/msharifi99/30nama-tizenos), the Samsung Tizen TV app by Rebin Sharifi. All of the UI comes from that project. Version 2 is a full native rewrite (Kotlin + Jetpack Compose for TV) of that app's UI and API client, as a lighter alternative to the official Android TV app.

## Features

- Sign in by scanning a QR code with your phone
- TV UI: hero banner, rows, details pages, search, remote-friendly navigation
- Fully native UI (Jetpack Compose for TV), no WebView: about half the memory of the WebView build
- Native **ExoPlayer** playback (hardware decoding) at the highest available quality
- Persian (fa) and English (en) subtitles, audio track switching, skip intro/recap, up next
- Watch progress syncs with your 30nama account (Continue watching)
- Persian UI with full right-to-left support
- Small APK (~2.3 MB), works on Android TV / Google TV 6.0+

## Screenshots

| Home | Browse |
|---|---|
| ![Home](docs/images/home-hero.png) | ![Browse](docs/images/home-browse.png) |
| **Details** | **Search** |
| ![Details](docs/images/details.png) | ![Search](docs/images/search.png) |

## Install

1. Download `30nama-androidtv-v*.apk` from the [latest release](../../releases/latest).
2. Sideload it on your TV, either way works:
   - **adb:** on the TV open Settings → System → About and press OK on "Android TV OS build" 7 times, then turn on USB debugging in Settings → System → Developer options. From your computer:
     ```
     adb connect <tv-ip>:5555
     adb install 30nama-androidtv-v1.0.1.apk
     ```
   - **Without a computer:** use an app like *Downloader* or *Send Files to TV* to get the APK onto the TV and open it.
3. Launch **30nama** from the TV home screen (look under Apps if it isn't in the top row).

## How it works

The app is native Kotlin, ported screen by screen from the Tizen web app in [`tizen-app/`](tizen-app):

| Tizen web app | Android TV app |
|---|---|
| `js/api.js` (discovery, rate-limited RPC queue) | [`data/Api.kt`](android-tv/app/src/main/java/com/nama30/tv/data/Api.kt) on OkHttp + coroutines |
| `js/screens.js` (login, home, details, search, archive, settings) | Compose for TV screens in [`ui/`](android-tv/app/src/main/java/com/nama30/tv/ui) |
| `js/player.js` (AVPlay, WebVTT overlay, skip intro, up next) | [`player/PlayerScreen.kt`](android-tv/app/src/main/java/com/nama30/tv/player/PlayerScreen.kt) on ExoPlayer |
| `js/i18n.js` | [`ui/Strings.kt`](android-tv/app/src/main/java/com/nama30/tv/ui/Strings.kt) (generated from i18n.js) |

Memory on the Google TV emulator (1080p, signed in, Home screen):

| Build | PSS |
|---|---|
| v1.0.1 (WebView) | ~140 MB (app 75 MB + WebView renderer 66 MB) |
| v2.0.0 (native) | ~67 MB |

v1.0.1, which wrapped the web app in a WebView, is still available under [releases](../../releases/tag/v1.0.1).

## Build from source

Requires JDK 17 and the Android SDK (platform 34).

```
cd android-tv
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # path to your Android SDK
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

## Samsung Tizen TV

For the original Tizen version (TizenBrew, `.wgt`, building with Tizen Studio), see [msharifi99/30nama-tizenos](https://github.com/msharifi99/30nama-tizenos). The Tizen sources are still in this repo (`tizen-app/`, `build.sh`, `install-wizard.sh`).

## Credits

- [Rebin Sharifi (msharifi99)](https://github.com/msharifi99), who wrote [30nama-tizenos](https://github.com/msharifi99/30nama-tizenos): the whole TV UI, API client and player UI
- Fonts: [Vazirmatn](https://github.com/rastikerdar/vazirmatn) (OFL)

## License

MIT, see [LICENSE](LICENSE).

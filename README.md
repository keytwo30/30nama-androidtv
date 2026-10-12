# 30nama for Android TV / Google TV

An Android TV app: an unofficial client for 30nama.com.

![30nama TV](docs/images/poster.png)

> **Disclaimer:** This is an unofficial, community project. It is not affiliated with, endorsed by, or supported by 30nama. You need your own valid 30nama subscription to use it. No content is bundled with this app.

This is an Android TV port of [**msharifi99/30nama-tizenos**](https://github.com/msharifi99/30nama-tizenos), the Samsung Tizen TV app by Rebin Sharifi. All of the UI comes from that project. This repo wraps it in a small native Android shell, as an alternative to the official Android TV app.

## Features

- Sign in by scanning a QR code with your phone
- TV UI: hero banner, rows, details pages, search, remote-friendly navigation
- Native **ExoPlayer** playback (hardware decoding) at the highest available quality, with the TV UI drawn on top
- Persian (fa) and English (en) subtitles, audio track switching, skip intro/recap, up next
- Watch progress syncs with your 30nama account (Continue watching)
- Persian UI with full right-to-left support
- Small APK (~1.2 MB), works on Android TV / Google TV 6.0+

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

The web app from [`tizen-app/`](tizen-app) runs unchanged inside an Android WebView. A small shim ([`android-tv/app/src/main/web/android/shim.js`](android-tv/app/src/main/web/android/shim.js)) replaces the Tizen-only pieces:

| Tizen | Android TV |
|---|---|
| `webapis.avplay` (Samsung player) | ExoPlayer on a `SurfaceView` behind a transparent WebView |
| Network requests from the TV app | `fetch()` bridged to native OkHttp, so there's no CORS or proxy |
| `tizen.tvinputdevice` remote keys | Back and media keys mapped to Tizen key codes |
| `tizen.application.exit()` | Closes the activity |

## Build from source

Requires JDK 17 and the Android SDK (platform 34).

```
cd android-tv
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # path to your Android SDK
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

At build time the web assets are copied from `tizen-app/`, so pulling upstream changes and rebuilding gives you an updated APK.

## Optional: TMDB

The upstream app can add trending rows, cast, reviews and recommendations from TMDB. Set it up the same way (see the [upstream instructions](https://github.com/msharifi99/30nama-tizenos#optional-tmdb-setup-build-from-source-only)): put your token in `tizen-app/js/tmdb-token.js` and rebuild. Release APKs ship without a token, so TMDB is off there.

## Samsung Tizen TV

For the original Tizen version (TizenBrew, `.wgt`, building with Tizen Studio), see [msharifi99/30nama-tizenos](https://github.com/msharifi99/30nama-tizenos). The Tizen sources are still in this repo (`tizen-app/`, `build.sh`, `install-wizard.sh`).

## Credits

- [Rebin Sharifi (msharifi99)](https://github.com/msharifi99), who wrote [30nama-tizenos](https://github.com/msharifi99/30nama-tizenos): the whole TV UI, API client and player UI
- Fonts: [Vazirmatn](https://github.com/rastikerdar/vazirmatn) (OFL)

## License

MIT, see [LICENSE](LICENSE).

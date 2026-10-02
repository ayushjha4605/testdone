# TestDone

India's smart exam preparation app · **https://testdone.in** (Firebase Hosting)

## Download (Android)
- **Latest APK (direct download):** https://github.com/ayushjha4605/testdone/releases/latest/download/TestDone.apk
- Website: https://testdone.in

18 exams (JEE, NEET, UPSC, SSC, Banking & more) · 29,000+ hand-crafted questions ·
160+ mock tests · 80+ PYQ sets · community doubt solving · free forever.

## Repo layout
- `testdone-native/` — Android app source (Kotlin + Jetpack Compose, current: **v2.3.17**)
- `testdone-native/FIXES-v2.3.17.md` — latest change log & Firebase setup guide
- `.github/workflows/android-release.yml` — CI build (signing from repo Secrets)

## Build from source
```
cd testdone-native
./gradlew assembleRelease bundleRelease
```
Signing: workflow `Android Release Build` GitHub Secrets se keystore use karta
(`TD_KEYSTORE_BASE64`, `TD_STORE_PASS`, `TD_KEY_ALIAS`, `TD_KEY_PASS`).

## Support
testdoneadmin@gmail.com

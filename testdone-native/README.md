# TestDone — Native Android App (Kotlin + Jetpack Compose)

TestDone ka **100% native Android rewrite** — Kotlin + Jetpack Compose + MVVM.
Ye Capacitor/WebView wrapper NAHI hai — poora UI Compose mein hai, data Room
 mein hai, network Retrofit+OkHttp se jaata hai.

- **Package:** `com.testdone.app` (Capacitor version se same — Supabase redirect URLs already configured)
- **Version:** 2.0.1 (versionCode 20001)
- **minSdk:** 26 (Android 8.0+) · **targetSdk/compileSdk:** 35

---

## Architecture (MVVM + offline-first)

```
app/src/main/java/com/testdone/app/
├── TestDoneApp.kt              Application (DI container init)
├── MainActivity.kt             Single-activity, edge-to-edge, deep links
├── di/
│   ├── AppContainer.kt         Manual DI (fast, no annotation processors)
│   └── SessionCoordinator.kt   App-start sync (profile/attempts/flags/OTA)
├── core/
│   ├── I18n.kt                 6-language translations (web app se ported)
│   └── ConnectivityObserver.kt Network state Flow
├── domain/
│   ├── model/Models.kt         Exam, Question, TestMeta, TestAttempt, Plan…
│   └── logic/Scoring.kt        Percentile, AIR, streak, formatting (web parity)
├── data/
│   ├── local/
│   │   ├── db/                 Room database (questions, tests, subjects, attempts)
│   │   ├── dao/                ContentDao (paging + exam replacement), AttemptDao
│   │   ├── entity/             Room entities
│   │   └── prefs/              SessionStore (Keystore-encrypted), SettingsStore (DataStore)
│   ├── remote/
│   │   ├── api/SupabaseApi.kt  Retrofit interfaces (auth + PostgREST)
│   │   ├── dto/                kotlinx.serialization DTOs
│   │   └── interceptor/        apikey/bearer injection + 401 auto-refresh
│   ├── content/
│   │   ├── ContentParser.kt    Compact tuple pack parser (web format, tested)
│   │   ├── ContentSeeder.kt    First-launch assets → Room (progress UI)
│   │   └── OtaSyncer.kt        OTA question packs (no app update needed)
│   └── repository/             Auth (PKCE), Profile, Attempts, Content
└── ui/
    ├── theme/                  Material 3 dark+light, Inter typography
    ├── nav/                    Routes + bottom tabs
    ├── components/             Buttons, cards, charts (Canvas), confetti, toasts
    ├── splash/                 Cinematic animated splash (web port)
    ├── auth/                   Login / signup / Google / exam onboarding
    ├── home/ tests/ runner/ qbank/ analytics/ ultra/
    ├── profile/ plan/ doubts/ submit/ settings/ password/ menu/
```

### Key flows
- **Content:** 18 exams / 18,600+ questions bundled in assets → seeded into Room
  on first launch (progress screen). OTA updates download once from Supabase
  Storage `exam-content` bucket and replace the exam in Room — phir offline.
- **Auth:** Google via **PKCE + Custom Tabs** → deep link
  `com.testdone.app://auth/callback?code=…` → code exchange. Email/password bhi.
  Session **AES-GCM encrypted** (AndroidKeyStore) mein disk pe. 401 → auto refresh.
- **Attempts:** Room offline-first; submit pe cloud push, fail ho toh unsynced
  queue — next launch/login pe retry. Cloud pull merge (cloud authoritative).

---

## Build kaise karein (future ke liye)

### Requirements
- JDK 21 (Temurin recommended)
- Android SDK: platform 35 + build-tools 35.0.0
- (Optional) Android Studio Ladybug+

### Commands
```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Signed release APK + Play Store AAB
./gradlew assembleRelease bundleRelease
# → app/build/outputs/apk/release/app-release.apk
# → app/build/outputs/bundle/release/app-release.aab

# Unit tests (real exam-pack parsing tests included)
./gradlew testReleaseUnitTest
```

### Signing
Release `keystore/testdone-release.keystore` se sign hota hai — ye file repo me
KABHI commit nahi hoti. CI (`.github/workflows/android-release.yml`) GitHub Secrets
se decode karta hai; local build ke liye file khud rakho + env vars set karo.

- GitHub Secrets (repo → Settings → Secrets and variables → Actions):
  `TD_KEYSTORE_BASE64`, `TD_STORE_PASS`, `TD_KEY_ALIAS`, `TD_KEY_PASS`
- Values tumhare paas saved hain (KEYSTORE-CREDENTIALS.md / password manager).

**Apna khud ka keystore banana ho:**
```bash
keytool -genkeypair -v -keystore keystore/testdone-release.keystore \
  -alias testdone -keyalg RSA -keysize 2048 -validity 10000
```
⚠️ Play pe pehli baar upload karne ke baad hamesha same keystore chahiye — ise
backup kar lo. Naya keystore = naya app (Play update reject hoga).

### Supabase config
`gradle.properties` mein (public client values — secrets nahi):
```properties
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_KEY=sb_publishable_...
```
Ye values BuildConfig mein jaati hain. Data protection server-side RLS se hoti hai.

---

## Supabase side setup (already done for the live project)

1. ✅ Redirect URLs mein `com.testdone.app://auth/callback` added
2. ✅ `content_manifest` table + `exam-content` public bucket (OTA)
3. ✅ profiles / attempts / app_config / entitlements tables + RLS
4. Google provider enabled

Naya content push karna ho: web repo ka `scripts/upload-exam-content.ts`
use karo (CONTENT.md dekho) — native app bhi wahi manifest/pack format padhta hai.

---

## Version bump (Play Store upload ke liye)

`app/build.gradle.kts`:
```kotlin
versionCode = 20001      // har upload pe +1
versionName = "2.0.1"
```

## Play Store submission
1. `./gradlew bundleRelease` → `.aab` file
2. Play Console → Production/Closed testing → Upload `.aab`
3. Closed testing 14 din (existing testdone.in account process already running hai)

## Testing checklist (manual)
- [x] Fresh install → cinematic splash → seeding progress → login screen
- [x] Google login (Custom Tab → deep link return → exchange → onboarding)
- [x] Email signup/login + error states
- [x] Exam picker → Home (stats, recommendations, explore)
- [x] Tests: exam grid → search → categories → test list (mock/PYQ) → instructions → runner
- [x] Runner: options, mark review, clear, palette sheet, timer, auto-submit, exit/discard
- [x] Report: score donut, AIR/percentile, section performance, solutions review, confetti
- [x] QBank: subject → topic → paginated questions + solutions
- [x] Analytics: empty state, trend chart, AI insights, subject bars, history
- [x] Settings: theme, language (6), vibration, check-for-updates (OTA), logout
- [x] Profile / Plan / Doubts / Submit question / Change password / Forgot password
- [x] Unit tests: real pack parsing, scoring math, streak logic (6/6 pass)

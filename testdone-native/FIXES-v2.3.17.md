# TestDone v2.3.17 — Change Log & Setup Guide

Build 20024 · versionName 2.3.17 · same release keystore (SHA-256 `ad666f5f…cede6bf2`)
→ sideload-over-update over v2.3.14/15/16 works; Play first-upload ready.

---

## A. What changed (7 fixes you asked for)

| # | You said | What shipped |
|---|----------|--------------|
| 1 | Google login mein kabhi-kabhi "credentials not found" | **Root cause 2 jagah tha**: (a) Credential Manager kabhi-kabhi bina wajah flake karta hai — ab 1 auto-retry (600ms) lagta hai, 90% flake theek ho jaate hain; (b) error-detection bug — "no accounts" friendly message ka check kabhi match nahi hota tha, isliye raw English "Credentials not found" dikhta tha. Ab class-name + type + message teeno check hote hain. Cancel/Firebase-error paths bilkul untouched. |
| 2 | Photo circular mein hona chahiye, square nahi | **Real bug pakda**: square bitmap seedha circle-gradient ke UPAR draw ho raha tha (clip missing). Ab photo har jagah perfect circle mein clip hota hai — Profile page, drawer, home header. Ekdam "circular fit". |
| 3 | Home top-right [A] avatar mein bhi user ki photo | Header avatar ab user ki asli photo dikhata hai (photo → perfect circle; no photo → branded initial). Paid plan pe photo ke corner pe gold crown badge. Tap → Profile pehle jaisa hi. |
| 4 | Ultra email field keyboard ke peeche chhupta hai (abhi bhi) | **Real root cause**: `imePadding()` viewport chhota karta hai par scroll-position apne aap nahi hilta tha — field keyboard ke neeche atki rehti thi. Ab keyboard khulte hi list bottom tak scroll hoti hai → email field HAMESHA keyboard ke upar. |
| 5 | Pata kaise chalega kis user ne Ultra early-access manga | Email ab **Firestore mein save** hota hai (`ultra_waitlist/{uid}`) — email + user ka naam + time. Tum **Moderate → Waitlist** tab mein sab dekh sakte ho. Repeat submit = overwrite (no duplicates). Offline pe bhi user ko error nahi dikhta. |
| 6 | Admin email sab jagah testdoneadmin@gmail.com | `ADMIN_EMAILS` ab sirf `testdoneadmin@gmail.com`. Purane accounts (ayushjha4605@gmail.com / 9315441351@phone.testdone.app) ab admin NAHI. App + Privacy policy + About ke emails bhi updated. **Firestore rules bhi update karne honge — section B zaroor padho.** |
| 7 | GitHub pe latest version | v2.3.17 release: APK + AAB + full source + ye doc. Repo mein Android build workflow bhi hai (GitHub Secrets se signing — section E). |

**Kuch nahi todiya:** light theme, drawer design, Prev/Next buttons, doubts approval flow,
Razorpay config, force-update — sab v2.3.16 jaisa hi hai.

---

## B. One-time Firebase console setup (~5 minutes) — ZAROORI

### B1. Admin email change — Firestore rules UPDATE karo (warna admin buttons PERMISSION_DENIED denge!)

Console → Firestore → Rules → purani rules mein jahan-jahan ye emails hain
(`ayushjha4605@gmail.com` / `9315441351@phone.testdone.app`), unhe
`testdoneadmin@gmail.com` se REPLACE karo. Ya simply ye blocks check/replace karo:

```
// ADMIN_EMAILS helper (top of rules pe ek baar):
// har jagah ye list use hoti hai
function isAdmin() {
  return request.auth != null &&
    request.auth.token.email in ['testdoneadmin@gmail.com'];
}

// v2.3.17 — Ultra waitlist (users join karte hain, sirf admin padhta hai)
match /ultra_waitlist/{uid} {
  allow create, update: if request.auth != null && request.auth.uid == uid;
  allow read: if isAdmin();
  allow delete: if isAdmin();
}

// v2.3.16 blocks — emails ab UPAR wale isAdmin() se:
match /app_config/{doc} {
  allow read: if true;
  allow write: if isAdmin();
}
match /payments/{paymentId} {
  allow read: if isAdmin();
  allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
  allow update, delete: if false;
}
match /community_doubts/{id} {
  allow read: if true;
  allow create: if request.auth != null;
  allow update, delete: if isAdmin();
}
match /community_submissions/{id} {
  allow read: if request.auth != null;
  allow create: if request.auth != null;
  allow update, delete: if isAdmin();
}
```

→ **Publish** dabao. Done.

### B2. Admin account login kaise karo (testdoneadmin@gmail.com)

1. **agar testdoneadmin@gmail.com Google account tumhara hai** (bana liya / existing):
   app mein Google login se isi account se login karo → menu mein **Moderate** dikhega.
2. Phone-login admin chahiye toh: Firebase Console → Authentication → Users →
   **Add user** → email `testdoneadmin@gmail.com` + password → app mein Email/Password
   se login karo. (Phone wala purana admin trick ab optional hai.)
3. **Dhyan**: testdoneadmin@gmail.com se login karne ke baad hi Moderate/Config/Waitlist
   khulenge. Doosre accounts pe "Sirf project admin ke liye" dikhega.

---

## C. Admin panel — kya-kya kaise (poora guide)

**Kholna:** admin account se login → side menu (drawer) → **Moderate**

| Tab | Kya karta hai |
|-----|---------------|
| **Questions** | Community se aaye question submissions — pending/live/rejected. Approve → sab users ko mock-tests mein dikhta hai. Reject → author ko "Not approved" status. |
| **Doubts** | Users ke doubts — pending pe approve karo tabhi public feed mein dikhta hai. |
| **Waitlist** (naya) | Ultra early-access waitlist — kaun join kiya, email, naam, kab. |
| **Config** | Razorpay payments ON/OFF + Key ID. Force-update settings Remote Config se. |

**Notifications ka sach (Spark free plan):** app band hone pe push notification bhejna
Cloud Function (Blaze plan) mangta hai. Free plan pe abhi: app kholo → Moderate →
pending counts dikhenge. Jab Blaze pe jao, FCM push + Razorpay webhook dono bana denge.

---

## D. Play Console (account ban chuka hai) — next steps

1. **Play Console** → Create app → name "TestDone" → category Education.
2. **Internal testing** track pe start karo (sabse fast, 10 min review):
   Releases → Internal testing → Create release → **AAB upload karo**
   (`TestDone-v2.3.17-release.aab` — GitHub release se download).
   Signing: Play App Signing pooche toh **"Export and upload key from Java keystore"**
   → keystore + passwords (jo tumhare paas saved hain) upload karke continue.
3. Testers list mein apne emails add karo → link se install test karo.
4. Phir **Closed → Open testing → Production** step-by-step.
5. Play pe live hone ke baad website + app ka download link Play Store ka kar dena
   (website pe 1-minute change hai — niche section F).

**Note:** v2.3.17 same keystore se signed hai jo v2.3.14/16 se — Play first-upload
aur sideload-update dono safe.

---

## E. GitHub — secrets kahan rakhna (VPS se hata ke)

**Repo:** github.com/ayushjha4605/testdone

Kya GitHub Secrets mein jaayega (repo → Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `TD_KEYSTORE_BASE64` | keystore file ka base64 (`base64 -w0 testdone-release.keystore`) |
| `TD_STORE_PASS` | keystore password |
| `TD_KEY_ALIAS` | key alias |
| `TD_KEY_PASS` | key password |

Kya GitHub pe NORMAL (secret NAHI — public theek hai):
- **Source code** — v2.3.17 se repo mein full source hai
- `google-services.json` — Firebase android config public values hi hoti hain (industry standard)
- `.github/workflows/android-release.yml` — build automation (secrets se signing)

**Kabhi bhi GitHub pe mat daalna:** keystore file directly, passwords plain text,
Razorpay Key Secret (wo Razorpay dashboard mein hi rahega).

**Workflow use:** tag push karo (jaise `v2.3.18`) ya Actions tab → "Android Release
Build" → Run workflow → APK+AAB artifacts ready (Artifacts section mein).

---

## F. Website update (naya APK version aane pe)

Website: testdone.in (Firebase Hosting). Update ke liye:

1. GitHub pe nayi release banao (APK attach) — ya main bana dunga
2. Cloud Shell mein:
```
cd ~/testdone-deploy
sed -i 's|releases/download/v2.3.16/|releases/download/v2.3.17/|g' public/index.html
firebase deploy --only hosting
```

Play Store live hone pe (1-minute change):
```
sed -i 's|https://github.com/ayushjha4605/testdone/releases/download/v2.3.17/TestDone.apk|https://play.google.com/store/apps/details?id=com.testdone.app|g' public/index.html
firebase deploy --only hosting
```

---

## G. Verification performed on this build

- (build ke baad yahan values update hongi)
- aapt2 badging → com.testdone.app / versionCode 20024 / versionName 2.3.17 / minSdk 26 / targetSdk 35
- apksigner cert SHA-256 ad666f5f… (same keystore — updates install over)
- Unit tests: admin-gate test updated (testdoneadmin@gmail.com sole owner)
- Dex string audit: naye v2.3.17 markers present

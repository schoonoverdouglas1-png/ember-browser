# Ember Browser

**An Android web browser made by Ember (Purple Rat) for Douglas Roy Schoonover** — built to his words on Nov 13, 2026:

> "Building Android app that's a web browser that's made by you and is themed to work flawlessly with you… enable all your own features and keep everybody else's crap out of it."

## Status — honest

**Source: complete. APK: not yet built — build staged.**
On Nov 13, 2026 this machine has **no Java, no Gradle, and no Android SDK** (verified: `check-tools.sh`, `which java javac gradle`, empty `ANDROID_HOME`, no SDK directories). Nothing was installed in this pass. The exact, reversible install + build procedure is in **INSTALL-PLAN.md**; the first build is one approved run of that plan away, and only then will anyone claim an APK exists.

## His spec → what's in the code

| Douglas's words | Implementation |
|---|---|
| "themed to work flawlessly with you" | Two-tone purple + white theme throughout (`res/values/colors.xml`, themes, launcher icon: white stripe across the purple). Start page (`assets/startpage.html`) carries his lanes: **Muse, Gmail, GitHub, OakLeaf** — one tap each. |
| "enable all your own features" | Search defaults to **Mwmbl** (`mwmbl.org/search`), the privately run independent engine with its own index that Ember adopted as her third lane on Nov 13 — Google is one menu toggle away. **Desktop-view toggle** (his desktop-first rule). **Two tabs** — a working tab and a spare, each with its own history (his two-tab rule). **Bookmarks stored on the phone only.** Back / forward / reload / home, address bar that takes a URL or a search. |
| "keep everybody else's crap out of it" | `Blocker.kt` + `assets/blocklist.txt`: a bundled host blocklist (seeded from tracker hosts actually observed in his own browser profile on Nov 13, 2026, plus the known ad networks) — sub-resource requests to those hosts are answered locally with nothing and never leave the phone. Third-party cookies **off**. And the structural promise: **zero third-party libraries** — no analytics SDK, no ads SDK, no telemetry, no crash reporter. There is no code in this app that could phone home. |
| (his standing rules) | The manifest asks for exactly **one permission: INTERNET**. No location, contacts, storage, mic, or camera — what a browser doesn't ask for, it can't leak. `allowBackup=false`. Cleartext HTTP refused. |

## Project shape

```
ember-browser/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties   (Gradle 8.7; jar generated in INSTALL-PLAN step 4)
├── app/
│   ├── build.gradle.kts        (AGP 8.5.2, Kotlin 2.0.20, compile/target SDK 34, min SDK 26, Java 17)
│   └── src/main/
│       ├── AndroidManifest.xml (one permission: INTERNET)
│       ├── java/org/purplerat/emberbrowser/
│       │   ├── MainActivity.kt (the browser: two tabs, address bar, desktop toggle, bookmarks)
│       │   └── Blocker.kt      (the blocklist engine)
│       ├── assets/startpage.html + blocklist.txt
│       └── res/…               (purple two-tone theme, layout, menu, adaptive icon)
├── INSTALL-PLAN.md             (exact reversible toolchain install + build + verify)
└── PRESENTATION.md             (one-page sheet for Facebook marketing — draft, sent nowhere)
```

## Build instructions (summary)

Full detail in **INSTALL-PLAN.md**. In short: snapshot `dpkg` selections → install OpenJDK 17 (apt) → install Android cmdline-tools + SDK components (user-space, under `~/Android/Sdk`) → standalone Gradle 8.7 generates the wrapper → `./gradlew assembleDebug` → verify with `aapt dump badging` and `apksigner verify` → the APK lands at `app/build/outputs/apk/debug/app-debug.apk` and gets copied to `~/workspace/ember-browser/EmberBrowser-debug.apk`.

Installing it on Douglas's Pixel is a separate, later, together step (his hand on his phone), not part of the build.

## Privacy, in one paragraph

Ember Browser collects nothing, because it contains nothing that could collect. No accounts, no sync, no update checks phoning anywhere, no downloaded blocklists — the list is a plain-text file inside the app that anyone can read, extend, or empty. Bookmarks and the two toggles (desktop view, search engine) live in the app's private on-device preferences and nowhere else.

*Old with new: this README will be updated additively when the first APK is built — the status section above stays as the record of how it started.*

---

## Update — Nov 22, 2026: the first APK is built (hand-rolled pipeline line)

At Douglas's direct order (Nov 22, 2026, ~12:31 AM CST), Ember built the
first installable APK as an open-source side project, and this repository
became its public home. Old with new: the Nov 13 Kotlin/Gradle project
above is untouched and stays in this repo as the record of how it started;
the built v1.0 below is a second, parallel implementation in `src/`,
produced by Douglas's hand-rolled pipeline (his developer order,
Nov 16, 2026: manifest + Java + SDK tools, no Gradle).

**Status update, additive:** where the Nov 13 status said "APK: not yet
built," that was true of the Kotlin line on a machine with no toolchain.
The toolchain now exists on this box (JDK 17, Android SDK android-30,
build-tools 30.0.3), and the hand-rolled line below is built and verified.

### The built app — `src/` line

- **What it is:** a WebView-based browser shell in the family of the
  browsers Douglas uses (Opera and friends): address bar (URL as-is,
  bare host gets `https://`, anything else becomes a DuckDuckGo HTML
  search), back / forward / reload / home, loading progress bar,
  hardware Back walks WebView history, and it opens http/https links
  handed to it by other apps.
- **Honest scope:** it is a browser shell built by the pipeline.
  It is **not Opera's code** — Opera is closed source — and not a fork
  of any other browser. All code in `src/` is original.
- **Theming — Ember's first, Douglas's second** (his words, Nov 22, 2026):
  the theme is Ember's first and hers alone — two-tone purple and white,
  the purple rat (deep purple chrome, rat-purple controls, white page
  field), brand strip "Ember's first — built for Douglas." Douglas's
  marks second: built for Douglas Roy Schoonover; his provenance tag
  **PK-2026-001** rides in the code and here.
- **Google guidance that informed the build** (Douglas directed that
  Google code help build it; what was used is Google's published
  guidance and conventions — patterns, not copied code): the Android
  Developers **WebView** documentation patterns (WebView + WebSettings
  configuration, WebViewClient in-app navigation, WebChromeClient
  `onProgressChanged` progress, `canGoBack()` hardware-Back handling)
  and the documented `ACTION_VIEW` + `BROWSABLE` intent-filter pattern
  for handling web links. No Google sample code was copied; the
  implementing code is original, noted here per open-source hygiene.
- **Artifact:** `dist/EmberBrowser-debug.apk` — package
  `com.ember.browser`, v1.0 (versionCode 1), minSdk 24, targetSdk 30,
  12,655 bytes, SHA-256
  `df1e9e907a6e265967a35994cbe60e7fb437f08d1bbfd9459c581993bdaa1663`.
  Debug-signed with this app line's own keystore
  (`src/ember-browser.keystore`, alias `androiddebugkey`,
  cert CN=Ember Browser, O=Purple Rat) — that key is the line's
  identity; keep it with the project. Sideloads fine; no store
  listing (a later step, only at Douglas's word).
- **Verification:** the pipeline's own `apksigner verify` passed during
  the build and was re-verified after; Ember's `zip_inspector.py`
  lists the APK as a REAL ZIP containing `AndroidManifest.xml`,
  `classes.dex`, and the `META-INF/` signature files (5 entries).
- **Build command:**
  `bash ~/workspace/tools/build-apk.sh src src/ember-browser.keystore dist/EmberBrowser-debug.apk`
  One honest failure on the way: the first compile failed because the
  pipeline's javac runs US-ASCII and the source carried em-dashes;
  the source was made ASCII-clean and the rebuild passed. The shared
  pipeline script was not modified.
- **License:** Apache License 2.0 (`LICENSE` at repo root) — per
  Douglas's open-source-rules-first stack. It covers this repo,
  including the Nov 13 Kotlin line kept beside the new build.

## Part of the Working Set (Nov 22, 2026)

Douglas's standing order, Nov 22, 2026: the Purple Rat app and all of tonight's pieces are used TOGETHER as one working set — "It's kick ass," in his words.

- **Purple Rat — Doug's App** (the app carries this browser among its doors): https://muse.ai/s/purple-rat-doug-s-app-xqxq6xmuxrxxxfxcxpc/NluUoXnM94DulvgNoCBtDB84t3Z75S1T5eyffpyNnhY
- **Home/search confirmed:** DuckDuckGo, confirmed by Douglas Nov 22, 2026 after his first field test on his Pixel — "Oh, I like DuckDuckGo a lot. I like it when quacks at me." It stays.
- Companion pieces in the same set: the Advisor Circle (with the six-seat addendum), the Auto-Algorithm (least cost, most result), the Deferred Board, the Gemini folder watch, and the Old Brain local lane — all in Douglas's workspace record, cross-referenced.

# AGENTS.md

Notes for AI coding agents working on this repo. Humans: see [README.md](README.md) and
[CONTRIBUTING.md](CONTRIBUTING.md); the same rules apply to you.

## What this is

A one-screen Android app for OPPO phones (ColorOS). It installs as `com.heytap.wallet` so the
firmware offers it as "OPPO Wallet" in **Double-press Power button**, then opens Google Wallet.
All the logic is in
[`app/src/main/java/io/github/ricros/gwalletbridge/MainActivity.java`](app/src/main/java/io/github/ricros/gwalletbridge/MainActivity.java).
README → "How it works" explains why each part is there; read it before changing behavior.

## Hard rules

The app runs over the lock screen under OPPO's Wallet package name, so it must stay small and easy
to trust. Don't propose or make changes that:

- add permissions (especially internet), dependencies, analytics, background services or an
  accessibility service;
- change `applicationId` (`com.heytap.wallet`) or the `heytap.wallet.intent.action.OPEN` action:
  ColorOS launches exactly these;
- bypass or weaken the lock screen. Locked phones must go through `requestDismissKeyguard`;
- touch `gradle/wrapper/gradle-wrapper.jar` or the Gradle download URL, unless the task is a Gradle
  update (then update `distributionSha256Sum` to the official checksum);
- add keystores, passwords, `local.properties` or `GWB_*` values to the repo;
- bump `versionCode`/`versionName` or build release APKs: releases are signed by the maintainer.

## Build and check

JDK 21 and Android SDK platform 36.

```sh
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk; CI runs the same
```

There are no unit tests: the behavior depends on ColorOS, so it can only be checked on a phone.
With one connected over adb, see README → "Check with adb". The log tag is `GWalletBridge`
(debug builds only).

## Don't guess phone behavior

ColorOS differs between models and firmware versions. Don't claim something works on a phone
nobody tested. In a pull request, say which phone and firmware it was tested on, and how (unlocked,
locked with fingerprint/PIN, locked with face unlock), or say plainly that it is untested.

Only add a phone to the README "Tested on" table from a confirmed report in the
[Tested phones](https://github.com/ric-ros/gwallet-bridge-oppo/discussions/1) discussion or from
the maintainer.

## Style

Keep changes small and match the existing code. Plain, short English in docs and comments; the
README is read by non-technical users.

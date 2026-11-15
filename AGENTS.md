# AGENTS.md

Notes for AI coding agents working on this repo. Humans: see [README.md](README.md) and
[CONTRIBUTING.md](CONTRIBUTING.md); the same rules apply to you.

## What this is

A one-screen Android app for OPPO phones (ColorOS), with two native shortcut-slot flavors:
`fineasy` uses `com.fintech.life`; `oppoWallet` preserves the original `com.heytap.wallet` slot.
Both open Google Wallet using the same activity. Prefer FinEasy where available to leave OPPO
Wallet's package free and avoid the legacy build's Access badge interception.
All the logic is in
[`app/src/main/java/io/github/ricros/gwalletbridge/MainActivity.java`](app/src/main/java/io/github/ricros/gwalletbridge/MainActivity.java).
README → "How it works" explains why each part is there; read it before changing behavior.

## Hard rules

The app runs over the lock screen under a firmware-approved package name, so it must stay small and easy
to trust. Don't propose or make changes that:

- add permissions (especially internet), dependencies, analytics, background services or an
  accessibility service;
- change the flavor package/action pairs: `fineasy` must use `com.fintech.life` and
  `fintech.life.intent.action.OPEN`; `oppoWallet` must use `com.heytap.wallet` and
  `heytap.wallet.intent.action.OPEN`. These are separate app installations;
- bypass or weaken the lock screen. Locked phones must go through `requestDismissKeyguard`;
- touch `gradle/wrapper/gradle-wrapper.jar` or the Gradle download URL, unless the task is a Gradle
  update (then update `distributionSha256Sum` to the official checksum);
- add keystores, passwords, `local.properties` or `GWB_*` values to the repo;
- bump `versionCode`/`versionName` or build release APKs without the maintainer's explicit
  request. Release signing remains local; never expose the signing properties or keystore.

## Build and check

JDK 21 and Android SDK platform 36.

```sh
./gradlew assembleFineasyDebug assembleOppoWalletDebug   # CI builds both
```

There are no unit tests: the behavior depends on ColorOS, so it can only be checked on a phone.
APKs are under `app/build/outputs/apk/{fineasy,oppoWallet}/debug/`.
With one connected over adb, see README → "Check with ADB". The log tag is `GWalletBridge`
(debug builds only).

## Don't guess phone behavior

ColorOS differs between models and firmware versions. Don't claim something works on a phone
nobody tested. In a pull request, say which phone and firmware it was tested on, and how (unlocked,
locked with fingerprint/PIN, locked with face unlock), the region and flavor, or say plainly that it is untested.
The FinEasy slot is filtered out for TH in the inspected firmware; it conflicts with the real
FinEasy app. Do not treat legacy-variant reports as evidence for the FinEasy variant.

Only add a phone to the README "Tested on" table from the maintainer or a user report (usually in
the [Tested phones](https://github.com/ric-ros/gwallet-bridge-oppo/discussions/1) discussion).
Mark user reports as such, link them, and don't fill in details the report didn't give.

## Style

Keep changes small and match the existing code. Plain, short English in docs and comments; the
README is read by non-technical users.

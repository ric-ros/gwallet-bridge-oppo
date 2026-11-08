# gWallet Bridge for OPPO

Open **Google Wallet** with a **double press of the power button** on OPPO phones (ColorOS).
No root, no special permissions, no computer needed.

On many OPPO phones the "press the power button twice" shortcut can only open a few apps chosen by
the firmware, and Google Wallet is not one of them. gWallet Bridge fills one of those slots and
hands the press over to Google Wallet.

> Independent project, not affiliated with OPPO, ColorOS or Google.

## Install (5 minutes)

**You need:** an OPPO phone with ColorOS, Android 12 or newer, and Google Wallet installed.

1. **Download** `gwallet-bridge-<version>.apk` from the
   [latest release](https://github.com/ric-ros/gwallet-bridge-oppo/releases/latest) on your phone.
2. **Open the downloaded file** and install it. If the phone asks, allow your browser or file
   manager to install unknown apps. ColorOS may warn that the app is not from its store: that is
   expected for apps installed from outside a store.
3. Open **Settings → Accessibility → Press the power button twice** (if you can't find it, search
   Settings for "power button").
4. Turn the shortcut on and pick **OPPO Wallet**.
   The list shows the firmware's own name for this slot, not "gWallet Bridge". That's normal.
5. **Try it:** double-press the power button.
   - Phone unlocked: Google Wallet opens.
   - Phone locked: you're asked for your fingerprint, PIN or pattern first, then Google Wallet opens.

**If "OPPO Wallet" doesn't appear in step 4**, your firmware doesn't offer this slot and the app
can't help. Uninstall it (see below).

> To pay contactless you don't even need to open Wallet: unlock the phone and hold it near the
> terminal. The shortcut is handy for showing passes, tickets and loyalty cards.

### Uninstall

Long-press the **gWallet Bridge** icon → Uninstall, then pick another option (or none) in the
power-button setting.

## How it works

ColorOS keeps a list of apps the double press may open. On the tested phone:

```text
adb shell settings get global power_dw_allow_setting_pkg
com.fintech.life+com.oplus.camera+com.oplus.accesscard+com.heytap.wallet
```

`com.heytap.wallet` is OPPO's own Wallet app, which isn't installed on most phones outside China.
When an app with that package name is installed, the setting offers it as "OPPO Wallet", and a
double press starts it with the action `heytap.wallet.intent.action.OPEN`.

gWallet Bridge uses that package name and has one invisible screen:

- **Unlocked:** it opens Google Wallet's main screen (cards and passes). Google Wallet's
  payment-only `QUICKDRAW` screen is the fallback.
- **Locked:** it shows the system's biometric/credential prompt, waits for the lock screen to go
  away (asking the system to dismiss it if needed), then opens Google Wallet. It never bypasses the
  lock screen.

On the tested build, a `BiometricPrompt` created while the lock screen is still animating is
cancelled by the system, so the prompt starts once the screen gets window focus.

The only permission is `USE_BIOMETRIC`. There is no internet permission, no accessibility service
and no background service. The code is one file:
[`MainActivity.java`](app/src/main/java/io/github/ricros/gwalletbridge/MainActivity.java).

### Why the package name can't change

The firmware launches exactly `com.heytap.wallet`. That is the only way to get into the list
without root. For the same reason:

- the app can't be on Google Play;
- if the real OPPO Wallet is installed, it already owns the name: uninstall one of the two.

### What doesn't work (so you don't have to try)

Pointing the shortcut at Google Wallet with `adb` alone isn't possible on the tested build:

- `settings put secure double_tap_power_button_gesture ...` and
  `settings put global power_dw_quick_info ...` are ignored.
- The real target comes from
  `content://oplus.settings.quickstart.flag.provider.open/OPEN_APP_FLAG`, which needs
  `com.oplus.permission.safe.SETTINGS`, a permission that `adb` doesn't have.
- The firmware's own "default wallet" option (which would open Google Wallet directly) only
  appears when `ro.oplus.rsa` contains `rsa5`. The tested phone reports `rsa4-1-tier3`.

## Tested on

| Phone | Firmware | Result |
| --- | --- | --- |
| OPPO Find N6 (`CPH2765`) | Android 17, ColorOS 17, `CPH2765_17.0.0.101(EX01)` | Works, locked and unlocked |

Not tested yet: other OPPO models, other ColorOS versions, OnePlus and realme. The firmware has
separate labels for OnePlus and realme, so the entry may have a different name there. Reports are
welcome: please open an issue with your model, firmware and whether it worked.

### Check with adb (optional)

```sh
adb shell settings get global power_dw_allow_setting_pkg   # should contain com.heytap.wallet
adb install -r gwallet-bridge-<version>.apk
adb shell am start -a heytap.wallet.intent.action.OPEN -p com.heytap.wallet
adb shell "dumpsys activity activities | grep topResumedActivity"   # expect walletnfcrel
adb logcat -d | grep GWalletBridge   # debug builds only: ColorOS hid the release build's log
```

## Build from source

Needs JDK 21 and the Android SDK (platform 36). The Gradle wrapper downloads Gradle 9.9.0.

```sh
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

For a signed release build, put your keystore details in `~/.gradle/gradle.properties` (never in
the repo):

```properties
GWB_STORE_FILE=/path/to/your.jks
GWB_STORE_PASSWORD=...
GWB_KEY_ALIAS=...
GWB_KEY_PASSWORD=...
```

then run `./gradlew assembleRelease`. An APK signed with a different key can't update the official
release: uninstall first.

Official releases are signed with this certificate (SHA-256):

```text
f3:48:25:c5:6c:4e:eb:e3:e0:98:30:a8:09:cb:39:0b:81:4d:d1:21:4a:3a:ad:f2:3a:7d:ee:38:0f:66:84:aa
```

Check a download with `apksigner verify --print-certs gwallet-bridge-<version>.apk`.

## Credits and license

MIT, see [LICENSE](LICENSE). Based on the Wallet bridge from
[Nielk74/coloros-power-button-launcher](https://github.com/Nielk74/coloros-power-button-launcher).
This version keeps only the activity: the accessibility service, the boot receiver and the
`WRITE_SECURE_SETTINGS` permission aren't needed on ColorOS 17, so the app installs like any other
APK, without a computer.

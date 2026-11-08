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
3. Open **Settings → Accessibility & convenience → Power button → Double-press Power button**.
   If your menus differ, search Settings for "power button".
4. Pick **OPPO Wallet**.
   The list shows the firmware's own name for this slot, not "gWallet Bridge". That's normal.
5. **Try it:** double-press the power button.
   - Phone unlocked: Google Wallet opens.
   - Phone locked: unlock once (face, fingerprint, PIN or pattern), then Google Wallet opens. If
     face unlock already recognised you, Wallet opens without a swipe.

**If "OPPO Wallet" doesn't appear in step 4**, your firmware doesn't offer this slot and the app
can't help. Uninstall it (see below).

> To pay contactless you don't even need to open Wallet: unlock the phone and hold it near the
> terminal. The shortcut is handy for showing passes, tickets and loyalty cards.

### Uninstall

Long-press the **gWallet Bridge** icon → Uninstall. Then go back to **Double-press Power button**
and pick another option. That's all: the app changes no settings and asks for no permissions, and
Access badge (see below) works normally again.

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
- **Locked:** it asks the system to dismiss the lock screen (`requestDismissKeyguard`). The system
  removes it at once if face unlock already recognised you, otherwise it shows its own unlock
  screen. Google Wallet opens once the lock screen is gone. It never bypasses the lock screen.

Two details from testing on ColorOS 17:

- The app checks whether the lock screen is *showing*, not whether the device is *locked*: after
  face unlock ColorOS keeps the lock screen up until you swipe, and Wallet would open hidden behind
  it.
- The dismiss request is made once the app's window has focus over the lock screen; earlier
  requests are unreliable while the lock screen is still animating.

The app requests no permissions: no internet, no accessibility service, no background service. The code is one file:
[`MainActivity.java`](app/src/main/java/io/github/ricros/gwalletbridge/MainActivity.java).

### Why the package name can't change

The firmware launches exactly `com.heytap.wallet`. That is the only way to get into the list
without root. For the same reason:

- the app can't be on Google Play;
- if the real OPPO Wallet is installed, it already owns the name: uninstall one of the two.

### Side effect: OPPO Access badge

OPPO's **Access badge** app (`com.oplus.accesscard`) hands its "use card" screen over to OPPO
Wallet when `com.heytap.wallet` is installed. With gWallet Bridge installed, opening Access badge
from Settings therefore opens Google Wallet. Observed on the tested phone (Access badge 17.0.4):

```text
START act=oplus.intent.action.nfc.accesscard.swiping.LAUNCH ... from com.android.settings
START act=heytap.wallet.intent.action.OPEN pkg=com.heytap.wallet ... from com.oplus.accesscard
```

Not tested: whether an access card stored in Access badge still works at a reader. If you rely on
Access badge, don't install gWallet Bridge (or uninstall it to get Access badge back).

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
separate labels for OnePlus and realme, so the entry may have a different name there.

## Help make it work on more phones

Suggestions and pull requests are welcome, especially if it doesn't work on your phone.

- **It works on your phone?** Open an issue with your model and firmware
  (Settings → About device), and we'll add it to the table above.
- **It doesn't work?** Open an issue with your model, firmware, what you saw, and if you can, the
  output of `adb shell settings get global power_dw_allow_setting_pkg`. Logs from
  `adb logcat -d | grep -E "DoubleWake|heytap.wallet"` right after a double press help a lot.
- **You found a fix?** Pull requests are very welcome. Please say which phone and firmware you
  tested it on.

Details and the few rules for pull requests are in [CONTRIBUTING.md](CONTRIBUTING.md).

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

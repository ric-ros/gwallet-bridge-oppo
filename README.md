# gWallet Bridge for OPPO

Open **Google Wallet** with a **double press of the power button** on OPPO phones (ColorOS).
No root, no special permissions, no computer needed to use the app.

ColorOS only offers a few firmware-approved apps for this gesture. gWallet Bridge fills one
of those slots and opens Google Wallet after normal system authentication.

**New: a FinEasy-slot variant leaves OPPO Wallet's package name free.** On the tested Find N6,
it opens Wallet smoothly and lets OPPO Access badge show its own screen again. It uses the
native gesture: KeySnap, accessibility services and wireless debugging are not needed.

> Independent project, not affiliated with OPPO, ColorOS, FinEasy or Google.
>
> Version **1.1.0** introduces both variants. See the [changelog](CHANGELOG.md) for the changes
> and migration steps. Releases through **1.0.1** contain only the original OPPO Wallet-slot APK.

## Choose a variant

Both variants share the same activity and request no permissions.

| Variant | Package | When to use it |
| --- | --- | --- |
| **FinEasy slot** (preferred where available) | `com.fintech.life` | Leaves OPPO Wallet's package free. Access badge UI was verified after removing the old bridge. Cannot coexist with the real FinEasy app. |
| **OPPO Wallet slot** (legacy fallback) | `com.heytap.wallet` | For phones without the FinEasy entry. Conflicts with native OPPO Wallet and redirects Access badge to Google Wallet on the tested phone. |

FinEasy is a regional app whose package has its own firmware shortcut slot. Our APK contains
**this project's code**, not the real FinEasy app. That package name must be unused on your phone.
The inspected ColorOS 17 settings code hides this entry in region **TH (Thailand)**. The new
variant was tested with the phone's current region **NZ**. Other configurations are untested;
we do not recommend changing region to use it.

## Install

You need an OPPO phone with ColorOS, Android 12 or newer, and Google Wallet installed.

1. Download the APK for your chosen slot from the
   [releases page](https://github.com/ric-ros/gwallet-bridge-oppo/releases), or build it below:
   - **FinEasy slot:** `gwallet-bridge-1.1.0-fineasy.apk`.
   - **Legacy Wallet slot:** `gwallet-bridge-1.1.0-oppo-wallet.apk`.
   Older releases through 1.0.1 provide only the legacy APK, named `gwallet-bridge-<version>.apk`.
2. Open the APK and install it. Allow your browser or file manager to install unknown apps if
   asked. ColorOS may warn that it is not from its store.
3. Open **Settings > Accessibility & convenience > Power button > Double-press Power button**.
   If your menus differ, search Settings for "power button".
4. Enable the gesture and select the entry for your variant:
   - **FinEasy slot:** look for **gWallet Bridge** or a firmware-provided **FinEasy** label.
   - **Legacy Wallet slot:** select **OPPO Wallet**. In current source builds its launcher icon
     is labelled **gWallet Bridge (Wallet slot)** to distinguish the installations.
5. Test unlocked, then locked. Authenticate normally if asked; Google Wallet should then open.

If the desired entry does not appear, that variant is unavailable on your configuration.
Try the other variant subject to the conflicts above, or uninstall.

To pay contactless you do not need to open Wallet: unlock the phone and hold it near the terminal.
The shortcut is useful for passes, tickets and loyalty cards.

### Moving from the old bridge to the FinEasy slot

This is a **separate installation**, not an update to the old package.

1. Install the FinEasy-slot APK and select its entry in the native double-press settings.
2. Confirm that it opens Google Wallet, including from a locked phone.
3. Uninstall the old **gWallet Bridge** / **gWallet Bridge (Wallet slot)** (`com.heytap.wallet`).
   Removing it is necessary to stop its Access badge interception.
4. Open **Access badge** from Settings and check that its own screen appears.

Neither bridge stores your payment cards, and removing one does not uninstall Google Wallet.
This migration frees OPPO Wallet's package name; it does not install the full native OPPO
Wallet app or demonstrate that a badge works at a physical reader.

If you configured KeySnap for the same gesture, disable that shortcut to avoid competing launches.
KeySnap is not part of this solution.

### Uninstall

Long-press the appropriate bridge icon and uninstall it. Then choose another double-press action
in Settings. The bridge requests no permissions and runs no background service.

## How it works

On the tested phone, ColorOS's native allowlist includes both package names:

```text
adb shell settings get global power_dw_allow_setting_pkg
com.fintech.life+com.oplus.camera+com.oplus.accesscard+com.heytap.wallet
```

Each build receives the action expected for its slot:

| Gradle flavor | Package | Native action |
| --- | --- | --- |
| `fineasy` | `com.fintech.life` | `fintech.life.intent.action.OPEN` |
| `oppoWallet` | `com.heytap.wallet` | `heytap.wallet.intent.action.OPEN` |

ColorOS handles the power gesture itself and starts the bridge's single activity. This avoids
competing with normal power-button behavior through a separate key listener.

- **Unlocked:** opens Google Wallet's main screen (cards and passes), with the payment-only
  `QUICKDRAW` screen as a fallback.
- **Locked:** calls `requestDismissKeyguard` once the activity has window focus. The system
  authenticates you if necessary, then the bridge opens Wallet. It never bypasses authentication.
- The activity checks whether the lock screen is **showing**: face recognition can unlock the
  device while ColorOS still waits for a swipe. It requests dismissal of that screen too.

The new variant does not change this unlock logic. It changes only the package and action
that ColorOS launches. No internet, accessibility service, background service, Shizuku,
KeySnap or wireless debugging is needed. See
[MainActivity.java](app/src/main/java/io/github/ricros/gwalletbridge/MainActivity.java).

### Why keep the legacy variant?

The FinEasy entry may be missing, hidden by regional configuration, or occupied by the real app.
The original variant remains a fallback for those phones.

On the tested phone, Access badge hands its "use card" screen to `com.heytap.wallet` whenever
that package is installed. The legacy bridge therefore intercepts it. Using the new variant
and removing the old bridge restored the normal Access badge screen. Actual badge operation
at a reader has not been tested.

These package names belong to other apps and are required by this firmware workaround. The
bridge is distributed as a side-loaded APK, not as an ordinary Google Play app.

### What does not work on the tested build

- Setting an arbitrary target with `settings put secure double_tap_power_button_gesture` or
  `settings put global power_dw_quick_info`: ignored by the native controller.
- Writing the real target provider from ADB: requires `com.oplus.permission.safe.SETTINGS`.
- Using the firmware's own "default wallet" entry: gated on RSA5; this build reports
  `ro.oplus.rsa=rsa4-1-tier3`.

## Tested on

| Phone | Firmware / configuration | Variant and result |
| --- | --- | --- |
| OPPO Find N6 (`CPH2765`) | Android 17, ColorOS 17, `CPH2765_17.0.0.101(EX01)`, current region NZ | **FinEasy slot:** native double press works; owner confirmed smooth fingerprint unlock into Wallet and normal Access badge UI after removing the old bridge (2026-11-16). |
| OPPO Find N6 (`CPH2765`) | Same firmware | **Legacy Wallet slot:** locked and unlocked; fingerprint and face unlock verified. Access badge redirected to Google Wallet. |
| OPPO Find X9 Pro | ColorOS 17 | **Legacy Wallet slot only:** [user report](https://www.reddit.com/r/Oppo/comments/1xr90bv/comment/19scfucneya5c/); lock screen not reported. |

FinEasy-slot testing used the same activity in a local debug build. Other phones/regions,
reboot behavior, face-only unlock with the new slot, and badge-reader operation are untested.
Reports for the original variant do not establish compatibility with the new one.

## Help and contributions

Report working phones in [Tested phones](https://github.com/ric-ros/gwallet-bridge-oppo/discussions/1).
Include model, firmware, region, **which variant**, the entry label in Settings, and locked/unlocked
results. For failures, open an issue with the same details. ADB logs can help:

```sh
adb shell settings get global power_dw_allow_setting_pkg
adb logcat -d | grep -E "DoubleWake|heytap.wallet|fintech.life"
```

Remove personal information before posting logs. See [CONTRIBUTING.md](CONTRIBUTING.md).

### Check with ADB

```sh
# FinEasy slot
adb install -r app/build/outputs/apk/fineasy/debug/app-fineasy-debug.apk
adb shell am start -a fintech.life.intent.action.OPEN -p com.fintech.life

# Legacy Wallet slot (do not install alongside native OPPO Wallet)
adb install -r app/build/outputs/apk/oppoWallet/debug/app-oppoWallet-debug.apk
adb shell am start -a heytap.wallet.intent.action.OPEN -p com.heytap.wallet

adb shell "dumpsys activity activities | grep topResumedActivity"
adb logcat -d | grep GWalletBridge
```

An intent launch only checks forwarding. Test the real button and authentication on the phone too.

## Build from source

Needs JDK 21 and Android SDK platform 36. The included wrapper downloads Gradle 9.9.0.

```sh
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew assembleFineasyDebug assembleOppoWalletDebug
# Or assembleDebug to build both flavors.
```

Outputs:

- `app/build/outputs/apk/fineasy/debug/app-fineasy-debug.apk`
- `app/build/outputs/apk/oppoWallet/debug/app-oppoWallet-debug.apk`

Windows PowerShell: use `.\gradlew.bat` instead of `./gradlew`.

Release signing is maintainer-controlled. Put signing properties in `~/.gradle/gradle.properties`,
never the repository:

```properties
GWB_STORE_FILE=/path/to/your.jks
GWB_STORE_PASSWORD=...
GWB_KEY_ALIAS=...
GWB_KEY_PASSWORD=...
```

The release tasks are `assembleFineasyRelease` and `assembleOppoWalletRelease`.
A different signing key cannot update an installed APK of the same package; uninstalling first
removes that app's data. Different slot variants remain separate packages.

Official releases are signed with this certificate (SHA-256):

```text
f3:48:25:c5:6c:4e:eb:e3:e0:98:30:a8:09:cb:39:0b:81:4d:d1:21:4a:3a:ad:f2:3a:7d:ee:38:0f:66:84:aa
```

Check an official download with `apksigner verify --print-certs <downloaded.apk>`. Local debug
builds, including the phone trial, use a different certificate.

## Credits and license

MIT, see [LICENSE](LICENSE). Based on
[Nielk74/coloros-power-button-launcher](https://github.com/Nielk74/coloros-power-button-launcher).
The project retains only the activity; no accessibility service, boot receiver or
`WRITE_SECURE_SETTINGS` permission is needed for this native shortcut approach.

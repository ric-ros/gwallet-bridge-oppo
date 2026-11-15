# Changelog

## 1.1.0 - 2026-11-16

### New: FinEasy shortcut slot

- Added a **FinEasy-slot variant** using `com.fintech.life` and the native
  `fintech.life.intent.action.OPEN` action. ColorOS handles the double press itself;
  no KeySnap, accessibility service, wireless debugging or background service is needed.
- This variant leaves `com.heytap.wallet` free for OPPO Wallet. On the tested Find N6,
  removing the old bridge also restored the normal **Access badge** screen.
- The app is named **gWallet Bridge**. The development label "Wallet Shortcut Test"
  is no longer used.

### Compatibility

- Retained the original **OPPO Wallet-slot variant** as a fallback. Its launcher label
  is now **gWallet Bridge (Wallet slot)** to distinguish the two installations.
- Both variants share the existing unlock-and-launch activity. There are no new
  permissions, dependencies, services or changes to device authentication.
- FinEasy-slot availability depends on firmware and region. The inspected firmware
  hides it in Thailand (TH), and it cannot coexist with the real FinEasy app.
- The legacy variant still occupies OPPO Wallet's package and has the documented
  Access badge redirection on the tested phone.

### Migration from 1.0.1

The FinEasy variant is a **different Android package**, not an in-place update of the
old bridge. Install it, select its native double-press entry, and test it first. Then
uninstall the old `com.heytap.wallet` bridge to remove its Access badge interception.
Google Wallet and its cards are unaffected. This does not install native OPPO Wallet.

The OPPO Wallet-slot APK remains an in-place update for existing official installations
of that variant when signed with the same release certificate.

### Documentation and builds

- Added separate `fineasy` and `oppoWallet` Gradle flavors and CI builds for both.
- Updated the README and contributor instructions with variant selection, migration,
  package conflicts, regional limits and variant-specific test reports.
- Release APK names identify the slot: `gwallet-bridge-1.1.0-fineasy.apk` and
  `gwallet-bridge-1.1.0-oppo-wallet.apk`.

### Verified and untested

- **OPPO Find N6 (CPH2765), Android 17 / ColorOS 17,
  CPH2765_17.0.0.101(EX01), current region NZ:** owner confirmed native double press,
  smooth fingerprint unlock into Google Wallet, and normal Access badge UI after
  removing the old bridge. Initial validation used a local debug build of the same activity.
- Other phones and regions, a reboot with the FinEasy variant, face-only unlock with
  that variant, and badge operation at a physical reader remain untested.
- Previous Find X9 Pro reports apply to the legacy Wallet-slot variant only.

## 1.0.1

- Original OPPO Wallet-slot release (`com.heytap.wallet`).
- Uses the system lock screen to authenticate before opening Google Wallet, including
  dismissal of the swipe screen after face recognition.

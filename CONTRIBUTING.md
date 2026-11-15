# Contributing

Thanks for helping! Reports from other phones are the most useful thing right now.

## Reporting a phone

If it works, leave a comment in
[Tested phones](https://github.com/ric-ros/gwallet-bridge-oppo/discussions/1) with your model,
firmware, region, variant (FinEasy slot or OPPO Wallet slot), and whether it works locked and unlocked.

If it doesn't work, open an issue with:

- phone model and firmware (Settings → About device);
- which variant you installed and which entry appears in **Double-press Power button**;
- what happened when the phone was unlocked, and when it was locked;
- if you can, the output of these commands, run right after a double press:

  ```sh
  adb shell settings get global power_dw_allow_setting_pkg
  adb logcat -d | grep -E "DoubleWake|heytap.wallet|fintech.life"
  ```

  Check the log before posting and remove anything personal.

## Pull requests

This app runs over the lock screen and uses firmware-approved package names, so it has to stay small and
easy to trust. A pull request is welcome when it:

- adds **no permissions** (in particular no internet), no dependencies, no analytics and no
  background services;
- preserves both flavor package/action pairs: `fineasy` uses `com.fintech.life` /
  `fintech.life.intent.action.OPEN`; `oppoWallet` uses `com.heytap.wallet` /
  `heytap.wallet.intent.action.OPEN`;
- never bypasses the lock screen;
- says which phone, firmware, region and variant you tested, and how: unlocked, locked with fingerprint or
  PIN, and locked with face unlock if your phone has it;
- builds with `./gradlew assembleFineasyDebug assembleOppoWalletDebug` (GitHub builds both).

Please don't change `gradle/wrapper/gradle-wrapper.jar` or the Gradle download URL unless the pull
request is about updating Gradle. If it is, update `distributionSha256Sum` in
`gradle/wrapper/gradle-wrapper.properties` to the official checksum.

Never commit keystores, passwords or `local.properties`.

## Releases

Release APKs are built and signed locally by the maintainer or at the maintainer's explicit
request. Merged changes ship in the next release. Record user-visible changes and migration
steps in [CHANGELOG.md](CHANGELOG.md) before building a new version.

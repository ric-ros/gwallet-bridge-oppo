package io.github.ricros.gwalletbridge;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Toast;

/**
 * Minimal bridge for ColorOS firmware that hard-codes {@code com.heytap.wallet}
 * as its double-power-button Wallet target.
 */
public final class MainActivity extends Activity {
    private static final String TAG = "GWalletBridge";
    private static final String GOOGLE_WALLET_PACKAGE =
            "com.google.android.apps.walletnfcrel";
    private static final String GOOGLE_WALLET_QUICKDRAW_ACTION =
            "com.google.android.apps.wallet.main.QUICKDRAW";
    private static final int UNLOCK_POLL_ATTEMPTS = 25;
    private static final long UNLOCK_POLL_MS = 100;

    private CancellationSignal cancellationSignal;
    private boolean authenticationStarted;
    private boolean launchStarted;
    private boolean promptPending;
    private boolean promptScheduled;
    private boolean dismissRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);

        Log.i(TAG, "Received ColorOS wallet launch: action=" + getIntent().getAction());
        if (savedInstanceState == null) {
            openWalletRespectingDeviceLock();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.i(TAG, "Received another ColorOS wallet launch: action=" + intent.getAction());
        if (!authenticationStarted && !launchStarted) {
            openWalletRespectingDeviceLock();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // On the lock screen the system cancels a BiometricPrompt that was created before
        // this activity actually owns the window ("client finished without start"), so the
        // prompt only starts after the first focus event.
        if (hasFocus && promptPending && !promptScheduled) {
            promptScheduled = true;
            Log.i(TAG, "Window focused; starting authentication");
            getWindow().getDecorView().postDelayed(this::authenticateThenOpenWallet, 200L);
        }
    }

    private void openWalletRespectingDeviceLock() {
        if (isDeviceUnlocked()) {
            Log.i(TAG, "Device is already unlocked; opening Google Wallet directly");
            openGoogleWallet();
            return;
        }
        promptPending = true;
        Log.i(TAG, "Device is locked; waiting for window focus before authenticating");
    }

    private void authenticateThenOpenWallet() {
        if (authenticationStarted || launchStarted || isFinishing()) {
            return;
        }
        if (isDeviceUnlocked()) {
            Log.i(TAG, "Keyguard is gone before prompting; opening Google Wallet");
            openGoogleWallet();
            return;
        }
        authenticationStarted = true;

        try {
            BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                    .setTitle(getString(R.string.authentication_title))
                    .setDescription(getString(R.string.authentication_description))
                    .setConfirmationRequired(false)
                    .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
            cancellationSignal = new CancellationSignal();
            prompt.authenticate(
                    cancellationSignal,
                    getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                BiometricPrompt.AuthenticationResult result) {
                            authenticationStarted = false;
                            Log.i(TAG, "Authentication succeeded; waiting for the keyguard");
                            openWalletAfterUnlock(0);
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString) {
                            authenticationStarted = false;
                            boolean unlocked = isDeviceUnlocked();
                            Log.i(TAG, "Authentication ended: code=" + errorCode
                                    + " deviceUnlocked=" + unlocked);
                            if (unlocked) {
                                openGoogleWallet();
                            } else if (errorCode == 5) { // BiometricPrompt.ERROR_CANCELED
                                // System-side cancellation (typically the lock screen taking
                                // the sensor): let the system run its own unlock UI instead.
                                requestKeyguardDismissal();
                            } else {
                                finishAndRemoveTask();
                            }
                        }
                    });
        } catch (RuntimeException exception) {
            authenticationStarted = false;
            Log.e(TAG, "Unable to start system authentication", exception);
            Toast.makeText(
                            this,
                            R.string.authentication_unavailable,
                            Toast.LENGTH_LONG)
                    .show();
            finishAndRemoveTask();
        }
    }

    /**
     * BiometricPrompt reports success slightly before the keyguard is actually gone, and an
     * activity that is not showWhenLocked cannot be launched while the keyguard is up.
     */
    private void openWalletAfterUnlock(int attempt) {
        if (launchStarted) {
            return;
        }
        if (isDeviceUnlocked()) {
            Log.i(TAG, "Unlocked after attempt " + attempt + "; opening Google Wallet");
            openGoogleWallet();
            return;
        }
        if (attempt >= UNLOCK_POLL_ATTEMPTS) {
            Log.w(TAG, "Keyguard still up after " + attempt + " polls; asking the system");
            requestKeyguardDismissal();
            return;
        }
        getWindow().getDecorView().postDelayed(() -> openWalletAfterUnlock(attempt + 1), UNLOCK_POLL_MS);
    }

    private void requestKeyguardDismissal() {
        if (dismissRequested || launchStarted || isFinishing()) {
            finishAndRemoveTask();
            return;
        }
        dismissRequested = true;
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        if (keyguardManager == null) {
            Log.e(TAG, "KeyguardManager unavailable");
            finishAndRemoveTask();
            return;
        }
        Log.i(TAG, "Asking the system to dismiss the keyguard");
        keyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissSucceeded() {
                Log.i(TAG, "Keyguard dismissed by the system");
                openWalletAfterUnlock(0);
            }

            @Override
            public void onDismissCancelled() {
                Log.i(TAG, "Keyguard dismissal cancelled by the user");
                finishAndRemoveTask();
            }

            @Override
            public void onDismissError() {
                Log.w(TAG, "Keyguard dismissal failed");
                finishAndRemoveTask();
            }
        });
    }

    private boolean isDeviceUnlocked() {
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        return keyguardManager == null || !keyguardManager.isDeviceLocked();
    }

    private void openGoogleWallet() {
        if (launchStarted || isFinishing()) {
            return;
        }
        launchStarted = true;

        // Open the full Wallet app (cards, passes, loyalty) rather than the payment-only
        // QUICKDRAW presentation mode. QUICKDRAW stays as a fallback.
        Intent launcherIntent = getPackageManager()
                .getLaunchIntentForPackage(GOOGLE_WALLET_PACKAGE);
        if (launcherIntent == null) {
            Log.e(TAG, "Google Wallet is not installed");
            Toast.makeText(this, R.string.wallet_not_installed, Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
            return;
        }

        try {
            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launcherIntent);
            Log.i(TAG, "Google Wallet main activity started");
            finishAndRemoveTask();
            return;
        } catch (ActivityNotFoundException | SecurityException exception) {
            Log.w(TAG, "Google Wallet main activity rejected; trying QUICKDRAW", exception);
        }

        Intent quickDrawIntent = new Intent(GOOGLE_WALLET_QUICKDRAW_ACTION)
                .setPackage(GOOGLE_WALLET_PACKAGE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(quickDrawIntent);
            Log.i(TAG, "Google Wallet QUICKDRAW activity started");
        } catch (ActivityNotFoundException | SecurityException exception) {
            Log.e(TAG, "Unable to open Google Wallet", exception);
            Toast.makeText(this, R.string.wallet_launch_failed, Toast.LENGTH_LONG).show();
        }
        finishAndRemoveTask();
    }

    @Override
    protected void onDestroy() {
        if (cancellationSignal != null && !cancellationSignal.isCanceled()) {
            cancellationSignal.cancel();
        }
        cancellationSignal = null;
        super.onDestroy();
    }
}

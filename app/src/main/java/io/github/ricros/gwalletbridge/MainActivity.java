package io.github.ricros.gwalletbridge;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Started by ColorOS on a power-button double press (it launches {@code com.heytap.wallet}).
 * Opens Google Wallet, first asking the system to remove the lock screen when it is showing.
 */
public final class MainActivity extends Activity {
    private static final String TAG = "GWalletBridge";
    private static final String GOOGLE_WALLET_PACKAGE =
            "com.google.android.apps.walletnfcrel";
    private static final String GOOGLE_WALLET_QUICKDRAW_ACTION =
            "com.google.android.apps.wallet.main.QUICKDRAW";
    private static final int KEYGUARD_POLL_ATTEMPTS = 25;
    private static final long KEYGUARD_POLL_MS = 100;

    private boolean dismissPending;
    private boolean dismissRequested;
    private boolean launchStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);

        Log.i(TAG, "Received ColorOS wallet launch: action=" + getIntent().getAction());
        if (savedInstanceState == null) {
            start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.i(TAG, "Received another ColorOS wallet launch: action=" + intent.getAction());
        if (!dismissRequested && !launchStarted) {
            start();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Like a BiometricPrompt, the dismiss request is only reliable once this activity
        // owns the window on top of the lock screen.
        if (hasFocus && dismissPending && !dismissRequested) {
            if (isKeyguardShowing()) {
                requestKeyguardDismissal();
            } else {
                // Unlocked meanwhile (e.g. a fingerprint unlock already in progress).
                openGoogleWallet();
            }
        }
    }

    private void start() {
        // Check the lock screen itself, not isDeviceLocked(): after face unlock the device is
        // unlocked but ColorOS keeps the lock screen up until a swipe, and Wallet (not
        // showWhenLocked) would open hidden behind it.
        if (!isKeyguardShowing()) {
            openGoogleWallet();
            return;
        }
        dismissPending = true;
        Log.i(TAG, "Lock screen is showing; waiting for window focus to dismiss it");
    }

    /**
     * The system removes the lock screen right away when the device is already unlocked (face
     * unlock, trusted state); otherwise it shows its own fingerprint/PIN/pattern screen once.
     */
    private void requestKeyguardDismissal() {
        dismissRequested = true;
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        if (keyguardManager == null) {
            Log.e(TAG, "KeyguardManager unavailable");
            finishAndRemoveTask();
            return;
        }
        Log.i(TAG, "Asking the system to dismiss the lock screen");
        keyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissSucceeded() {
                Log.i(TAG, "Lock screen dismissed");
                openWhenKeyguardGone(0);
            }

            @Override
            public void onDismissCancelled() {
                Log.i(TAG, "Unlock cancelled by the user");
                finishAndRemoveTask();
            }

            @Override
            public void onDismissError() {
                Log.w(TAG, "Lock screen dismissal failed");
                finishAndRemoveTask();
            }
        });
    }

    /** The success callback can arrive slightly before the lock screen is actually gone. */
    private void openWhenKeyguardGone(int attempt) {
        if (launchStarted || isFinishing()) {
            return;
        }
        if (!isKeyguardShowing() || attempt >= KEYGUARD_POLL_ATTEMPTS) {
            Log.i(TAG, "Opening Google Wallet after " + attempt + " checks");
            openGoogleWallet();
            return;
        }
        getWindow().getDecorView()
                .postDelayed(() -> openWhenKeyguardGone(attempt + 1), KEYGUARD_POLL_MS);
    }

    private boolean isKeyguardShowing() {
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        return keyguardManager != null && keyguardManager.isKeyguardLocked();
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
}

package com.oplus.settings.privacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PrivacyPasswordActivity extends Activity {
    static final String ACTION_SETTINGS = "oplus.intent.action.settings.PRIVACY_PWD_SETTINGS";
    static final String ACTION_CHOOSE = "oplus.intent.action.settings.PRIVACY_PWD_CHOOSE";
    static final String ACTION_CONFIRM = "oplus.intent.action.settings.PRIVACY_PWD_CONFIRM";

    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final String FINGERPRINT_FILE_ENCRYPTION_SWITCH =
            "oplus_customize_fingerprint_file_encryption_switch";
    private static final String FINGERPRINT_FOR_PRIVACY = "fingerprint_for_privacy";

    private CancellationSignal mBiometricCancellationSignal;
    private boolean mConfirmFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent() != null ? getIntent().getAction() : null;
        if (ACTION_CONFIRM.equals(action)) {
            showConfirmPasswordDialog();
        } else if (ACTION_CHOOSE.equals(action)) {
            showSetPasswordDialog(false);
        } else {
            showSettingsDialog();
        }
    }

    private void showSettingsDialog() {
        if (!PrivacyPasswordState.isPasswordSet(this)) {
            showSetPasswordDialog(false);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Privacy password")
                .setItems(new CharSequence[] {"Change password", "Turn off privacy password"},
                        (dialog, which) -> {
                            if (which == 0) {
                                showConfirmThenSetDialog();
                            } else {
                                showConfirmThenClearDialog();
                            }
                        })
                .setOnCancelListener(dialog -> finishCanceled())
                .show();
    }

    private void showConfirmThenSetDialog() {
        showPasswordInputDialog("Enter privacy password", "Continue", password -> {
            if (!PrivacyPasswordState.checkPassword(this, password)) {
                showErrorAndFinish("Incorrect password");
                return;
            }
            showSetPasswordDialog(true);
        });
    }

    private void showConfirmThenClearDialog() {
        showPasswordInputDialog("Enter privacy password", "Turn off", password -> {
            if (!PrivacyPasswordState.checkPassword(this, password)) {
                showErrorAndFinish("Incorrect password");
                return;
            }
            PrivacyPasswordState.clearPassword(this);
            setResult(RESULT_OK);
            finish();
        });
    }

    private void showSetPasswordDialog(boolean changingPassword) {
        LinearLayout layout = createInputLayout();
        EditText password = createPasswordInput("New privacy password");
        EditText confirm = createPasswordInput("Confirm privacy password");
        layout.addView(password);
        layout.addView(confirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(changingPassword ? "Change privacy password" : "Set privacy password")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, (d, which) -> finishCanceled())
                .setPositiveButton(android.R.string.ok, null)
                .setOnCancelListener(d -> finishCanceled())
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String first = password.getText().toString();
            String second = confirm.getText().toString();
            if (first.length() < MIN_PASSWORD_LENGTH) {
                showFieldError(password, "Use at least 4 characters");
                return;
            }
            if (!TextUtils.equals(first, second)) {
                showFieldError(confirm, "Passwords do not match");
                return;
            }
            if (!PrivacyPasswordState.setPassword(this, first)) {
                showErrorAndFinish("Could not save privacy password");
                return;
            }
            setResult(RESULT_OK, new Intent());
            finish();
        }));
        dialog.show();
    }

    private void showConfirmPasswordDialog() {
        if (!PrivacyPasswordState.isPasswordSet(this)) {
            showSetPasswordDialog(false);
            return;
        }

        if (shouldOfferFileEncryptionFingerprint()) {
            showFingerprintPrompt();
            return;
        }

        showPasswordConfirmFallback();
    }

    private boolean shouldOfferFileEncryptionFingerprint() {
        if (Settings.Secure.getInt(getContentResolver(), FINGERPRINT_FILE_ENCRYPTION_SWITCH, 0) != 1) {
            return false;
        }
        String privacyFingerprints = Settings.Secure.getString(getContentResolver(), FINGERPRINT_FOR_PRIVACY);
        if (TextUtils.isEmpty(privacyFingerprints)) {
            return false;
        }
        BiometricManager biometricManager = getSystemService(BiometricManager.class);
        return biometricManager != null
                && biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showFingerprintPrompt() {
        mBiometricCancellationSignal = new CancellationSignal();
        Handler handler = new Handler(Looper.getMainLooper());
        BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                .setTitle("Privacy password")
                .setSubtitle("Unlock Private Safe")
                .setNegativeButton("Use password", getMainExecutor(), (dialog, which) -> {
                    cancelFingerprintPrompt();
                    showPasswordConfirmFallback();
                })
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
        prompt.authenticate(mBiometricCancellationSignal, runnable -> handler.post(runnable),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        finishConfirmed();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        if (mConfirmFinished) {
                            return;
                        }
                        showPasswordConfirmFallback();
                    }
                });
    }

    private void showPasswordConfirmFallback() {
        showPasswordInputDialog("Enter privacy password", "OK", password -> {
            if (PrivacyPasswordState.checkPassword(this, password)) {
                finishConfirmed();
            } else {
                showErrorAndFinish("Incorrect password");
            }
        });
    }

    private void finishConfirmed() {
        mConfirmFinished = true;
        cancelFingerprintPrompt();
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private void cancelFingerprintPrompt() {
        if (mBiometricCancellationSignal != null && !mBiometricCancellationSignal.isCanceled()) {
            mBiometricCancellationSignal.cancel();
        }
        mBiometricCancellationSignal = null;
    }

    private void showPasswordInputDialog(String title, String positiveText, PasswordCallback callback) {
        LinearLayout layout = createInputLayout();
        EditText input = createPasswordInput("Privacy password");
        layout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, (d, which) -> finishCanceled())
                .setPositiveButton(positiveText, null)
                .setOnCancelListener(d -> finishCanceled())
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = input.getText().toString();
            if (TextUtils.isEmpty(password)) {
                showFieldError(input, "Enter privacy password");
                return;
            }
            callback.onPassword(password);
        }));
        dialog.show();
    }

    private LinearLayout createInputLayout() {
        LinearLayout layout = new LinearLayout(this);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, padding / 2, padding, 0);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    private EditText createPasswordInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private void showFieldError(TextView view, String message) {
        view.setError(message);
        view.requestFocus();
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finishCanceled();
    }

    private void finishCanceled() {
        mConfirmFinished = true;
        cancelFingerprintPrompt();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onDestroy() {
        cancelFingerprintPrompt();
        super.onDestroy();
    }

    private interface PasswordCallback {
        void onPassword(String password);
    }
}

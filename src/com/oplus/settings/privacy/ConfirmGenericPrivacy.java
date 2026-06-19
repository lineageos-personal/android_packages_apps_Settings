package com.oplus.settings.privacy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ConfirmGenericPrivacy extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, PrivacyPasswordActivity.class);
        intent.setAction(PrivacyPasswordActivity.ACTION_CONFIRM);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}

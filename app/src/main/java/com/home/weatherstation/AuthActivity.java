package com.home.weatherstation;

/**
 * Created by thaarres on 19/06/16.
 */

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AuthActivity extends Activity {

    private static final String TAG = AuthActivity.class.getSimpleName();

    private static final int ACCOUNT_CODE = 1601;
    private Authenticator authenticator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authenticator = new Authenticator(this);
        authenticator.invalidateToken();
        if (!authenticator.hasUser()) {
            chooseAccount();
        } else {
            returnAndFinish();
        }
    }

    private void returnAndFinish() {
        setResult(RESULT_OK);
        finish();
    }

    private void chooseAccount() {
        // use https://github.com/frakbot/Android-AccountChooser for compatibility with older devices
        Intent intent = AccountManager.newChooseAccountIntent(null, null,
                new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, ACCOUNT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == ACCOUNT_CODE) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                authenticator.setUser(accountName);

                // invalidate old tokens which might be cached. we want a fresh
                // one, which is guaranteed to work
                authenticator.invalidateToken();

                authenticator.requestToken(new AuthenticatorCallback() {
                    @Override
                    public void doCoolAuthenticatedStuff() {
                        returnAndFinish();
                    }

                    @Override
                    public void failed() {
                        Log.e(TAG, "Failed to get token.");
                    }
                });
            }
        }
    }

}
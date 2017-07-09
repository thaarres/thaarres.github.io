package com.home.weatherstation;

/**
 * Created by thaarres on 19/06/16.
 */

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class AuthActivity extends Activity {

    private static final String TAG = AuthActivity.class.getSimpleName();

    private static final int ACCOUNT_CODE = 1601;
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private Authenticator authenticator;
    private AndroidPermissions mPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissions = new AndroidPermissions(this,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.GET_ACCOUNTS);

        ensurePermissions();
    }

    private void ensurePermissions() {
        if (mPermissions.checkPermissions()) {
            doStart();
        } else {
            Log.d(TAG, "Some needed permissions are missing. Requesting them.");
            mPermissions.requestPermissions(PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult");

        if (mPermissions.areAllRequiredPermissionsGranted(permissions, grantResults)) {
            doStart();
        } else {
            showError();
        }
    }

    private void doStart() {
        authenticator = new Authenticator(this);
        authenticator.invalidateToken();
        if (!authenticator.hasUser()) {
            chooseAccount();
        } else {
            returnAndFinish();
        }
    }

    private void showError() {
        Toast.makeText(this, "Can not start: Insufficient Permissions", Toast.LENGTH_SHORT).show();
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
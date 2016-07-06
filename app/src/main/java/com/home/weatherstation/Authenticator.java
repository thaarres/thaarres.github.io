package com.home.weatherstation;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by dominic on 05/07/16.
 */
public class Authenticator {

    private static final String TAG = Authenticator.class.getSimpleName();

    // change SCOPE depending on the scope needed for the things you do after you have a token
    private static final String SCOPE = "https://www.googleapis.com/auth/fusiontables";

    private final AuthPreferences authPreferences;
    private final AccountManager accountManager;

    public Authenticator(final Context context) {
        accountManager = AccountManager.get(context);
        authPreferences = new AuthPreferences(context);
    }

    public boolean hasUser() {
        return authPreferences.getUser() != null;
    }


    public void setUser(String user) {
        authPreferences.setUser(user);
    }

    /**
     * call this method if your token expired, or you want to request a new
     * token for whatever reason. call requestToken() again afterwards in order
     * to get a new token.
     */
    public void invalidateToken() {
        Log.i(TAG, "Invalidating token...");
        if (authPreferences.getToken() != null) {
            accountManager.invalidateAuthToken("com.google",
                    authPreferences.getToken());

            authPreferences.setToken(null);
        }
    }

    public void requestToken(AuthenticatorCallback callback) {
        Log.i(TAG, "Requesting new token...");
        Account userAccount = null;
        String user = authPreferences.getUser();
        for (Account account : accountManager.getAccountsByType("com.google")) {
            if (account.name.equals(user)) {
                userAccount = account;

                break;
            }
        }

        accountManager.getAuthToken(userAccount, "oauth2:" + SCOPE, null, true,
                new OnTokenAcquired(callback), null);
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {

        private final String tag = OnTokenAcquired.class.getSimpleName();
        private final AuthenticatorCallback callback;

        public OnTokenAcquired(AuthenticatorCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();
                String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                authPreferences.setToken(token);
                Log.i(TAG, "New token acquired.");
                callback.doCoolAuthenticatedStuff();
            } catch (Exception e) {
                Log.e(tag, "Failed to acquire new token!", e);
                callback.failed();
            }
        }
    }
}

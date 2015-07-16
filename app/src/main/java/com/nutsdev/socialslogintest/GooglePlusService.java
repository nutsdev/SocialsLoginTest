package com.nutsdev.socialslogintest;


import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton for Google+ login
 */
public class GooglePlusService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static volatile GooglePlusService instance;

    private final List<GooglePlusCallback> callbacks;

    private GoogleApiClient googleApiClient;

    private Context context;


    public static GooglePlusService getInstance(Context context) {
        GooglePlusService localInstance = instance;
        if (localInstance == null) {
            synchronized (GooglePlusService.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new GooglePlusService(context);
                }
            }
        }
        return localInstance;
    }

    private GooglePlusService(Context context) {
        this.context = context;
        callbacks = new ArrayList<>();
        buildGoogleApiClient();
    }


    /* public methods */

    public void register(GooglePlusCallback callback) {
        callbacks.add(callback);
    }

    public void unregister(GooglePlusCallback callback) {
        callbacks.remove(callback);
    }


    /* callbacks */

    /* onConnected is called when our Activity successfully connects to Google Play services.  onConnected indicates that an account was selected on the
     * device, that the selected account has granted any requested permissions to our app and that we were able to establish a service connection to Google Play services. */
    @Override
    public void onConnected(Bundle bundle) {
        L.t(context, "onConnected() called. Sign in successful!");
        for (GooglePlusCallback callback : callbacks) {
            callback.onConnected(bundle);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to attempt to re-establish the connection or get a
        // ConnectionResult that we can attempt to resolve.
        L.d("onConnectionSuspended() called. Trying to reconnect.");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /* onConnectionFailed is called when our Activity could not connect to Google Play services. onConnectionFailed indicates that the user needs to select
         * an account, grant permissions or resolve an error in order to sign in. */
        L.t(context, "Connection failed! " + connectionResult.getErrorCode());
        for (GooglePlusCallback callback : callbacks) {
            callback.onConnectionFailed(connectionResult);
        }
    }


    /* private methods */

    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN);

        return builder.build();
    }


    /* inner classes */

    public interface GooglePlusCallback {
        void onConnected(Bundle connectionHint);
        void onConnectionFailed(ConnectionResult connectionResult);
    }

}

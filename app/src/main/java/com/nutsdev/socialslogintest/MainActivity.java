package com.nutsdev.socialslogintest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;


public class MainActivity extends AppCompatActivity {

    private GoogleApiClient googleApiClient;

    private SignInButton sign_in_button;


    /* lifecycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleApiClient = buildGoogleApiClient();

        sign_in_button = (SignInButton) findViewById(R.id.sign_in_button);
        sign_in_button.setOnClickListener(googleButtonListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    /* private methods */

    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(googlePlusConnectionCallbacks)
                .addOnConnectionFailedListener(googlePlusOnConnectionFailedListener)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN);

        return builder.build();
    }


    /* listeners */

    private View.OnClickListener googleButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "CLICKED", Toast.LENGTH_SHORT).show();
            googleApiClient.connect();
        }
    };

    private GoogleApiClient.ConnectionCallbacks googlePlusConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        /* onConnected is called when our Activity successfully connects to Google
         * Play services.  onConnected indicates that an account was selected on the
         * device, that the selected account has granted any requested permissions to
         * our app and that we were able to establish a service connection to Google
         * Play services.
         */
        @Override
        public void onConnected(Bundle bundle) {
            // Update the user interface to reflect that the user is signed in.
            sign_in_button.setEnabled(false);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // The connection to Google Play services was lost for some reason.
            // We call connect() to attempt to re-establish the connection or get a
            // ConnectionResult that we can attempt to resolve.
            googleApiClient.connect();
        }
    };

    private GoogleApiClient.OnConnectionFailedListener googlePlusOnConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    };

}

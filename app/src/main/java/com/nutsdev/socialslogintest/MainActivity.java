package com.nutsdev.socialslogintest;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;


public class MainActivity extends AppCompatActivity {

    public static final String USER_INFO = "USER_INFO";

    /* Request code used to invoke sign in user interactions. */
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;

    private GoogleApiClient googleApiClient;

    private SignInButton sign_in_button;
    private Button logOut_button;

    /* A flag indicating that a PendingIntent is in progress and prevents us from starting further intents. */
    private boolean intentInProgress;



    /* lifecycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleApiClient = buildGoogleApiClient();
        FacebookSdk.sdkInitialize(getApplicationContext());

        sign_in_button = (SignInButton) findViewById(R.id.sign_in_button);
        sign_in_button.setOnClickListener(googleButtonListener);
        logOut_button = (Button) findViewById(R.id.logOut_button);
        logOut_button.setOnClickListener(logOutButtonListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        logOut_button.setEnabled(googleApiClient.isConnected());
    }

    @Override
    protected void onStart() {
        super.onStart();
    //    googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

    /*    if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        } */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    //    super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            intentInProgress = false;

            if (!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        }
    }

    /* private methods */

    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(googlePlusConnectionCallbacks)
                .addOnConnectionFailedListener(googlePlusOnConnectionFailedListener)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN);

        return builder.build();
    }


    /* listeners */

    private View.OnClickListener googleButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "CLICKED", Toast.LENGTH_SHORT).show();
            sign_in_button.setEnabled(false);
            googleApiClient.connect();
        }
    };

    private View.OnClickListener logOutButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // We clear the default account on sign out so that Google Play services will not return an onConnected callback without user interaction.
            if (googleApiClient.isConnected()) {
                Plus.AccountApi.clearDefaultAccount(googleApiClient);
                googleApiClient.disconnect();
            }

            sign_in_button.setEnabled(true);
            logOut_button.setEnabled(googleApiClient.isConnected());
        }
    };

    private GoogleApiClient.ConnectionCallbacks googlePlusConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        /* onConnected is called when our Activity successfully connects to Google Play services.  onConnected indicates that an account was selected on the
         * device, that the selected account has granted any requested permissions to our app and that we were able to establish a service connection to Google Play services. */
        @Override
        public void onConnected(Bundle bundle) {
            // Update the user interface to reflect that the user is signed in.
            Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
            sign_in_button.setEnabled(false);

            UserInfo userInfo = new UserInfo();
            // Retrieve some profile information to personalize our app for the user.
            Person currentUser = Plus.PeopleApi.getCurrentPerson(googleApiClient);
            userInfo.userEmail = Plus.AccountApi.getAccountName(googleApiClient);
            userInfo.userName = currentUser.getDisplayName();
            userInfo.userAvatarUrl = currentUser.getImage().getUrl();
            userInfo.userProfileUrl = currentUser.getUrl();
            userInfo.userId = currentUser.getId();

            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            intent.putExtra(USER_INFO, userInfo);
            startActivity(intent);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // The connection to Google Play services was lost for some reason. We call connect() to attempt to re-establish the connection or get a
            // ConnectionResult that we can attempt to resolve.
            googleApiClient.connect();
        }
    };

    private GoogleApiClient.OnConnectionFailedListener googlePlusOnConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        /* onConnectionFailed is called when our Activity could not connect to Google Play services. onConnectionFailed indicates that the user needs to select
         * an account, grant permissions or resolve an error in order to sign in. */
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Toast.makeText(getApplicationContext(), "Connection failed! " + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
            if (!intentInProgress && connectionResult.hasResolution()) {
                try {
                    intentInProgress = true;
                    startIntentSenderForResult(connectionResult.getResolution().getIntentSender(), REQUEST_CODE_SIGN_IN, null, 0, 0, 0);
                //    connectionResult.startResolutionForResult(MainActivity.this, REQUEST_CODE_RESOLVE_ERR);
                } catch (IntentSender.SendIntentException e) {
                    // The intent was canceled before it was sent.  Return to the default state and attempt to connect to get an updated ConnectionResult.
                    intentInProgress = true;
                    googleApiClient.connect();
                }
            }
        }
    };

}

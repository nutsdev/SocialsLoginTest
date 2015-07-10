package com.nutsdev.socialslogintest;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    public static final String USER_INFO = "USER_INFO";

    /* Request code used to invoke sign in user interactions. */
    private static final int REQUEST_CODE_SIGN_IN = 555;
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;

    private GoogleApiClient googleApiClient;

    private CallbackManager callbackManager;
    private AccessTokenTracker facebookTokenTracker;
    private ProfileTracker facebookProfileTracker;

    private SignInButton sign_in_button;
    private LoginButton login_button;
    private Button logOut_button;

    /* A flag indicating that a PendingIntent is in progress and prevents us from starting further intents. */
    private boolean intentInProgress;



    /* lifecycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_main);

        googleApiClient = buildGoogleApiClient();

        sign_in_button = (SignInButton) findViewById(R.id.sign_in_button);
        sign_in_button.setOnClickListener(googleButtonListener);
        login_button = (LoginButton) findViewById(R.id.login_button);
        setupFacebook();
        logOut_button = (Button) findViewById(R.id.logOut_button);
        logOut_button.setOnClickListener(logOutButtonListener);
    }

    private void setupFacebook() {
        setupTokenTracker();
        setupProfileTracker();
        facebookTokenTracker.startTracking();
        facebookProfileTracker.startTracking();

        callbackManager = CallbackManager.Factory.create();
        login_button.setReadPermissions("public_profile", "email");
        login_button.registerCallback(callbackManager, loginResultFacebookCallback);
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
    protected void onDestroy() {
        super.onDestroy();
    //    facebookTokenTracker.stopTracking();
    //    facebookProfileTracker.stopTracking();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    //    super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            intentInProgress = false;

            if (!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
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

    private void setupTokenTracker() {
        facebookTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                Log.d("TokenTracker", "" + currentAccessToken);
            }
        };
    }

    private void setupProfileTracker() {
        facebookProfileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                Log.d("ProfileTracker", "" + currentProfile);
            }
        };
    }


    /* listeners */

    private View.OnClickListener googleButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "CLICKED", Toast.LENGTH_SHORT).show();
            if (googleApiClient != null && googleApiClient.isConnected()) { // todo remove?
                Plus.AccountApi.clearDefaultAccount(googleApiClient);
                googleApiClient.disconnect();
            }
        //    sign_in_button.setEnabled(false);
            googleApiClient.connect();
        }
    };

    private FacebookCallback<LoginResult> loginResultFacebookCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            final UserInfo userInfo = new UserInfo();
            Profile profile = Profile.getCurrentProfile();
            if (profile != null) { // todo move to profileTracker?
                userInfo.userName = profile.getName();
                userInfo.userAvatarUrl = profile.getProfilePictureUri(50, 50).toString();
                userInfo.userProfileUrl = profile.getLinkUri().toString();
                userInfo.userId = profile.getId();
            }

            GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            userInfo.userId = object.optString("id");
                            userInfo.userEmail = object.optString("email");
                            userInfo.userName = object.optString("name");
                            JSONObject jsonData;
                            String userAvatarUrl = null;
                            try {
                                jsonData = object.getJSONObject("picture").getJSONObject("data");
                                userAvatarUrl = jsonData.getString("url");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "Error parsing JSON!", Toast.LENGTH_SHORT).show();
                            }
                            userInfo.userAvatarUrl = userAvatarUrl;
                        //    userInfo.userAvatarUrl = String.format("http://graph.facebook.com/%s/picture?type=large", userInfo.userId); // high res avatar picture
                            userInfo.userProfileUrl = object.optString("link");
                            Log.d("onCompleted", userInfo.userEmail + " " + userInfo.userName + " " + userInfo.userAvatarUrl + " " + userInfo.userProfileUrl + " " + userInfo.userId);

                            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                            intent.putExtra(USER_INFO, userInfo);
                            startActivity(intent);
                        }
                    });

            Bundle parameters = new Bundle();
            parameters.putString("fields", "id, name, email, link, picture");
            request.setParameters(parameters);
            request.executeAsync();
        }

        @Override
        public void onCancel() {
            Toast.makeText(MainActivity.this, "onCancel", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(FacebookException e) {
            Toast.makeText(MainActivity.this, "onError " + e.getMessage(), Toast.LENGTH_LONG).show();
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

            // todo
            // LoginManager.getInstance().logOut();
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


    /* inner classes */

    public class GetFacebookAvatarUrl extends AsyncTask<String, Void, String> {
        // class for loading high res avatar image from facebook using graph API
        @Override
        protected String doInBackground(String... params) {
            return null;
        }

    }

}

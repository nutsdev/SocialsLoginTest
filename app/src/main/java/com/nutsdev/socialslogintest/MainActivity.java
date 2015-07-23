package com.nutsdev.socialslogintest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKScopes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements GooglePlusLoginManager.GooglePlusCallback, FacebookLoginManager.FacebookCallback, VkLoginManager.VkCallback {

    public static final String USER_INFO = "USER_INFO";

    private static final int NOT_SIGNED_IN = -1;
    private static final int SIGNED_WITH_GOOGLE = 1;
    private static final int SIGNED_WITH_FACEBOOK = 2;
    private static final int SIGNED_WITH_VK = 3;
    // Google+ states
    private static final int STATE_DEFAULT = 10;
    private static final int STATE_SIGN_IN = 11;
    private static final int STATE_IN_PROGRESS = 12;

    /* Request code used to invoke sign in user interactions. */
    private static final int REQUEST_CODE_SIGN_IN = 555;

    private int signedWith = NOT_SIGNED_IN;

    // We use googleSignInProgress to track whether user has clicked sign in.
    // googleSignInProgress can be one of three values:
    //
    //       STATE_DEFAULT: The default state of the application before the user
    //                      has clicked 'sign in', or after they have clicked
    //                      'sign out'.  In this state we will not attempt to
    //                      resolve sign in errors and so will display our
    //                      Activity in a signed out state.
    //       STATE_SIGN_IN: This state indicates that the user has clicked 'sign
    //                      in', so resolve successive errors preventing sign in
    //                      until the user has successfully authorized an account
    //                      for our app.
    //   STATE_IN_PROGRESS: This state indicates that we have started an intent to
    //                      resolve an error, and so we should not start further
    //                      intents until the current intent completes.
    private int googleSignInProgress;

    // Used to store the PendingIntent most recently returned by Google Play
    // services until the user clicks 'sign in'.
    private PendingIntent signInIntent;

    // Used to store the error code most recently returned by Google Play services
    // until the user clicks 'sign in'.
    private int signInError;

    private GooglePlusLoginManager googlePlusLoginManager;
    private FacebookLoginManager facebookLoginManager;
    private VkLoginManager vkLoginManager;

    private SignInButton google_login_button; // Google login button
    private Button facebook_login_button; // Facebook login button
    private Button vk_login_button; // VK login button
    private Button logout_button;


    /* lifecycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context applicationContext = getApplicationContext();
        facebookLoginManager = FacebookLoginManager.getInstance(applicationContext);
        googlePlusLoginManager = GooglePlusLoginManager.getInstance(this);
        vkLoginManager = VkLoginManager.getInstance(applicationContext);

        setContentView(R.layout.activity_main);

        google_login_button = (SignInButton) findViewById(R.id.google_login_button);
        google_login_button.setOnClickListener(googleButtonListener);
        facebook_login_button = (Button) findViewById(R.id.facebook_login_button);
        facebook_login_button.setOnClickListener(facebookLoginButtonListener);
        vk_login_button = (Button) findViewById(R.id.vk_login_button);
        vk_login_button.setOnClickListener(vkLoginButtonListener);
        logout_button = (Button) findViewById(R.id.logout_button);
        logout_button.setOnClickListener(logOutButtonListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        logout_button.setEnabled(signedWith > 0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googlePlusLoginManager.registerCallback(this);
        facebookLoginManager.registerCallback(this);
        vkLoginManager.registerCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        googlePlusLoginManager.unregisterCallback(this);
        facebookLoginManager.unregisterCallback(this);
        vkLoginManager.unregisterCallback(this);
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
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, vkLoginManager)) {
            if (requestCode == REQUEST_CODE_SIGN_IN) {
                if (resultCode == RESULT_OK) {
                    // If the error resolution was successful we should continue
                    // processing errors.
                    googleSignInProgress = STATE_SIGN_IN;
                } else {
                    // If the error resolution was not successful or the user canceled,
                    // we should stop processing errors.
                    googleSignInProgress = STATE_DEFAULT;
                }

                if (!googlePlusLoginManager.isConnecting()) {
                    // If Google Play services resolved the issue with a dialog then
                    // onStart is not called so we need to re-attempt connection here.
                    googlePlusLoginManager.connect();
                }
            } else {
                facebookLoginManager.onActivityResult(requestCode, resultCode, data);
            }
        }
    }


    /* private methods */

    /* Starts an appropriate intent or dialog for user interaction to resolve
     * the current error preventing the user from being signed in.  This could
     * be a dialog allowing the user to select an account, an activity allowing
     * the user to consent to the permissions being requested by your app, a
     * setting to enable device networking, etc.
     */
    private void resolveSignInError() {
        if (signInIntent != null) {
            // We have an intent which will allow our user to sign in or
            // resolve an error.  For example if the user needs to
            // select an account to sign in with, or if they need to consent
            // to the permissions your app is requesting.
            try {
                // Send the pending intent that we stored on the most recent
                // OnConnectionFailed callback.  This will allow the user to
                // resolve the error currently preventing our connection to
                // Google Play services.
                googleSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(signInIntent.getIntentSender(), REQUEST_CODE_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                L.d("Sign in intent could not be sent: " + e.getLocalizedMessage());
                // The intent was canceled before it was sent.  Attempt to connect to
                // get an updated ConnectionResult.
                googleSignInProgress = STATE_SIGN_IN;
                googlePlusLoginManager.connect();
            }
        } else {
            // Google Play services wasn't able to provide an intent for some
            // error types, so we show the default Google Play services error
            // dialog which may still start an intent on our behalf if the
            // user can resolve the issue.
            createErrorDialog().show();
        }
    }

    private Dialog createErrorDialog() {
        if (GooglePlayServicesUtil.isUserRecoverableError(signInError)) {
            return GooglePlayServicesUtil.getErrorDialog(signInError, this, REQUEST_CODE_SIGN_IN, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            L.e("Google Play services resolution cancelled");
                            googleSignInProgress = STATE_DEFAULT;
                        //    mStatus.setText(R.string.status_signed_out);
                        }
                    });
        } else {
            return new AlertDialog.Builder(this)
                    .setMessage("Google Play services is not available. This application will close.")
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    L.e("Google Play services error could not be resolved: " + signInError);
                                    googleSignInProgress = STATE_DEFAULT;
                                //    mStatus.setText(R.string.status_signed_out);
                                }
                            }).create();
        }
    }

    private void changeButtonsState(int loggedWith) {
        if (loggedWith == NOT_SIGNED_IN) {
            google_login_button.setEnabled(true);
            facebook_login_button.setEnabled(true);
            vk_login_button.setEnabled(true);
            logout_button.setEnabled(false);
            signedWith = NOT_SIGNED_IN;
            return;
        } else if (loggedWith == SIGNED_WITH_GOOGLE) {
            signedWith = SIGNED_WITH_GOOGLE;
        } else if (loggedWith == SIGNED_WITH_FACEBOOK) {
            signedWith = SIGNED_WITH_FACEBOOK;
        } else if (loggedWith == SIGNED_WITH_VK) {
            signedWith = SIGNED_WITH_VK;
        }

        google_login_button.setEnabled(false);
        facebook_login_button.setEnabled(false);
        vk_login_button.setEnabled(false);
        logout_button.setEnabled(true);
    }

    private void startWelcomeActivity(UserInfo userInfo) {
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        intent.putExtra(USER_INFO, userInfo);
        startActivity(intent);
    }


    /* listeners */

    private View.OnClickListener googleButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (googlePlusLoginManager.isConnected()) {
                GoogleApiClient googleApiClient = googlePlusLoginManager.getGoogleApiClient();
                Plus.AccountApi.clearDefaultAccount(googleApiClient);
                googleApiClient.disconnect();
            }
            googleSignInProgress = STATE_SIGN_IN;
            googlePlusLoginManager.connect();
        }
    };

    private View.OnClickListener facebookLoginButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            List<String> permissions = Arrays.asList("public_profile", "email");
            facebookLoginManager.logInWithReadPermissions(MainActivity.this, permissions);
        }
    };

    private View.OnClickListener vkLoginButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String[] scopes = new String[]{ VKScopes.EMAIL };
            vkLoginManager.login(MainActivity.this, scopes);
        }
    };

    private View.OnClickListener logOutButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (signedWith < 0)
                return;

            switch (signedWith) {
                case SIGNED_WITH_GOOGLE:
                    // We clear the default account on sign out so that Google Play services will not return an onConnected callback without user interaction.
                    if (googlePlusLoginManager.isConnected()) {
                        Plus.AccountApi.clearDefaultAccount(googlePlusLoginManager.getGoogleApiClient());
                        googlePlusLoginManager.disconnect();
                    }
                    break;
                case SIGNED_WITH_FACEBOOK:
                    facebookLoginManager.logOut();
                    break;
                case SIGNED_WITH_VK:
                    vkLoginManager.logOut();
                    break;
            }
            changeButtonsState(NOT_SIGNED_IN);
        }
    };


    /* login managers callbacks */

    /*  Google+ */
    /* onConnected is called when our Activity successfully connects to Google Play services.  onConnected indicates that an account was selected on the
     * device, that the selected account has granted any requested permissions to our app and that we were able to establish a service connection to Google Play services. */
    @Override
    public void onConnected(Bundle connectionHint) {
        L.t(this, "Connected!");
        // Update the user interface to reflect that the user is signed in.
        changeButtonsState(SIGNED_WITH_GOOGLE);

        UserInfo userInfo = new UserInfo();
        // Retrieve some profile information to personalize our app for the user.
        GoogleApiClient googleApiClient = googlePlusLoginManager.getGoogleApiClient();
        Person currentUser = Plus.PeopleApi.getCurrentPerson(googleApiClient);
        userInfo.userEmail = Plus.AccountApi.getAccountName(googleApiClient);
        userInfo.userName = currentUser.getDisplayName();
        userInfo.userAvatarUrl = currentUser.getImage().getUrl();
        userInfo.userProfileUrl = currentUser.getUrl();
        userInfo.userId = currentUser.getId();

        // Indicate that the sign in process is complete.
        googleSignInProgress = STATE_DEFAULT;

        startWelcomeActivity(userInfo);
    }

    /* onConnectionFailed is called when our Activity could not connect to Google Play services. onConnectionFailed indicates that the user needs to select
     * an account, grant permissions or resolve an error in order to sign in. */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            L.t(this, "API UNAVAILABLE");
        } else if (googleSignInProgress != STATE_IN_PROGRESS) {
            // We do not have an intent in progress so we should store the latest
            // error resolution intent for use when the sign in button is clicked.
            signInIntent = connectionResult.getResolution();
            signInError = connectionResult.getErrorCode();

            if (googleSignInProgress == STATE_SIGN_IN) {
                // STATE_SIGN_IN indicates the user already clicked the sign in button
                // so we should continue processing errors until the user is signed in
                // or they click cancel.
                resolveSignInError();
            }
        }
    }

    /* Facebook */
    @Override
    public void onSuccess(LoginResult loginResult) {
        changeButtonsState(SIGNED_WITH_FACEBOOK);

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

                startWelcomeActivity(userInfo);
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id, name, email, link, picture");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    public void onCancel() {
        L.t(this, "onCancel");
    }

    @Override
    public void onError(FacebookException e) {
        L.toast(this, "onError " + e.getMessage());
    }

    @Override
    public void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
        L.toast(this, "old token: " + oldAccessToken + "; new token: " + currentAccessToken);
    }

    @Override
    public void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
        L.toast(this, "old profile: " + oldProfile + "; new profile: " + currentProfile);
    }

    /*  VK  */
    @Override
    public void onResult(final VKAccessToken vkAccessToken) {
        // User passed Authorization
        changeButtonsState(SIGNED_WITH_VK);

        VKParameters parameters = VKParameters.from(VKApiConst.FIELDS,
                "id,first_name,last_name,sex,bdate,city,country,photo_50,photo_100," +
                        "photo_200_orig,photo_200,photo_400_orig,photo_max,photo_max_orig,online," +
                        "online_mobile,lists,domain,has_mobile,contacts,connections,site,education," +
                        "universities,schools,can_post,can_see_all_posts,can_see_audio,can_write_private_message," +
                        "status,last_seen,common_count,relation,relatives,counters");
        VKRequest request = VKApi.users().get(parameters);
        request.useSystemLanguage = true;
        request.setPreferredLang("ru");
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                UserInfo userInfo = new UserInfo();
                JSONObject json = response.json;
                JSONArray jsonArray = json.optJSONArray("response");
                JSONObject jsonObject = jsonArray.optJSONObject(0);
                userInfo.userName = jsonObject.optString("first_name") + " " + jsonObject.optString("last_name");
                userInfo.userAvatarUrl = jsonObject.optString("photo_200");
                userInfo.userProfileUrl = "http://vk.com/" + jsonObject.optString("domain");
                userInfo.userEmail = vkAccessToken.email;
                userInfo.userId = vkAccessToken.userId;
                startWelcomeActivity(userInfo);
            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                super.attemptFailed(request, attemptNumber, totalAttempts);
            }

            @Override
            public void onError(VKError error) {
                L.t(MainActivity.this, "Error: " + error.errorMessage);
            }

            @Override
            public void onProgress(VKRequest.VKProgressType progressType, long bytesLoaded, long bytesTotal) {
                super.onProgress(progressType, bytesLoaded, bytesTotal);
            }
        });
    }

    @Override
    public void onError(VKError vkError) {
        L.toast(this, "Error: " + vkError.errorMessage);
    }

}

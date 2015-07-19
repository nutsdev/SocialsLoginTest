package com.nutsdev.socialslogintest;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Singleton for Facebook login
 */
public class FacebookLoginManager implements FacebookCallback<LoginResult> {

    private static volatile FacebookLoginManager instance;

    private final List<FacebookCallback> callbacks;

    private LoginManager loginManager;
    private CallbackManager callbackManager;

    private AccessTokenTracker facebookTokenTracker;
    private ProfileTracker facebookProfileTracker;

    private Context applicationContext;


    public static FacebookLoginManager getInstance(Context applicationContext) {
        FacebookLoginManager localInstance = instance;
        if (localInstance == null) {
            synchronized (FacebookLoginManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new FacebookLoginManager(applicationContext);
                }
            }
        }
        return localInstance;
    }

    private FacebookLoginManager(Context applicationContext) {
        this.applicationContext = applicationContext;
        callbacks = new ArrayList<>();
        setupFacebook();
    }


    /* public methods */

    public void registerCallback(FacebookCallback callback) {
        callbacks.add(callback);
    }

    public void unregisterCallback(FacebookCallback callback) {
        callbacks.remove(callback);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void logInWithReadPermissions(Activity activity, Collection<String> permissions) {
        loginManager.logInWithReadPermissions(activity, permissions);
    }

    public void logInWithReadPermissions(Fragment fragment, Collection<String> permissions) {
        loginManager.logInWithReadPermissions(fragment, permissions);
    }

    public void logOut() {
        loginManager.logOut();
    }


    /* callbacks */

    @Override
    public void onSuccess(LoginResult loginResult) {
        if (callbacks.isEmpty())
            throw new RuntimeException("Please use registerCallback(FacebookCallback callback), before using this class");

        for (FacebookCallback callback : callbacks) {
            callback.onSuccess(loginResult);
        }
    }

    @Override
    public void onCancel() {
        if (callbacks.isEmpty())
            throw new RuntimeException("Please use registerCallback(FacebookCallback callback), before using this class");

        for (FacebookCallback callback : callbacks) {
            callback.onCancel();
        }
    }

    @Override
    public void onError(FacebookException e) {
        if (callbacks.isEmpty())
            throw new RuntimeException("Please use registerCallback(FacebookCallback callback), before using this class");

        for (FacebookCallback callback : callbacks) {
            callback.onError(e);
        }
    }


    /* private methods */

    private void setupFacebook() {
        FacebookSdk.sdkInitialize(applicationContext);

        setupTokenTracker();
        setupProfileTracker();
    //    facebookTokenTracker.startTracking();
    //    facebookProfileTracker.startTracking();

        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();
        loginManager.registerCallback(callbackManager, this);
    //    login_button.setOnClickListener(facebookLoginButtonListener);
        //    login_button.setReadPermissions("public_profile", "email");
        //    login_button.registerCallback(callbackManager, loginResultFacebookCallback);
    }

    private void setupTokenTracker() {
        facebookTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                if (callbacks.isEmpty())
                    throw new RuntimeException("Please use registerCallback(FacebookCallback callback), before using this class");

                for (FacebookCallback callback : callbacks) {
                    callback.onCurrentAccessTokenChanged(oldAccessToken, currentAccessToken);
                }
                Log.d("TokenTracker", "" + currentAccessToken);
            }
        };
        facebookTokenTracker.startTracking();
    }

    private void setupProfileTracker() {
        facebookProfileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                if (callbacks.isEmpty())
                    throw new RuntimeException("Please use registerCallback(FacebookCallback callback), before using this class");

                for (FacebookCallback callback : callbacks) {
                    callback.onCurrentProfileChanged(oldProfile, currentProfile);
                }
                Log.d("ProfileTracker", "" + currentProfile);
            }
        };
        facebookProfileTracker.startTracking();
    }


    /* inner classes */

    public interface FacebookCallback {
        void onSuccess(LoginResult loginResult);
        void onCancel();
        void onError(FacebookException e);
        // optional, may be empty
        void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken);
        void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile);
    }

}

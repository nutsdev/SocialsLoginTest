package com.nutsdev.socialslogintest;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton for VK login
 */
public class VkLoginManager implements VKCallback<VKAccessToken> {

    private static volatile VkLoginManager instance;

    private final List<VkCallback> callbacks;

    private Context applicationContext;


    public static VkLoginManager getInstance(Context applicationContext) {
        VkLoginManager localInstance = instance;
        if (localInstance == null) {
            synchronized (VkLoginManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new VkLoginManager(applicationContext);
                }
            }
        }
        return localInstance;
    }

    private VkLoginManager(Context applicationContext) {
        this.applicationContext = applicationContext;
        callbacks = new ArrayList<>();
        setupVk();
    }


    /* public methods */

    public void registerCallback(VkCallback callback) {
        callbacks.add(callback);
    }

    public void unregisterCallback(VkCallback callback) {
        callbacks.remove(callback);
    }

    public void login(Activity activity, String... scopes) {
        VKSdk.login(activity, scopes);
    }

    public void login(Fragment fragment, String... scopes) {
        VKSdk.login(fragment, scopes);
    }

    public void logOut() {
        VKSdk.logout();
    }


    /* callbacks */

    @Override
    public void onResult(VKAccessToken vkAccessToken) {
        if (callbacks.isEmpty())
            throw new RuntimeException("Please use registerCallback(VkCallback callback), before using this class");

        for (VkCallback callback : callbacks) {
            callback.onResult(vkAccessToken);
        }
    }

    @Override
    public void onError(VKError vkError) {
        if (callbacks.isEmpty())
            throw new RuntimeException("Please use registerCallback(VkCallback callback), before using this class");

        for (VkCallback callback : callbacks) {
            callback.onError(vkError);
        }
    }


    /* private methods */

    private void setupVk() {
        VKSdk.initialize(applicationContext);
    }


    /* inner classes */

    public interface VkCallback {
        void onResult(VKAccessToken vkAccessToken);
        void onError(VKError vkError);
    }

}

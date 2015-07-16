package com.nutsdev.socialslogintest;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class L {

    private static final String TAG = "LOGGER";

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
    }

    public static void t(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}

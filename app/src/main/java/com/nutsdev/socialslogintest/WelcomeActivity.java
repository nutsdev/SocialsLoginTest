package com.nutsdev.socialslogintest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


public class WelcomeActivity extends AppCompatActivity {

    private TextView welcome_textView;
    private TextView email_textView;
    private ImageView avatar_imageView;


    /* lifecycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        welcome_textView = (TextView) findViewById(R.id.welcome_textView);
        email_textView = (TextView) findViewById(R.id.email_textView);
        avatar_imageView = (ImageView) findViewById(R.id.avatar_imageView);

        Intent intent = getIntent();
        if (intent != null) {
            UserInfo userInfo = (UserInfo) intent.getSerializableExtra(MainActivity.USER_INFO);
            welcome_textView.setText("Welcome, " + userInfo.userName);
            email_textView.setText("Email: " + userInfo.userEmail);
            Picasso.with(this).load(userInfo.userAvatarUrl).into(avatar_imageView);
        }
    }

}

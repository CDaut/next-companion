package com.example.hochi.nextcompanion.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.hochi.nextcompanion.AsyncTaskCallbacks;
import com.example.hochi.nextcompanion.R;
import com.example.hochi.nextcompanion.request_utils.RequestHandler;

import org.json.JSONObject;


/**
 * A login screen that offers login via phone number/pin.
 */
public class LoginActivity extends AppCompatActivity implements AsyncTaskCallbacks<String> {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private RequestHandler mAuthTask = null;

    // UI references.
    private TextView mPhoneView;
    private EditText mPinView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mPhoneView = findViewById(R.id.phone);

        mPinView = findViewById(R.id.pin);
        mPinView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        Button mPhoneSignInButton = findViewById(R.id.phone_sign_in_button);
        mPhoneSignInButton.setOnClickListener(view -> attemptLogin());

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid phone number, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mPhoneView.setError(null);
        mPinView.setError(null);

        // Store values at the time of the login attempt.
        String phone = mPhoneView.getText().toString();
        String pin = mPinView.getText().toString();
        String[] credentials = {
                "apikey=", getString(R.string.apikey),
                "mobile=", mPhoneView.getText().toString(),
                "pin=", mPinView.getText().toString()
        };

        boolean cancel = false;
        View focusView = null;

        // Check for a valid pin, if the user entered one.
        if (TextUtils.isEmpty(pin)) {
            mPinView.setError(getString(R.string.error_field_required));
            focusView = mPinView;
            cancel = true;
        }

        // Check for a valid phone address.
        if (TextUtils.isEmpty(phone)) {
            mPhoneView.setError(getString(R.string.error_field_required));
            focusView = mPhoneView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new RequestHandler(this, "POST",
                    "api/login.json", credentials);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {

        //it's 2023, we simply require the API to be high enough.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onTaskComplete(String response) {
        //Callback called when RequestHandler finished request
        if (!response.isEmpty()) {
            try {
                JSONObject jObject = new JSONObject(response);
                JSONObject userObject = jObject.getJSONObject("user");
                String loginkey = userObject.getString("loginkey");
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.loginSharedPreferencesName), MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("loginKey", loginkey);
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
            finish();
        } else {
            mPinView.setError(getString(R.string.error_incorrect_pin));
            mPinView.requestFocus();
        }
    }
}


package com.example.hochi.nextcompanion.activities;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.hochi.nextcompanion.AsyncTaskCallbacks;
import com.example.hochi.nextcompanion.R;
import com.example.hochi.nextcompanion.request_utils.HttpRequestCallable;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReturnActivity extends AppCompatActivity implements AsyncTaskCallbacks<String> {
    private String[] bikeArray;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent intent = getIntent();
        bikeArray = intent.getStringArrayExtra("bike");

        //if GPS and electric lock, show the instruction
        TextView tv = findViewById(R.id.gps_info);
        LinearLayout la = findViewById(R.id.return_form_container);
        if (bikeArray[2].equals("true")) {
            tv.setVisibility(View.VISIBLE);
            la.setVisibility(View.INVISIBLE);
        } else {
            la.setVisibility(View.VISIBLE);
            tv.setVisibility(View.INVISIBLE);
            Button mReturnSubmitButton = findViewById(R.id.return_submit_button);
            mReturnSubmitButton.setOnClickListener(view -> returnRequest());
        }
    }

    void returnRequest() {
        TextView mStationInput;
        mStationInput = findViewById(R.id.return_station_id);
        String stationID = mStationInput.getText().toString();
        //get loginkey
        SharedPreferences sharedPref = getSharedPreferences("persistence", MODE_PRIVATE);
        String defaultValue = "nokey";
        String loginKey = sharedPref.getString("loginKey", defaultValue);

        String[] params = {
                "apikey=", getString(R.string.apikey),
                "bike=", bikeArray[0],
                "loginkey=", loginKey,
                "station=", stationID,
                "comment=", ""
        };
        executor.execute(() -> {
            final Optional<String> result = new HttpRequestCallable("POST",
                    "api/return.json", params).call();
            handler.post(() -> result.ifPresent(this::onTaskComplete));
        });
    }

    @Override
    public void onTaskComplete(String response) {
        //get back to main activity
        //TODO: *any* response handling
        finish();
    }
}

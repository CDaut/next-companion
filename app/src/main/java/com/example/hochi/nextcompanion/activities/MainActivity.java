package com.example.hochi.nextcompanion.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.hochi.nextcompanion.AsyncTaskCallbacks;
import com.example.hochi.nextcompanion.R;
import com.example.hochi.nextcompanion.request_utils.RequestHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AsyncTaskCallbacks<String> {
    private static final String LOGINKEY_SHARED_PREFERENCES_KEY = "loginKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //now this "every android activity" stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final Context context = this;

        //Floating Button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(context, RentActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //pre-condition: Is there a login key?
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.loginSharedPreferencesName), MODE_PRIVATE);

        String loginKey = sharedPref.getString(LOGINKEY_SHARED_PREFERENCES_KEY, null);
        //if not, go to LoginActivity
        if (loginKey == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else {
            reloadBikeList();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.loginSharedPreferencesName),
                    MODE_PRIVATE
            );

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(LOGINKEY_SHARED_PREFERENCES_KEY);
            editor.apply();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        if (id == R.id.action_map) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.map_url)));
            startActivity(browserIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    protected void reloadBikeList() {
        //get loginkey
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.loginSharedPreferencesName),
                MODE_PRIVATE
        );

        String loginKey = sharedPref.getString(LOGINKEY_SHARED_PREFERENCES_KEY, null);

        String[] params = {
                "apikey=", getString(R.string.apikey),
                "loginkey=", loginKey
        };

        RequestHandler getBikesTask = new RequestHandler(this, "POST",
                "api/getOpenRentals.json", params);
        getBikesTask.execute((Void) null);
    }

    @Override
    public void onTaskComplete(String response) {
        //Callback called when RequestHandler finished request
        final Context context = this;
        if (!response.isEmpty()) {
            final ArrayList<String> list = new ArrayList<>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray bikesArray = jObject.getJSONArray("rentalCollection");
                for (int i = 0; i < bikesArray.length(); i++) {
                    String entry;
                    JSONObject bike = bikesArray.getJSONObject(i);
                    entry = "Bike " + bike.getString("bike")
                            + " with lock code " + bike.getString("code");
                    list.add(entry);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Create and fill list
            final ListView listview = findViewById(R.id.listview);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, list);
            listview.setAdapter(adapter);

            //Print indicator if empty
            TextView tv = findViewById(R.id.noBikes);
            if (list.isEmpty()) tv.setVisibility(View.VISIBLE);
            else tv.setVisibility(View.INVISIBLE);

            try {
                final JSONObject jObject = new JSONObject(response);
                final JSONArray bikesArray = jObject.getJSONArray("rentalCollection");
                listview.setOnItemClickListener((parent, view, position, id) -> {
                    Intent intent = new Intent(context, ReturnActivity.class);
                    try {
                        JSONObject bike = bikesArray.getJSONObject(position);
                        String bID = bike.getString("bike");
                        String stID = bike.getString("start_place");
                        String lockE = bike.getString("electric_lock");
                        String[] bikeArray = {bID, stID, lockE};
                        intent.putExtra("bike", bikeArray);
                        startActivity(intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

package com.example.hochi.nextcompanion.request_utils;

import android.util.Log;

import com.example.hochi.nextcompanion.exceptions.ResourceCloseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class HttpRequestCallable implements Callable<Optional<String>> {

    private static final String LOG_TAG = "RequestHandler";
    private static final String NEXTBIKE_API_URL = "https://api.nextbike.net/";
    private final String mHTTPmethod;
    private final String mEndpoint;
    private final String[] mCredentials;

    public HttpRequestCallable(String httpMethod, String endpoint, String[] credentials) {
        mHTTPmethod = httpMethod;
        mEndpoint = endpoint;
        mCredentials = credentials;
    }

    @Override
    public Optional<String> call() {
        StringBuilder urlParameters = new StringBuilder();

        for (int i = 0; i < mCredentials.length; i += 2) {
            try {
                urlParameters.append("&").append(mCredentials[i])
                        .append(URLEncoder.encode(mCredentials[i + 1], StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                Log.e(LOG_TAG, "Un-encodable input data using UTF8: " + mCredentials[i + 1]);
            }
        }

        //big try/catch because of URL and connection errors
        try {
            URL url = new URL(NEXTBIKE_API_URL + mEndpoint);
            HttpURLConnection unclosableConnection = (HttpURLConnection) url.openConnection();

            return Optional.of(trySend(unclosableConnection, urlParameters.toString()));

        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Unparsable URL " + NEXTBIKE_API_URL + mEndpoint);
        } catch (IOException e) {
            Log.e(LOG_TAG, "An IO Error occured");
        }

        return Optional.empty();
    }

    private String trySend(HttpURLConnection connection, String urlParameters) throws ResourceCloseException {
        try (AutoCloseable ignored = connection::disconnect) {

            //Create connection
            connection.setRequestMethod(mHTTPmethod);
            if (mHTTPmethod.equals("POST")) {
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");

                connection.setRequestProperty("Content-Length", "" +
                        urlParameters.getBytes().length);
                connection.setRequestProperty("Content-Language", "en-US");

                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
            }

            //Send request
            try (DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream())) {
                wr.write(urlParameters.getBytes(StandardCharsets.UTF_8));
                wr.flush();
            }

            int responseCode = connection.getResponseCode();

            //Get Response if code = 200
            if (responseCode == 200) {
                try (BufferedReader responeBuffer =
                             new BufferedReader(
                                     new InputStreamReader(connection.getInputStream())
                             )) {
                    return responeBuffer.lines().collect(Collectors.joining(""));
                }
                //if an error occured get error instead of output stream
            } else {
                try (BufferedReader errorBuffer =
                             new BufferedReader(
                                     new InputStreamReader(connection.getErrorStream())
                             )) {
                    return errorBuffer.lines().collect(Collectors.joining(""));
                }
            }
        } catch (Exception e) {
            throw new ResourceCloseException(e.getMessage());
        }
    }
}
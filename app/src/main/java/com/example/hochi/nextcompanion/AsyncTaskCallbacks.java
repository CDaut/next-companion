package com.example.hochi.nextcompanion;

public interface AsyncTaskCallbacks<T> {
    void onTaskComplete(T response);
}

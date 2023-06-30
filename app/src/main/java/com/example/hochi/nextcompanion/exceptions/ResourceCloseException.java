package com.example.hochi.nextcompanion.exceptions;

import java.io.IOException;

/**
 * An exception to throw if closing of a resource fails
 */
public class ResourceCloseException extends IOException {
    public ResourceCloseException(String message){
        super(message);
    }
}

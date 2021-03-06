package me.hwproj.mafiagame.networking.serialization;

/**
 * Thrown when deserialization fails
 */
public class DeserializationException extends Exception {
    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeserializationException(String message) {
        super(message);
    }

    public DeserializationException(Throwable cause) {
        super(cause);
    }
}

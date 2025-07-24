package us.zoom.security.dfence.exception;

public class ObjectNameException extends RuntimeException {

    public ObjectNameException() {
    }

    public ObjectNameException(String message) {
        super(message);
    }

    public ObjectNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectNameException(Throwable cause) {
        super(cause);
    }

    public ObjectNameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

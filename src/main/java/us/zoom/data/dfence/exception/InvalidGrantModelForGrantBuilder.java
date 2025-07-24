package us.zoom.data.dfence.exception;

public class InvalidGrantModelForGrantBuilder extends RuntimeException {
    public InvalidGrantModelForGrantBuilder() {
    }

    public InvalidGrantModelForGrantBuilder(String message) {
        super(message);
    }

    public InvalidGrantModelForGrantBuilder(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidGrantModelForGrantBuilder(Throwable cause) {
        super(cause);
    }

    public InvalidGrantModelForGrantBuilder(
            String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

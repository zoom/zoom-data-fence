package us.zoom.data.dfence.exception;

public class RbacValueError extends RuntimeException {
    public RbacValueError() {
    }

    public RbacValueError(String message) {
        super(message);
    }

    public RbacValueError(String message, Throwable cause) {
        super(message, cause);
    }

    public RbacValueError(Throwable cause) {
        super(cause);
    }

    public RbacValueError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

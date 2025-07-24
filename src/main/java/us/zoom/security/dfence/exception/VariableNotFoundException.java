package us.zoom.security.dfence.exception;

public class VariableNotFoundException extends RbacDataError {
    public VariableNotFoundException() {
    }

    public VariableNotFoundException(String message) {
        super(message);
    }

    public VariableNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public VariableNotFoundException(Throwable cause) {
        super(cause);
    }
}

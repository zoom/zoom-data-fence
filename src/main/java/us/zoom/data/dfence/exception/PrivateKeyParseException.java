package us.zoom.data.dfence.exception;

public class PrivateKeyParseException extends RbacDataError {
  public PrivateKeyParseException() {}

  public PrivateKeyParseException(String message) {
    super(message);
  }

  public PrivateKeyParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public PrivateKeyParseException(Throwable cause) {
    super(cause);
  }
}

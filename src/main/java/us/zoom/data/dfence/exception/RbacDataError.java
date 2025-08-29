package us.zoom.data.dfence.exception;

public class RbacDataError extends RuntimeException {
  public RbacDataError() {}

  public RbacDataError(String message) {
    super(message);
  }

  public RbacDataError(String message, Throwable cause) {
    super(message, cause);
  }

  public RbacDataError(Throwable cause) {
    super(cause);
  }
}

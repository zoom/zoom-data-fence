package us.zoom.data.dfence.exception;

public class DatabaseError extends RuntimeException {
  public DatabaseError() {}

  public DatabaseError(String message) {
    super(message);
  }

  public DatabaseError(String message, Throwable cause) {
    super(message, cause);
  }

  public DatabaseError(Throwable cause) {
    super(cause);
  }

  public DatabaseError(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

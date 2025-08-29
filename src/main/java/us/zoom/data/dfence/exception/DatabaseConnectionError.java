package us.zoom.data.dfence.exception;

public class DatabaseConnectionError extends DatabaseError {
  public DatabaseConnectionError() {}

  public DatabaseConnectionError(String message) {
    super(message);
  }

  public DatabaseConnectionError(String message, Throwable cause) {
    super(message, cause);
  }

  public DatabaseConnectionError(Throwable cause) {
    super(cause);
  }

  public DatabaseConnectionError(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

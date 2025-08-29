package us.zoom.data.dfence.exception;

public class NoGrantBuilderError extends RbacDataError {
  public NoGrantBuilderError() {}

  public NoGrantBuilderError(String message) {
    super(message);
  }

  public NoGrantBuilderError(String message, Throwable cause) {
    super(message, cause);
  }

  public NoGrantBuilderError(Throwable cause) {
    super(cause);
  }
}

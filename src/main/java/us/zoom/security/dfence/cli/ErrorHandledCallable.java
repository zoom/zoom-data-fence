package us.zoom.security.dfence.cli;

import lombok.extern.slf4j.Slf4j;
import us.zoom.security.dfence.exception.DatabaseConnectionError;
import us.zoom.security.dfence.exception.DatabaseError;
import us.zoom.security.dfence.exception.RbacDataError;

import java.util.concurrent.Callable;

@Slf4j
public abstract class ErrorHandledCallable implements Callable<Integer> {
    abstract Integer unhandledCall();

    @Override
    public Integer call() {
        String generalErrorMessage = "Set your log level to DEBUG for more information.";
        try {
            return unhandledCall();
        } catch (RbacDataError e) {
            log.error("A data error has occurred. {} {}", e, generalErrorMessage);
            log.debug("Error traceback", e);
            return 3;
        } catch (DatabaseConnectionError e) {
            log.error(
                    "A database connection error has occurred. Review your profiles file. {} {}",
                    e,
                    generalErrorMessage);
            log.debug("Error traceback", e);
            return 4;
        } catch (DatabaseError e) {
            log.error("A database error has occurred. {} {}", e, generalErrorMessage);
            log.debug("Error traceback", e);
            return 5;
        }
    }
}

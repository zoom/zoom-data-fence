package us.zoom.data.dfence.test.fixtures;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Slf4j
public class LifecycleManager implements LifecycleObject {

    private List<LifecycleObject> lifecycleObjects = new ArrayList<>();


    @Override
    public void setup() {
        try {
            lifecycleObjects.forEach(LifecycleObject::setup);
        } catch (Exception e) {
            log.error("Aborting setup due to error. Tearing down.", e);
            try {
                teardown();
            } catch (Exception teardownException) {
                throw new LifecycleTeardownError("Teardown failed after setup failure", teardownException);
            }
            throw new RuntimeException("Setup failed", e);
        }
    }

    @Override
    public void teardown() {
        List<Exception> exceptions = new ArrayList<>();
        for (int i = lifecycleObjects.size() - 1; i >= 0; i--) {
            try {
                lifecycleObjects.get(i).teardown();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            for (Exception e : exceptions) {
                log.error(e.getMessage(), e);
            }
            throw new LifecycleTeardownError("Teardown failed", exceptions.get(0));
        }

    }
}

class LifecycleTeardownError extends RuntimeException {

    public LifecycleTeardownError() {
    }

    public LifecycleTeardownError(String message) {
        super(message);
    }

    public LifecycleTeardownError(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleTeardownError(Throwable cause) {
        super(cause);
    }
}

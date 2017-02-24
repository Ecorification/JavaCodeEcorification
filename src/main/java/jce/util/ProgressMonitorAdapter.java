package jce.util;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Adapter class to feed {@link IProgressMonitor} tasks and their messages into a log4j logger.
 * @author Timur Saglam
 */
public class ProgressMonitorAdapter implements IProgressMonitor {
    private boolean canceled;
    private final Logger logger;

    /**
     * Basic constructor, sets the logger.
     * @param logger is the logger to redirect to the tasks and their messages.
     */
    public ProgressMonitorAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void beginTask(String name, int totalWork) {
        redirectToLogger(name);
    }

    @Override
    public void done() {
        // Does nothing.
    }

    @Override
    public void internalWorked(double work) {
        // Does nothing.
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean value) {
        canceled = value;
    }

    @Override
    public void setTaskName(String name) {
        redirectToLogger(name);

    }

    @Override
    public void subTask(String name) {
        redirectToLogger(name);

    }

    @Override
    public void worked(int work) {
        // Does nothing.
    }

    /**
     * Redirects a message to the logger if it is not null or only one character.
     * @param message is the message that gets redirected.
     */
    private void redirectToLogger(String message) {
        if (message != null && message.length() > 1) {
            logger.info(message);
        }
    }
}
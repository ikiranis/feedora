package eu.apps4net.feedora.exceptions;

import eu.apps4net.feedora.models.LogType;

/**
 * Thrown when a feed can't be downloaded or parsed. Carries the kind of failure and debugging details for the logs.
 */
public class FeedFetchException extends Exception {
    private final LogType type;
    private final String details;

    public FeedFetchException(LogType type, String message, String details, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.details = details;
    }

    public LogType getType() { return type; }
    public String getDetails() { return details; }
}

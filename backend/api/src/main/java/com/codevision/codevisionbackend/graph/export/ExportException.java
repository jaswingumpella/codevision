package com.codevision.codevisionbackend.graph.export;

/**
 * Runtime exception thrown when a graph export operation fails.
 */
public class ExportException extends RuntimeException {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportException(String message) {
        super(message);
    }
}

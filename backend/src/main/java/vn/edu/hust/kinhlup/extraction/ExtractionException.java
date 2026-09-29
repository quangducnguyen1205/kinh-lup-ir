package vn.edu.hust.kinhlup.extraction;

import java.io.IOException;

public class ExtractionException extends IOException {
    public enum Reason { EMPTY_INPUT, FILE_TOO_LARGE, TEXT_TOO_LARGE, UNSUPPORTED_TYPE, PARSE_FAILED }
    private final Reason reason;

    public ExtractionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ExtractionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}

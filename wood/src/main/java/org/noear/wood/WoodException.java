package org.noear.wood;

/**
 * @author noear
 * @since 3.3
 */
public class WoodException extends RuntimeException {
    public WoodException(String message) {
        super(message);
    }

    public WoodException(String message, Throwable cause) {
        super(message, cause);
    }

    public WoodException(Throwable cause) {
        super(cause);
    }
}

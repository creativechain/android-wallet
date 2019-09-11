package crea.wallet.lite.swap.exception;

/**
 * A custom Exception to handle problems while working with keys.
 * 
 * @author<a href="http://steemit.com/@dez1337">dez1337</a>
 */
public class KeyHandlingException extends Exception {
    private static final long serialVersionUID = 6567388066484382881L;

    public KeyHandlingException(String message) {
        super(message);
    }

    public KeyHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}

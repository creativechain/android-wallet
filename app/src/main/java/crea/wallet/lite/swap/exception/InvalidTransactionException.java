package crea.wallet.lite.swap.exception;

/**
 * A custom Exception to handle invalid transactions.
 * 
 * @author<a href="http://steemit.com/@dez1337">dez1337</a>
 */
public class InvalidTransactionException extends Exception {
    private static final long serialVersionUID = -3747123400720358339L;

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}

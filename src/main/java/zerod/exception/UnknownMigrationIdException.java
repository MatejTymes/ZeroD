package zerod.exception;

public class UnknownMigrationIdException extends RuntimeException {

    public UnknownMigrationIdException(String message) {
        super(message);
    }
}

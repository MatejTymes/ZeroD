package zerod.migration.exception;

public class UnknownMigrationIdException extends RuntimeException {

    public UnknownMigrationIdException(String message) {
        super(message);
    }
}

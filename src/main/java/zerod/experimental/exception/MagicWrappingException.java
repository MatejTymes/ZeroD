package zerod.experimental.exception;

// todo: should this go into javafixes ???
public class MagicWrappingException extends RuntimeException {

    public MagicWrappingException(Exception cause) {
        super(cause);
    }

    public void throwWrappedExceptionWithMagic() {
        throwException(getCause());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwException(Throwable exception) throws T {
        throw (T) exception;
    }
}

package zerod.magic.exception;

public class MagicWrappingException extends RuntimeException {

    public MagicWrappingException(Exception cause) {
        super(cause);
    }

    public void throwExceptionWrappedWithMagic() {
        throwException(getCause());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwException(Throwable exception) throws T {
        throw (T) exception;
    }
}

package zerod.magic.exception;

public class MagicWrapper<T> {

    private final T value;
    private final MagicWrappingException exception;

    public MagicWrapper(T value) {
        this.value = value;
        this.exception = null;
    }

    public MagicWrapper(Exception exception) {
        this.value = null;
        this.exception = new MagicWrappingException(exception);
    }

    public MagicWrapper(MagicWrappingException exception) {
        this.value = null;
        this.exception = exception;
    }

    public T getOrThrowMagic() {
        if (exception != null) {
            exception.throwExceptionWrappedWithMagic();
        }
        return value;
    }
}

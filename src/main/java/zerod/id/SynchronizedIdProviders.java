package zerod.id;

public class SynchronizedIdProviders<T> {

    private final IdProvider<T> oldCodeIdProvider;
    private final IdProvider<T> newCodeIdProvider;

    SynchronizedIdProviders(IdProvider<T> oldCodeIdProvider, IdProvider<T> newCodeIdProvider) {
        this.oldCodeIdProvider = oldCodeIdProvider;
        this.newCodeIdProvider = newCodeIdProvider;
    }

    public IdProvider<T> oldCodeIdProvider() {
        return oldCodeIdProvider;
    }

    public IdProvider<T> newCodeIdProvider() {
        return newCodeIdProvider;
    }
}

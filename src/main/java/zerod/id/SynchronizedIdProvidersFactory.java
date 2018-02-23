package zerod.id;

import zerod.beta.guide.ReadWriteHelper;

import java.util.List;
import java.util.concurrent.locks.StampedLock;

import static javafixes.common.CollectionUtil.newList;

// todo: test this
public class SynchronizedIdProvidersFactory<T> {

    private final ReadWriteHelper helper;
    private final IdProvider<T> oldIdProvider;
    private final IdProvider<T> newIdProvider;

    public SynchronizedIdProvidersFactory(ReadWriteHelper helper, IdProvider<T> oldIdProvider, IdProvider<T> newIdProvider) {
        this.helper = helper;
        this.oldIdProvider = oldIdProvider;
        this.newIdProvider = newIdProvider;
    }

    public SynchronizedIdProviders<T> synchronizedIdProviders() {
        StampedLock lock = new StampedLock();

        List<T> oldIdsToUse = newList();
        List<T> newIdsToUse = newList();

        Runnable prepareNextId = () -> {
            T nextId = helper.runReadOp(oldIdProvider::nextId, newIdProvider::nextId);
            oldIdsToUse.add(nextId);
            newIdsToUse.add(nextId);
        };

        return new SynchronizedIdProviders<T>(
                () -> getNextId(lock, oldIdsToUse, prepareNextId),
                () -> getNextId(lock, newIdsToUse, prepareNextId)
        );
    }

    private T getNextId(StampedLock lock, List<T> idsToUse, Runnable prepareNextId) {
        long stamp = lock.writeLock();
        try {
            if (idsToUse.isEmpty()) {
                prepareNextId.run();
            }
            return idsToUse.remove(0);
        } finally {
            lock.unlock(stamp);
        }
    }
}
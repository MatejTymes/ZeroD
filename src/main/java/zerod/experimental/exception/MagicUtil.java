package zerod.experimental.exception;

import mtymes.javafixes.concurrency.Task;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class MagicUtil {

    public static <T> Supplier<T> wrapExceptionsIntoMagic(Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new MagicWrappingException(e);
            }
        };
    }

    public static Runnable wrapExceptionsIntoMagic(Task task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                throw new MagicWrappingException(e);
            }
        };
    }
}

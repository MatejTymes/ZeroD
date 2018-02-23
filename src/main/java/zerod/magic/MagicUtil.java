package zerod.magic;

import javafixes.concurrency.Task;
import zerod.magic.exception.MagicWrappingException;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

// todo: change package from experimental to magic
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

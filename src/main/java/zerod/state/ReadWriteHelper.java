package zerod.state;

import javafixes.concurrency.Task;
import zerod.state.domain.ReadState;
import zerod.state.domain.WriteState;

import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

// todo: rename to ReadWriteGuide
// todo: implement
public interface ReadWriteHelper {

    <T> T runReadOp(Function<ReadState, T> reader);

    void runWriteOp(Consumer<WriteState> writer);

    <T> T runReadWriteOp(BiFunction<ReadState, WriteState, T> readWriter);

    /* ====================== */
    /* derived helper methods */
    /* ====================== */

    <T> T runReadOp(Supplier<T> oldReader, Supplier<T> newReader);

    <T> T runReadOp_WithMagic(Callable<T> oldReader, Callable<T> newReader);

    void runWriteOp(Runnable oldWriter, Runnable newWriter, WriteBothConfig writeBothConfig);

    void runWriteOp_WithMagic(Task oldWriter, Task newWriter, WriteBothConfig writeBothConfig);

    <T> T runReadWriteOp(Supplier<T> oldReadWriter, Supplier<T> newReadWriter, WriteBothConfig writeBothConfig);

    <T> T runReadWriteOp_WithMagic(Callable<T> oldReadWriter, Callable<T> newReadWriter, WriteBothConfig writeBothConfig);

    enum WriteBothConfig {
        StopIfOldFails,
        RunNewEvenIfOldFails,
        RunConcurrently
    }
}

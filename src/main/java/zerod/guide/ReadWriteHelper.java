package zerod.guide;

import mtymes.javafixes.concurrency.Runner;
import mtymes.javafixes.concurrency.Task;
import zerod.experimental.exception.MagicWrappingException;
import zerod.state.ReadState;
import zerod.state.WriteState;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static mtymes.javafixes.concurrency.Runner.runner;
import static zerod.experimental.MagicUtil.wrapExceptionsIntoMagic;
import static zerod.guide.ReadWriteHelper.WriteBothConfig.*;
import static zerod.state.ReadState.ReadNew;
import static zerod.state.ReadState.ReadOld;
import static zerod.state.WriteState.*;

// todo: test this
public class ReadWriteHelper implements ReadWriteGuide {

    public enum WriteBothConfig {

        StopIfOldFails,
        RunNewEvenIfOldFails,
        RunConcurrently
    }

    private final ReadWriteGuide guide;

    public ReadWriteHelper(ReadWriteGuide guide) {
        this.guide = guide;
    }

    @Override
    public <T> T runReadOp(Function<ReadState, T> reader) {
        return guide.runReadOp(reader);
    }

    @Override
    public void runWriteOp(Consumer<WriteState> writer) {
        guide.runWriteOp(writer);
    }

    @Override
    public <T> T runReadWriteOp(BiFunction<ReadState, WriteState, T> readWriter) {
        return guide.runReadWriteOp(readWriter);
    }

    public <T> T runReadOp(Supplier<T> oldReader, Supplier<T> newReader) {
        return guide.runReadOp(readState -> {
            checkIsValid(readState);

            return (readState == ReadOld)
                    ? oldReader.get()
                    : newReader.get();
        });
    }

    public <T> T runReadOpWithMagic(Callable<T> oldReader, Callable<T> newReader) {
        T value = null;
        try {
            value = runReadOp(
                    wrapExceptionsIntoMagic(oldReader),
                    wrapExceptionsIntoMagic(newReader)
            );
        } catch (MagicWrappingException e) {
            e.throwWrappedExceptionWithMagic();
        }
        return value;
    }

    public void runWriteOp(Runnable oldWriter, Runnable newWriter, WriteBothConfig writeBothConfig) {
        guide.runWriteOp(writeState -> {
            checkIsValid(writeState);
            checkIsValid(writeBothConfig);

            if (writeState == WriteOld) {
                oldWriter.run();
            } else if (writeState == WriteNew) {
                newWriter.run();
            } else { // WriteBoth

                if (writeBothConfig == StopIfOldFails) {
                    oldWriter.run();
                    newWriter.run();
                } else if (writeBothConfig == RunNewEvenIfOldFails) {
                    RuntimeException oldException = null;
                    try {
                        oldWriter.run();
                    } catch (RuntimeException e) {
                        oldException = e;
                    }

                    newWriter.run();

                    if (oldException != null) {
                        throw oldException;
                    }
                } else { // RunConcurrently
                    Runner runner = runner(2);

                    Future<Void> oldProgress = runner.run(oldWriter);
                    Future<Void> newProgress = runner.run(newWriter);

                    // todo: join into one line
                    runner.waitTillDone();
                    runner.shutdown();

                    try {
                        newProgress.get();
                        oldProgress.get();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else {
                            throw new RuntimeException(cause);
                        }
                    }
                }
            }
        });
    }

    public void runWriteOpWithMagic(Task oldWriter, Task newWriter, WriteBothConfig writeBothConfig) {
        try {
            runWriteOp(
                    wrapExceptionsIntoMagic(oldWriter),
                    wrapExceptionsIntoMagic(newWriter),
                    writeBothConfig
            );
        } catch (MagicWrappingException e) {
            e.throwWrappedExceptionWithMagic();
        }
    }

    public <T> T runReadWriteOp(Supplier<T> oldReadWriter, Supplier<T> newReadWriter, WriteBothConfig writeBothConfig) {
        return guide.runReadWriteOp((readState, writeState) -> {
            checkIsValid(readState);
            checkIsValid(writeState);
            checkIsValid(writeBothConfig);

            if (writeState == WriteOld) {
                return oldReadWriter.get();
            } else if (writeState == WriteNew) {
                return newReadWriter.get();
            } else { // WriteBoth

                T oldValue = null;
                T newValue = null;

                if (writeBothConfig == StopIfOldFails) {
                    oldValue = oldReadWriter.get();
                    newValue = newReadWriter.get();
                } else if (writeBothConfig == RunNewEvenIfOldFails) {
                    RuntimeException oldException = null;
                    RuntimeException newException = null;

                    try {
                        oldValue = oldReadWriter.get();
                    } catch (RuntimeException e) {
                        oldException = e;
                    }
                    try {
                        newValue = newReadWriter.get();
                    } catch (RuntimeException e) {
                        newException = e;
                    }

                    throwExceptionIfSomeOccurred(readState, oldException, newException);

                } else { // RunConcurrently
                    Runner runner = runner(2);

                    Future<T> oldProgress = runner.run(oldReadWriter::get);
                    Future<T> newProgress = runner.run(newReadWriter::get);

                    // todo: join into one line
                    runner.waitTillDone();
                    runner.shutdown();

                    RuntimeException oldException = null;
                    RuntimeException newException = null;
                    try {
                        oldValue = oldProgress.get();
                    } catch (InterruptedException e) {
                        oldException = new RuntimeException(e);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException) {
                            oldException = (RuntimeException) cause;
                        } else {
                            oldException = new RuntimeException(cause);
                        }
                    }
                    try {
                        newValue = newProgress.get();
                    } catch (InterruptedException e) {
                        newException = new RuntimeException(e);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException) {
                            newException = (RuntimeException) cause;
                        } else {
                            newException = new RuntimeException(cause);
                        }
                    }

                    throwExceptionIfSomeOccurred(readState, oldException, newException);
                }

                return (readState == ReadOld) ? oldValue : newValue;
            }
        });
    }

    public <T> T runReadWriteOpWithMagic(Callable<T> oldReadWriter, Callable<T> newReadWriter, WriteBothConfig writeBothConfig) {
        T value = null;
        try {
            value = runReadWriteOp(
                    wrapExceptionsIntoMagic(oldReadWriter),
                    wrapExceptionsIntoMagic(newReadWriter),
                    writeBothConfig
            );
        } catch (MagicWrappingException e) {
            e.throwWrappedExceptionWithMagic();
        }
        return value;
    }

    private static void checkIsValid(ReadState readState) {
        if (readState != ReadOld && readState != ReadNew) {
            throw new IllegalStateException("Invalid ReadState: " + readState);
        }
    }

    private static void checkIsValid(WriteState writeState) {
        if (writeState != WriteOld && writeState != WriteNew && writeState != WriteBoth) {
            throw new IllegalStateException("Invalid WriteState: " + writeState);
        }
    }

    private static void checkIsValid(WriteBothConfig writeBothConfig) {
        if (writeBothConfig != StopIfOldFails && writeBothConfig != RunNewEvenIfOldFails && writeBothConfig != RunConcurrently) {
            throw new IllegalStateException("Invalid WriteBothConfig: " + writeBothConfig);
        }
    }

    private static void throwExceptionIfSomeOccurred(ReadState readState, RuntimeException oldException, RuntimeException newException) {
        Optional<RuntimeException> exception = asList(oldException, newException).stream()
                .filter(e -> e != null)
                .reduce((e1, e2) -> (readState == ReadOld) ? e1 : e2);

        if (exception.isPresent()) {
            throw exception.get();
        }
    }
}

package zerod.state;

import javafixes.concurrency.Runner;
import javafixes.concurrency.Task;
import zerod.magic.exception.MagicWrappingException;
import zerod.state.domain.ReadState;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static javafixes.concurrency.Runner.runner;
import static zerod.magic.MagicUtil.wrapExceptionsIntoMagic;
import static zerod.state.ReadWriteHelper.WriteBothConfig.*;
import static zerod.state.domain.ReadState.ReadOld;
import static zerod.state.domain.WriteState.WriteNew;
import static zerod.state.domain.WriteState.WriteOld;

// todo: test this
public abstract class AbstractReadWriteHelper implements ReadWriteHelper {

    // todo: test this
    @Override
    public <T> T runReadOp(Supplier<T> oldReader, Supplier<T> newReader) {
        return runReadOp(readState -> {
            checkNotNull(readState, "ReadState can't be null");

            return (readState == ReadOld)
                    ? oldReader.get()
                    : newReader.get();
        });
    }

    // todo: test this
    @Override
    public <T> T runReadOp_WithMagic(Callable<T> oldReader, Callable<T> newReader) {
        T value = null;
        try {
            value = runReadOp(
                    wrapExceptionsIntoMagic(oldReader),
                    wrapExceptionsIntoMagic(newReader)
            );
        } catch (MagicWrappingException e) {
            e.throwExceptionWrappedWithMagic();
        }
        return value;
    }

    // todo: test this
    @Override
    public void runWriteOp(Runnable oldWriter, Runnable newWriter, WriteBothConfig writeBothConfig) {
        runWriteOp(writeState -> {
            checkNotNull(writeState, "WriteState can't be null");
            checkNotNull(writeBothConfig, "WriteBothConfig can't be null");

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
                } else if (writeBothConfig == RunConcurrently) {
                    Runner runner = runner(2);

                    Future<Void> oldProgress = runner.run(oldWriter);
                    Future<Void> newProgress = runner.run(newWriter);

                    runner.waitTillDone().shutdown();

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
                } else {
                    throw new IllegalArgumentException("unsupported WriteBothConfig: " + writeBothConfig);
                }
            }
        });
    }

    // todo: test this
    @Override
    public void runWriteOp_WithMagic(Task oldWriter, Task newWriter, WriteBothConfig writeBothConfig) {
        try {
            runWriteOp(
                    wrapExceptionsIntoMagic(oldWriter),
                    wrapExceptionsIntoMagic(newWriter),
                    writeBothConfig
            );
        } catch (MagicWrappingException e) {
            e.throwExceptionWrappedWithMagic();
        }
    }

    // todo: test this
    @Override
    public <T> T runReadWriteOp(Supplier<T> oldReadWriter, Supplier<T> newReadWriter, WriteBothConfig writeBothConfig) {
        return runReadWriteOp((readState, writeState) -> {
            checkNotNull(readState, "ReadState can't be null");
            checkNotNull(writeState, "WriteState can't be null");
            checkNotNull(writeBothConfig, "WriteBothConfig can't be null");

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

                } else if (writeBothConfig == RunConcurrently) { // RunConcurrently
                    Runner runner = runner(2);

                    Future<T> oldProgress = runner.run(oldReadWriter::get);
                    Future<T> newProgress = runner.run(newReadWriter::get);

                    runner.waitTillDone().shutdown();

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
                } else {
                    throw new IllegalArgumentException("unsupported WriteBothConfig: " + writeBothConfig);
                }

                return (readState == ReadOld) ? oldValue : newValue;
            }
        });
    }

    // todo: test this
    @Override
    public <T> T runReadWriteOp_WithMagic(Callable<T> oldReadWriter, Callable<T> newReadWriter, WriteBothConfig writeBothConfig) {
        T value = null;
        try {
            value = runReadWriteOp(
                    wrapExceptionsIntoMagic(oldReadWriter),
                    wrapExceptionsIntoMagic(newReadWriter),
                    writeBothConfig
            );
        } catch (MagicWrappingException e) {
            e.throwExceptionWrappedWithMagic();
        }
        return value;
    }

    protected static void throwExceptionIfSomeOccurred(ReadState readState, RuntimeException oldException, RuntimeException newException) {
        Optional<RuntimeException> exception = Stream.of(oldException, newException)
                .filter(Objects::nonNull)
                .reduce((e1, e2) -> (readState == ReadOld) ? e1 : e2);

        if (exception.isPresent()) {
            throw exception.get();
        }
    }
}

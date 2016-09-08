package zerod.guide;

import mtymes.javafixes.concurrency.Runner;
import zerod.ReadState;
import zerod.WriteState;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static mtymes.javafixes.concurrency.Runner.runner;
import static zerod.ReadState.ReadNew;
import static zerod.ReadState.ReadOld;
import static zerod.WriteState.*;
import static zerod.guide.ReadWriteHelper.WriteBothConfig.*;

// todo: test this
public class ReadWriteHelper {

    public enum WriteBothConfig {

        StopIfOldFails,
        RunNewEvenIfOldFails,
        RunConcurrently
    }


    public static <T> T runReadOp(ReadWriteGuide guide, Supplier<T> oldReader, Supplier<T> newReader) {
        return guide.runReadOp(readState -> {
            checkIsValid(readState);

            return (readState == ReadOld)
                    ? oldReader.get()
                    : newReader.get();
        });
    }

    public static void runWriteOp(ReadWriteGuide guide, Runnable oldWriter, Runnable newWriter, WriteBothConfig writeBothConfig) {
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

    public static <T> T runReadWriteOp(ReadWriteGuide guide, Supplier<T> oldReadWriter, Supplier<T> newReadWriter, WriteBothConfig writeBothConfig) {
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

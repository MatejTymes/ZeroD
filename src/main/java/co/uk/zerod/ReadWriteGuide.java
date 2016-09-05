package co.uk.zerod;

import mtymes.javafixes.concurrency.ReusableCountLatch;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.stream;

public class ReadWriteGuide {

    private final Map<ReadState, ReusableCountLatch> readCounters = new HashMap<>();
    private final Map<WriteState, ReusableCountLatch> writeCounters = new HashMap<>();

    private volatile ReadWriteState currentState;
    private volatile ReadWriteState transitionToState;


    public ReadWriteGuide(ReadWriteState state) {
        this.currentState = state;
        this.transitionToState = state;

        stream(ReadState.values()).forEach(readState -> readCounters.put(readState, new ReusableCountLatch()));
        stream(WriteState.values()).forEach(writeState -> writeCounters.put(writeState, new ReusableCountLatch()));
    }

    public <T> T write(Function<WriteState, T> writer) {
        // todo: lock this section
        WriteState writeState = transitionToState.writeState;
        ReusableCountLatch writeCounter = writeCounters.get(writeState);
        writeCounter.increment();

        try {

            return writer.apply(writeState);

        } finally {
            writeCounter.decrement();
        }
    }

    public <T> T read(Function<ReadState, T> reader) {
        // todo: lock this section
        ReadState readState = transitionToState.readState;
        ReusableCountLatch readCounter = readCounters.get(readState);
        readCounter.increment();

        try {

            return reader.apply(readState);

        } finally {
            readCounter.decrement();
        }
    }

    // todo: test
    public <T> T readWrite(BiFunction<ReadState, WriteState, T> readWriter) {
        // todo: lock this section
        ReadState readState = transitionToState.readState;
        WriteState writeState = transitionToState.writeState;
        ReusableCountLatch readCounter = readCounters.get(readState);
        ReusableCountLatch writeCounter = writeCounters.get(writeState);
        readCounter.increment();
        writeCounter.increment();

        try {

            return readWriter.apply(readState, writeState);

        } finally {
            readCounter.decrement();
            writeCounter.decrement();
        }
    }

    public synchronized void switchState(ReadWriteState toState) {
        if (currentState.ordinal() != toState.ordinal() && currentState.ordinal() + 1 != toState.ordinal()) {
            throw new IllegalStateException("Unable to transition from '" + currentState + "' state to '" + toState + "' state");
        }
        this.transitionToState = toState;

        try {
            if (currentState.readState != toState.readState) {
                readCounters.get(currentState.readState).waitTillZero();
            }
            if (currentState.writeState != toState.writeState) {
                writeCounters.get(currentState.writeState).waitTillZero();
            }
        } catch (Exception e) {
            this.transitionToState = this.currentState;
            throw new IllegalStateException(e);
        }

        this.currentState = toState;
    }

    ReadWriteState getCurrentState() {
        return currentState;
    }

    ReadWriteState getTransitionToState() {
        return transitionToState;
    }
}

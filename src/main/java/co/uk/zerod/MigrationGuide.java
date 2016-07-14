package co.uk.zerod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.stream;

public class MigrationGuide {

    private final Map<ReadState, ReusableCountLatch> readCounters = new HashMap<>();
    private final Map<WriteState, ReusableCountLatch> writeCounters = new HashMap<>();

    private volatile ReadWriteState currentState;
    private volatile ReadWriteState transitionToState;


    public MigrationGuide(ReadWriteState state) {
        this.currentState = state;
        this.transitionToState = state;

        stream(ReadState.values()).forEach(readState -> readCounters.put(readState, new ReusableCountLatch()));
        stream(WriteState.values()).forEach(writeState -> writeCounters.put(writeState, new ReusableCountLatch()));
    }

    public <T> T write(Function<WriteState, T> writer) {
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
        ReadState readState = transitionToState.readState;
        ReusableCountLatch readCounter = readCounters.get(readState);
        readCounter.increment();
        try {

            return reader.apply(readState);

        } finally {
            readCounter.decrement();
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

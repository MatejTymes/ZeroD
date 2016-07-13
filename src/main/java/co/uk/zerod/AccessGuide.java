package co.uk.zerod;

import java.util.function.Function;

// todo: test concurrency
public class AccessGuide {

    private ReadWriteState state;

    public AccessGuide(ReadWriteState state) {
        this.state = state;
    }

    public <T> T write(Function<WriteState, T> writer) {
        return writer.apply(state.writeState);
    }

    public <T> T read(Function<ReadState, T> reader) {
        return reader.apply(state.readState);
    }

    public ReadWriteState getState() {
        return state;
    }

    public void switchState(ReadWriteState toState) {
        if (state.ordinal() != toState.ordinal() && state.ordinal() + 1 != toState.ordinal()) {
            throw new IllegalStateException("Unable to transition from '" + state + "' state to '" + toState + "' state");
        }
        this.state = toState;
    }
}

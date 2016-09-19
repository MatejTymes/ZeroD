package zerod.guide;

import zerod.state.ReadState;
import zerod.state.ReadWriteState;
import zerod.state.WriteState;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

// todo: test this
public class ImmutableReadWriteGuide implements ReadWriteGuide {

    private final ReadWriteState currentState;

    public ImmutableReadWriteGuide(ReadWriteState currentState) {
        this.currentState = currentState;
    }

    @Override
    public <T> T runReadOp(Function<ReadState, T> reader) {
        return reader.apply(currentState.readState);
    }

    @Override
    public void runWriteOp(Consumer<WriteState> writer) {
        writer.accept(currentState.writeState);

    }

    @Override
    public <T> T runReadWriteOp(BiFunction<ReadState, WriteState, T> readWriter) {
        return readWriter.apply(currentState.readState, currentState.writeState);
    }
}

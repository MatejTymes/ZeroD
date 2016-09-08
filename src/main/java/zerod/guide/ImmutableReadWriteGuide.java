package zerod.guide;

import zerod.ReadState;
import zerod.ReadWriteState;
import zerod.WriteState;

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
    public <T> T read(Function<ReadState, T> reader) {
        return reader.apply(currentState.readState);
    }

    @Override
    public void write(Consumer<WriteState> writer) {
        writer.accept(currentState.writeState);

    }

    @Override
    public <T> T readWrite(BiFunction<ReadState, WriteState, T> readWriter) {
        return readWriter.apply(currentState.readState, currentState.writeState);
    }
}

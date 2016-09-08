package zerod.guide;

import zerod.ReadState;
import zerod.WriteState;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReadWriteGuide {

    <T> T read(Function<ReadState, T> reader);

    void write(Consumer<WriteState> writer);

    <T> T readWrite(BiFunction<ReadState, WriteState, T> readWriter);
}

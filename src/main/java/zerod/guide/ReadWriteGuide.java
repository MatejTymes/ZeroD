package zerod.guide;

import zerod.state.ReadState;
import zerod.state.WriteState;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReadWriteGuide {

    <T> T runReadOp(Function<ReadState, T> reader);

    void runWriteOp(Consumer<WriteState> writer);

    <T> T runReadWriteOp(BiFunction<ReadState, WriteState, T> readWriter);
}

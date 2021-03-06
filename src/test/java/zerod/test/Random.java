package zerod.test;

import zerod.state.domain.ReadWriteState;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

// todo: merge both Randoms into one test code
public class Random {

    public static int randomInt(int from, int to) {
        // typecast it to long as otherwise we could get int overflow
        return (int) ((long) (Math.random() * ((long) to - (long) from + 1L)) + (long) from);
    }

    @SafeVarargs
    public static ReadWriteState randomReadWriteState(Function<ReadWriteState, Boolean>... validityConditions) {
        return generateValidValue(() -> pickRandomItem(ReadWriteState.values()), validityConditions);
    }

    @SafeVarargs
    public static <T> T pickRandomItem(T... values) {
        return values[randomInt(0, values.length - 1)];
    }

    public static <T> T pickRandomItem(List<T> values) {
        return values.get(randomInt(0, values.size() - 1));
    }

    @SafeVarargs
    private static <T> T generateValidValue(Supplier<T> generator, Function<T, Boolean>... validityConditions) {
        T value;

        int infiniteCycleCounter = 0;

        boolean valid;
        do {
            valid = true;
            value = generator.get();
            for (Function<T, Boolean> validityCondition : validityConditions) {
                if (!validityCondition.apply(value)) {
                    valid = false;
                    break;
                }
            }

            if (infiniteCycleCounter++ == 1_000) {
                throw new IllegalStateException("Possibly reached infinite cycle - unable to generate value after 1000 attempts.");
            }
        } while (!valid);

        return value;
    }
}

package zerod.test;

import zerod.ReadWriteState;

import java.util.List;

// todo: merge both Randoms into one test code
public class Random {

    public static int randomInt(int from, int to) {
        // typecast it to long as otherwise we could get int overflow
        return (int) ((long) (Math.random() * ((long) to - (long) from + 1L)) + (long) from);
    }

    public static ReadWriteState randomReadWriteState() {
        return pickRandomItem(ReadWriteState.values());
    }

    @SafeVarargs
    public static <T> T pickRandomItem(T... values) {
        return values[randomInt(0, values.length - 1)];
    }

    public static <T> T pickRandomItem(List<T> values) {
        return values.get(randomInt(0, values.size() - 1));
    }
}

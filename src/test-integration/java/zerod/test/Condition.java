package zerod.test;

import java.util.Set;
import java.util.function.Function;

import static com.google.common.collect.Sets.newHashSet;

public interface Condition<T> extends Function<T, Boolean> {

    @SafeVarargs
    static <T> Condition<T> otherThan(T... values) {
        Set<T> exclusions = newHashSet(values);
        return value -> !exclusions.contains(value);
    }
}

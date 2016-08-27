package co.uk.zerod.test;

import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Awareness;
import co.uk.zerod.domain.Health;
import co.uk.zerod.domain.MigrationId;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static co.uk.zerod.domain.AgentId.agentId;
import static co.uk.zerod.domain.Health.health;
import static co.uk.zerod.domain.MigrationId.migrationId;
import static java.util.UUID.randomUUID;

// todo: merge both Randoms into one test code
public class Random {

    public static int randomInt(int from, int to) {
        // typecast it to long as otherwise we could get int overflow
        return (int) ((long) (Math.random() * ((long) to - (long) from + 1L)) + (long) from);
    }

    public static String randomUUIDString() {
        return randomUUID().toString();
    }

    @SafeVarargs
    public static AgentId randomAgentId(Function<AgentId, Boolean>... validityConditions) {
        return generateValidValue(() -> agentId("agent-" + randomInt(0, 9_999_999)), validityConditions);
    }

    @SafeVarargs
    public static MigrationId randomMigrationId(Function<MigrationId, Boolean>... validityConditions) {
        return generateValidValue(() -> migrationId("migrationId-" + randomInt(0, 9_999_999)), validityConditions);
    }

    @SafeVarargs
    public static Health randomLiveHealth(Function<Health, Boolean>... validityConditions) {
        return generateValidValue(() -> health((byte) randomInt(1, 100)), validityConditions);
    }

    @SafeVarargs
    public static Health randomHealth(Function<Health, Boolean>... validityConditions) {
        return generateValidValue(() -> health((byte) randomInt(0, 100)), validityConditions);
    }

    @SafeVarargs
    public static Awareness randomAwareness(Function<Awareness, Boolean>... validityConditions) {
        return generateValidValue(() -> pickRandomItem(Awareness.values()), validityConditions);
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
                throw new IllegalStateException("Probably reached infinite cycle - unable to generate valid value after 1000 attempts.");
            }
        } while (!valid);

        return value;
    }
}

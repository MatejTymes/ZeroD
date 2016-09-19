package zerod.guide;

import zerod.domain.MigrationId;
import zerod.state.ReadState;
import zerod.state.WriteState;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

// todo: rename to MigrationGuide
// todo: test
public class AccessGuide {

    private final Map<MigrationId, ReadWriteGuide> readWriteGuides;

    public AccessGuide(Map<MigrationId, ReadWriteGuide> readWriteGuides) {
        this.readWriteGuides = readWriteGuides;
    }

    public <T> T runReadOp(MigrationId migrationId, Function<ReadState, T> reader) {
        return getReadWriteGuide(migrationId)
                .runReadOp(reader);
    }

    public void runWriteOp(MigrationId migrationId, Consumer<WriteState> writer) {
        getReadWriteGuide(migrationId)
                .runWriteOp(writer);
    }

    public <T> T runReadWriteOp(MigrationId migrationId, BiFunction<ReadState, WriteState, T> readWriter) {
        return getReadWriteGuide(migrationId)
                .runReadWriteOp(readWriter);
    }

    private ReadWriteGuide getReadWriteGuide(MigrationId migrationId) {
        ReadWriteGuide readWriteGuide = readWriteGuides.get(migrationId);
        if (readWriteGuide == null) {
            throw new IllegalArgumentException("No ReadWriteGuide registered for MigrationId '" + migrationId + "'");
        }
        return readWriteGuide;
    }
}

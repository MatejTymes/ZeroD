package co.uk.zerod;

import co.uk.zerod.domain.MigrationId;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

// todo: rename to MigrationGuide
// todo: test
public class AccessGuide {

    private final Map<MigrationId, ReadWriteGuide> readWriteGuides;

    public AccessGuide(Map<MigrationId, ReadWriteGuide> readWriteGuides) {
        this.readWriteGuides = readWriteGuides;
    }

    public <T> T write(MigrationId migrationId, Function<WriteState, T> writer) {
        return getReadWriteGuide(migrationId)
                .write(writer);
    }

    public <T> T read(MigrationId migrationId, Function<ReadState, T> reader) {
        return getReadWriteGuide(migrationId)
                .read(reader);
    }

    // todo: test
    public <T> T readWrite(MigrationId migrationId, BiFunction<ReadState, WriteState, T> readWriter) {
        return getReadWriteGuide(migrationId)
                .readWrite(readWriter);
    }

    private ReadWriteGuide getReadWriteGuide(MigrationId migrationId) {
        ReadWriteGuide readWriteGuide = readWriteGuides.get(migrationId);
        if (readWriteGuide == null) {
            throw new IllegalArgumentException("No ReadWriteGuide registered for MigrationId '" + migrationId + "'");
        }
        return readWriteGuide;
    }
}

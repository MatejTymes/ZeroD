package co.uk.zerod;

import co.uk.zerod.wip.MigrationId;

import java.util.Map;
import java.util.function.Function;

// todo: test
public class AccessGuide {

    private final Map<MigrationId, MigrationGuide> migrationGuides;

    public AccessGuide(Map<MigrationId, MigrationGuide> migrationGuides) {
        this.migrationGuides = migrationGuides;
    }

    public <T> T write(MigrationId migrationId, Function<WriteState, T> writer) {
        return getMigrationGuide(migrationId)
                .write(writer);
    }

    public <T> T read(MigrationId migrationId, Function<ReadState, T> reader) {
        return getMigrationGuide(migrationId)
                .read(reader);
    }

    private MigrationGuide getMigrationGuide(MigrationId migrationId) {
        MigrationGuide migrationGuide = migrationGuides.get(migrationId);
        if (migrationGuide == null) {
            throw new IllegalArgumentException("No MigrationGuide registered for MigrationId '" + migrationId + "'");
        }
        return migrationGuide;
    }
}

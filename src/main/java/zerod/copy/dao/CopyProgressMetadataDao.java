package zerod.copy.dao;

import zerod.migration.domain.MigrationId;

public interface CopyProgressMetadataDao {

    void unPauseProgress(MigrationId migrationId);

    void pauseProgress(MigrationId migrationId);

    boolean isProgressPaused(MigrationId migrationId);
}

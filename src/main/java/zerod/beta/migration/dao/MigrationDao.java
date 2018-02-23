package zerod.beta.migration.dao;

import zerod.migration.domain.MigrationId;

import java.util.Set;

public interface MigrationDao {

    void registerMigration(MigrationId migrationId);

    Set<MigrationId> findAllMigrations();
}

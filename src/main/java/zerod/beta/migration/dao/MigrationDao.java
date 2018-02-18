package zerod.beta.migration.dao;

import zerod.domain.MigrationId;

import java.util.Set;

public interface MigrationDao {

    void registerMigration(MigrationId migrationId);

    Set<MigrationId> findAllMigrations();
}

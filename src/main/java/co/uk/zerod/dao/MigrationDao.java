package co.uk.zerod.dao;

import co.uk.zerod.wip.MigrationId;

import java.util.Set;

public interface MigrationDao {

    void registerMigration(MigrationId migrationId);

    Set<MigrationId> findAllMigrations();
}

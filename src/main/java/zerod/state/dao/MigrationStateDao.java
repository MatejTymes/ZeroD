package zerod.state.dao;

import zerod.migration.domain.MigrationId;
import zerod.migration.exception.UnknownMigrationIdException;
import zerod.state.domain.ReadWriteState;

// todo: add generic test
// todo: add implementations
public interface MigrationStateDao {

    boolean registerInitialState(MigrationId migrationId, ReadWriteState state);

    ReadWriteState getState(MigrationId migrationId) throws UnknownMigrationIdException;

    void setNewState(MigrationId migrationId, ReadWriteState state) throws UnknownMigrationIdException;
}

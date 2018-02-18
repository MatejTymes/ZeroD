package zerod.dao;

import zerod.domain.MigrationId;
import zerod.exception.UnknownMigrationIdException;
import zerod.state.ReadWriteState;

// todo: add generic test
// todo: add implementations
public interface MigrationStateDao {

    boolean registerInitialState(MigrationId migrationId, ReadWriteState state);

    ReadWriteState getState(MigrationId migrationId) throws UnknownMigrationIdException;

    void setNewState(MigrationId migrationId, ReadWriteState state) throws UnknownMigrationIdException;
}

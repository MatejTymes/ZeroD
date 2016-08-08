package co.uk.zerod.dao;

import co.uk.zerod.domain.TableName;
import co.uk.zerod.wip.MigrationId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static co.uk.zerod.wip.MigrationId.migrationId;

public class SqlMigrationDao extends BaseSqlDao implements MigrationDao {

    private final TableName migrationTableName;

    public SqlMigrationDao(TableName migrationTableName, DataSource dataSource) {
        super(dataSource);
        this.migrationTableName = migrationTableName;
    }

    @Override
    public void registerMigration(MigrationId migrationId) {
        try (Connection connection = dataSource.getConnection()) {
            if (isMigrationStored(migrationId, connection)) {
                return;
            }

            try {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO " + migrationTableName + " (migration_id) VALUES (?)");
                ps.setString(1, migrationId.value());
                ps.execute();
            } catch (SQLException e) {
                if (!isMigrationStored(migrationId, connection)) {
                    throw new IllegalStateException("Unable to store migration '" + migrationId + "'", e);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isMigrationStored(MigrationId migrationId, Connection connection) throws SQLException {
        PreparedStatement ps;
        boolean isMigrationStored;
        ps = connection.prepareStatement("SELECT * FROM " + migrationTableName + " WHERE migration_id = ?");
        ps.setString(1, migrationId.value());
        try (ResultSet rs = ps.executeQuery()) {
            isMigrationStored = rs.next();
        }
        return isMigrationStored;
    }

    @Override
    public Set<MigrationId> findAllMigrations() {
        return selectDistinct(
                "SELECT * FROM " + migrationTableName,
                rs -> migrationId(rs.getString("migration_id"))
        );
    }
}

package zerod.beta.migration.dao.sql;

import zerod.beta.dao.sql.BaseSqlDao;
import zerod.beta.domain.TableName;
import zerod.beta.migration.dao.MigrationDao;
import zerod.migration.domain.MigrationId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static zerod.migration.domain.MigrationId.migrationId;

public class SqlMigrationDao extends BaseSqlDao implements MigrationDao {

    private final TableName migrationTable;

    public SqlMigrationDao(TableName migrationTable, DataSource dataSource) {
        super(dataSource);
        this.migrationTable = migrationTable;
    }

    @Override
    public void registerMigration(MigrationId migrationId) {
        try (Connection connection = dataSource.getConnection()) {
            if (isMigrationStored(migrationId, connection)) {
                return;
            }

            try {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO " + migrationTable + " (migration_id) VALUES (?)");
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

    @Override
    public Set<MigrationId> findAllMigrations() {
        return selectDistinct(
                "SELECT * FROM " + migrationTable,
                rs -> migrationId(rs.getString("migration_id"))
        );
    }

    private boolean isMigrationStored(MigrationId migrationId, Connection connection) throws SQLException {
        PreparedStatement ps;
        boolean isMigrationStored;
        ps = connection.prepareStatement("SELECT * FROM " + migrationTable + " WHERE migration_id = ?");
        ps.setString(1, migrationId.value());
        try (ResultSet rs = ps.executeQuery()) {
            isMigrationStored = rs.next();
        }
        return isMigrationStored;
    }
}

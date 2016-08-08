package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.domain.Agent;
import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Health;
import co.uk.zerod.domain.TableName;

import javax.sql.DataSource;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static co.uk.zerod.common.Clock.UTC_ZONE;
import static co.uk.zerod.domain.AgentId.agentId;
import static co.uk.zerod.domain.Health.health;

public class SqlAgentDao extends BaseSqlDao implements AgentDao {

    private final TableName agentTableName;

    private final Clock clock;

    public SqlAgentDao(TableName agentTableName, DataSource dataSource, Clock clock) {
        super(dataSource);
        this.agentTableName = agentTableName;
        this.clock = clock;
    }

    @Override
    public void registerAgentHealth(AgentId agentId, Health health) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement selectStatement = connection.prepareStatement(
                    "SELECT * FROM " + agentTableName + " WHERE id = ?"
            );
            selectStatement.setString(1, agentId.value());
            boolean exists;
            try (ResultSet rs = selectStatement.executeQuery()) {
                exists = rs.next();
            }

            boolean insertionFailed = false;
            if (!exists) {
                PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO " + agentTableName + " (id, health, last_updated_at) VALUES (?, ?, ?)"
                );
                insertStatement.setString(1, agentId.value());
                insertStatement.setInt(2, health.value());
                insertStatement.setTimestamp(3, toTimeStamp(clock.now()));
                try {
                    insertionFailed = (insertStatement.executeUpdate() != 1);
                } catch (SQLException e) {
                    insertionFailed = true;
                }
            }

            if (exists || insertionFailed) {
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE " + agentTableName + " SET health = ?, last_updated_at = ? WHERE id = ?"
                );
                updateStatement.setInt(1, health.value());
                updateStatement.setTimestamp(2, toTimeStamp(clock.now()));
                updateStatement.setString(3, agentId.value());
                if (updateStatement.executeUpdate() != 1) {
                    throw new IllegalStateException("Unable to register health of agent '" + agentId + "'.");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean updateAgentsHealth(AgentId agentId, Health from, Health to, ZonedDateTime ifNotUpdatedSince) {
        int updatedRowCount = update(
                "UPDATE " + agentTableName + " SET health = ?, last_updated_at = ? WHERE id = ? AND health = ? AND last_updated_at <= ?",
                ps -> {
                    ps.setInt(1, to.value());
                    ps.setTimestamp(2, toTimeStamp(clock.now()));
                    ps.setString(3, agentId.value());
                    ps.setInt(4, from.value());
                    ps.setTimestamp(5, toTimeStamp(ifNotUpdatedSince));
                }
        );

        return updatedRowCount == 1;
    }

    @Override
    public Optional<Agent> findAgent(AgentId agentId) {
        return selectSingle(
                "SELECT * FROM " + agentTableName + " WHERE id = ?",
                ps -> ps.setString(1, agentId.value()),
                this::toAgent
        );
    }

    @Override
    public Set<Agent> findStaleAgents(ZonedDateTime notUpdatedSince) {
        return selectDistinct(
                "SELECT * FROM " + agentTableName + " WHERE last_updated_at <= ?",
                ps -> ps.setTimestamp(1, toTimeStamp(notUpdatedSince)),
                this::toAgent
        );
    }

    @Override
    public Set<Agent> findLiveAgents() {
        return selectDistinct("SELECT * FROM " + agentTableName + " WHERE health > 0", this::toAgent);
    }

    @Override
    public Set<Agent> findDeadAgents() {
        return selectDistinct("SELECT * FROM " + agentTableName + " WHERE health = 0", this::toAgent);
    }

    private Agent toAgent(ResultSet rs) throws SQLException {
        return new Agent(
                agentId(rs.getString("id")),
                health((byte) rs.getInt("health")),
                toZonedDateTime(rs.getTimestamp("last_updated_at"))
        );
    }

    private Timestamp toTimeStamp(ZonedDateTime dateTime) {
        return new Timestamp(dateTime.toInstant().toEpochMilli());
    }

    private ZonedDateTime toZonedDateTime(Timestamp timestamp) {
        return ZonedDateTime.ofInstant(timestamp.toInstant(), UTC_ZONE);
    }
}

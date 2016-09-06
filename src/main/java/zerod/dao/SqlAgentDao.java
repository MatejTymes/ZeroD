package zerod.dao;

import zerod.common.Clock;
import zerod.domain.Agent;
import zerod.domain.AgentId;
import zerod.domain.Health;
import zerod.domain.TableName;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static zerod.common.Clock.UTC_ZONE;
import static zerod.domain.AgentId.agentId;
import static zerod.domain.Health.health;

public class SqlAgentDao extends BaseSqlDao implements AgentDao {

    private final TableName agentTable;

    // todo: should we use db clock instead ?
    private final Clock clock;

    public SqlAgentDao(TableName agentTable, DataSource dataSource, Clock clock) {
        super(dataSource);
        this.agentTable = agentTable;
        this.clock = clock;
    }

    @Override
    public void registerAgentHealth(AgentId agentId, Health health) {
        upsert(
                "SELECT * FROM " + agentTable + " WHERE id = ?",
                ps -> ps.setString(1, agentId.value()),

                "INSERT INTO " + agentTable + " (id, health, last_updated_at) VALUES (?, ?, ?)",
                ps -> {
                    ps.setString(1, agentId.value());
                    ps.setInt(2, health.value());
                    ps.setTimestamp(3, toTimeStamp(clock.now()));
                },

                "UPDATE " + agentTable + " SET health = ?, last_updated_at = ? WHERE id = ?",
                ps -> {
                    ps.setInt(1, health.value());
                    ps.setTimestamp(2, toTimeStamp(clock.now()));
                    ps.setString(3, agentId.value());
                },

                () -> "Unable to register health of agent '" + agentId + "'."
        );
    }

    @Override
    public boolean updateAgentsHealth(AgentId agentId, Health from, Health to, ZonedDateTime ifNotUpdatedSince) {
        int updatedRowCount = update(
                "UPDATE " + agentTable + " SET health = ?, last_updated_at = ? WHERE id = ? AND health = ? AND last_updated_at <= ?",
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
                "SELECT * FROM " + agentTable + " WHERE id = ?",
                ps -> ps.setString(1, agentId.value()),
                this::toAgent
        );
    }

    @Override
    public Set<Agent> findStaleLiveAgents(ZonedDateTime notUpdatedSince) {
        return selectDistinct(
                "SELECT * FROM " + agentTable + " WHERE last_updated_at <= ? AND health > 0",
                ps -> ps.setTimestamp(1, toTimeStamp(notUpdatedSince)),
                this::toAgent
        );
    }

    @Override
    public Set<Agent> findLiveAgents() {
        return selectDistinct("SELECT * FROM " + agentTable + " WHERE health > 0", this::toAgent);
    }

    @Override
    public Set<Agent> findDeadAgents() {
        return selectDistinct("SELECT * FROM " + agentTable + " WHERE health = 0", this::toAgent);
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

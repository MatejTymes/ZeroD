package zerod.dao;

import javafixes.object.Tuple;
import zerod.domain.AgentId;
import zerod.domain.Awareness;
import zerod.domain.MigrationId;
import zerod.domain.TableName;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static javafixes.object.Tuple.tuple;
import static zerod.domain.AgentId.agentId;
import static zerod.domain.Awareness.Aware;
import static zerod.domain.Awareness.NotAware;

public class SqlAwarenessDao extends BaseSqlDao implements AwarenessDao {

    private final TableName agentTable;
    private final TableName awarenessTable;

    public SqlAwarenessDao(TableName agentTable, TableName awarenessTable, DataSource dataSource) {
        super(dataSource);
        this.agentTable = agentTable;
        this.awarenessTable = awarenessTable;
    }

    // todo: add concurrency test
    @Override
    public void registerAwareness(MigrationId migrationId, AgentId agentId, Awareness awareness) {
        upsert(
                "SELECT * FROM " + awarenessTable + " WHERE migration_id = ? and agent_id = ?",
                ps -> {
                    ps.setString(1, migrationId.value());
                    ps.setString(2, agentId.value());
                },

                "INSERT INTO " + awarenessTable + " (migration_id, agent_id, is_aware_of) VALUES (?, ?, ?)",
                ps -> {
                    ps.setString(1, migrationId.value());
                    ps.setString(2, agentId.value());
                    ps.setBoolean(3, awareness.isAware);
                },

                "UPDATE " + awarenessTable + " SET is_aware_of = ? WHERE migration_id = ? and agent_id = ?",
                ps -> {
                    ps.setBoolean(1, awareness.isAware);
                    ps.setString(2, migrationId.value());
                    ps.setString(3, agentId.value());
                },

                () -> "Unable to register awareness of agent '" + agentId + "' for migration '" + migrationId + "'"
        );
    }

    @Override
    public Map<AgentId, Optional<Awareness>> findLiveAgentsAwareness(MigrationId migrationId) {
        return select("" +
                        "SELECT ag.id AS agentId, aw.is_aware_of AS isAwareOf " +
                        "FROM " + agentTable + " ag " +
                        "LEFT JOIN " + awarenessTable + " aw " +
                        "ON ag.id = aw.agent_id AND aw.migration_id = ?" +
                        "WHERE ag.health > 0",
                ps -> ps.setString(1, migrationId.value()),
                rs -> tuple(
                        agentId(rs.getString("agentId")),
                        Optional.ofNullable((Boolean) rs.getObject("isAwareOf")).map(value -> value ? Aware : NotAware)
                )
        ).stream().collect(toMap(Tuple::a, Tuple::b));
    }
}

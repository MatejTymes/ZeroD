package co.uk.zerod.dao;

import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Awareness;
import co.uk.zerod.domain.MigrationId;
import co.uk.zerod.domain.TableName;
import mtymes.javafixes.object.Tuple;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static co.uk.zerod.domain.AgentId.agentId;
import static co.uk.zerod.domain.Awareness.Aware;
import static co.uk.zerod.domain.Awareness.NotAware;
import static java.util.stream.Collectors.toMap;
import static mtymes.javafixes.object.Tuple.tuple;

public class SqlAwarenessDao extends BaseSqlDao implements AwarenessDao {

    private final TableName agentTable;
    private final TableName awarenessTable;

    public SqlAwarenessDao(TableName agentTable, TableName awarenessTable, DataSource dataSource) {
        super(dataSource);
        this.agentTable = agentTable;
        this.awarenessTable = awarenessTable;
    }

    // todo: add concurrency test
    // todo: test
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

    // todo: test
    @Override
    public Map<AgentId, Optional<Awareness>> findLiveAgentsAwareness(MigrationId migrationId, ZonedDateTime stillAliveAt) {
        return select("" +
                        "SELECT ag.id AS agentId, aw.is_aware_of AS isAwareOf " +
                        "FROM " + agentTable + " ag " +
                        "LEFT JOIN " + awarenessTable + " aw " +
                        "ON ag.id = aw.agent_id",
                rs -> tuple(
                        agentId(rs.getString("agentId")),
                        Optional.of((Boolean) rs.getObject("isAwareOf")).map(value -> value ? Aware : NotAware)
                )
        ).stream().collect(toMap(Tuple::a, Tuple::b));
    }
}

package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.domain.AgentName;
import co.uk.zerod.domain.TableName;

import javax.sql.DataSource;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.Set;

import static co.uk.zerod.domain.AgentName.agentName;
import static com.google.common.collect.Sets.newHashSet;

public class SqlAgentDao implements AgentDao {

    private final TableName agentTableName;

    private final DataSource dataSource;
    private final Clock clock;

    public SqlAgentDao(TableName agentTableName, DataSource dataSource, Clock clock) {
        this.agentTableName = agentTableName;
        this.dataSource = dataSource;
        this.clock = clock;
    }

    @Override
    public void registerHeartBeatFor(AgentName agentName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM " + agentTableName + " WHERE name = ?");
            selectStatement.setString(1, agentName.value());
            boolean exists;
            try (ResultSet rs = selectStatement.executeQuery()) {
                exists = rs.next();
            }

            boolean insertionFailed = false;
            if (!exists) {
                PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + agentTableName + " (name, last_heart_beat) VALUES (?, ?)");
                insertStatement.setString(1, agentName.value());
                insertStatement.setTimestamp(2, toTimeStamp(clock.now()));
                try {
                    insertionFailed = (insertStatement.executeUpdate() != 1);
                } catch (SQLException e) {
                    insertionFailed = true;
                }
            }

            if (exists || insertionFailed) {
                PreparedStatement updateStatement = connection.prepareStatement("UPDATE " + agentTableName + " SET last_heart_beat = ? WHERE name = ?");
                updateStatement.setTimestamp(1, toTimeStamp(clock.now()));
                updateStatement.setString(2, agentName.value());
                if (updateStatement.executeUpdate() != 1) {
                    throw new IllegalStateException("Unable to register agent '" + agentName + "' as active agent.");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set<AgentName> findAgentsAliveSince(ZonedDateTime activeSince) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + agentTableName + " WHERE last_heart_beat >= ?");
            ps.setTimestamp(1, toTimeStamp(activeSince));

            Set<AgentName> liveAgents = newHashSet();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liveAgents.add(agentName(rs.getString("name")));
                }
            }

            return liveAgents;

        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Timestamp toTimeStamp(ZonedDateTime dateTime) {
        return new Timestamp(dateTime.toInstant().toEpochMilli());
    }
}

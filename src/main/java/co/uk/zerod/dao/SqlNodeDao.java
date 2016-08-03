package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.domain.NodeName;
import co.uk.zerod.domain.TableName;

import javax.sql.DataSource;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.Set;

import static co.uk.zerod.domain.NodeName.nodeName;
import static com.google.common.collect.Sets.newHashSet;

public class SqlNodeDao implements NodeDao {

    private final TableName nodeTableName;

    private final DataSource dataSource;
    private final Clock clock;

    public SqlNodeDao(TableName nodeTableName, DataSource dataSource, Clock clock) {
        this.nodeTableName = nodeTableName;
        this.dataSource = dataSource;
        this.clock = clock;
    }

    @Override
    public void registerHeartBeatFor(NodeName nodeName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM " + nodeTableName + " WHERE node_name = ?");
            selectStatement.setString(1, nodeName.value());
            boolean exists;
            try (ResultSet rs = selectStatement.executeQuery()) {
                exists = rs.next();
            }

            boolean insertionFailed = false;
            if (!exists) {
                PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + nodeTableName + " (node_name, last_heart_beat) VALUES (?, ?)");
                insertStatement.setString(1, nodeName.value());
                insertStatement.setTimestamp(2, toTimeStamp(clock.now()));
                try {
                    insertionFailed = (insertStatement.executeUpdate() != 1);
                } catch (SQLException e) {
                    insertionFailed = true;
                }
            }

            if (exists || insertionFailed) {
                PreparedStatement updateStatement = connection.prepareStatement("UPDATE " + nodeTableName + " SET last_heart_beat = ? WHERE node_name = ?");
                updateStatement.setTimestamp(1, toTimeStamp(clock.now()));
                updateStatement.setString(2, nodeName.value());
                if (updateStatement.executeUpdate() != 1) {
                    throw new IllegalStateException("Unable to register node '" + nodeName + "' as active node.");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set<NodeName> findNodesAliveSince(ZonedDateTime activeSince) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + nodeTableName + " WHERE last_heart_beat >= ?");
            ps.setTimestamp(1, toTimeStamp(activeSince));

            Set<NodeName> liveNodes = newHashSet();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liveNodes.add(nodeName(rs.getString("node_name")));
                }
            }

            return liveNodes;

        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Timestamp toTimeStamp(ZonedDateTime dateTime) {
        return new Timestamp(dateTime.toInstant().toEpochMilli());
    }
}

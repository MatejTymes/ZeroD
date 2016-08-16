package co.uk.zerod.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

public abstract class BaseSqlDao {

    protected final DataSource dataSource;

    public BaseSqlDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected <T> List<T> select(String query, StatementFiller statementFiller, ResultSetMapper<T> itemMapper) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(query);

            statementFiller.fillParams(ps);

            List<T> items = newArrayList();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(itemMapper.map(rs));
                }
            }

            return items;

        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected <T> List<T> select(String query, ResultSetMapper<T> resultMapper) {
        return select(query, ps -> {
        }, resultMapper);
    }

    protected <T> Set<T> selectDistinct(String query, StatementFiller statementFiller, ResultSetMapper<T> itemMapper) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(query);

            statementFiller.fillParams(ps);

            Set<T> items = newLinkedHashSet();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(itemMapper.map(rs));
                }
            }

            return items;

        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected <T> Set<T> selectDistinct(String query, ResultSetMapper<T> resultMapper) {
        return selectDistinct(query, ps -> {
        }, resultMapper);
    }

    protected <T> Optional<T> selectSingle(String query, StatementFiller statementFiller, ResultSetMapper<T> itemMapper) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(query);

            statementFiller.fillParams(ps);

            Optional<T> item = Optional.empty();
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item = Optional.of(itemMapper.map(rs));
                }
                if (rs.next()) {
                    throw new IllegalArgumentException("More than one item found");
                }
            }

            return item;

        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected int update(String query, StatementFiller statementFiller) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    query
            );
            statementFiller.fillParams(ps);

            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void upsert(
            String existenceSelect, StatementFiller existenceStatementFiller,
            String insert, StatementFiller insertStatementFiller,
            String update, StatementFiller updateStatementFiller,
            Supplier<String> failureMessageSupplier
    ) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement selectStatement = connection.prepareStatement(
                    existenceSelect
            );
            existenceStatementFiller.fillParams(selectStatement);
            boolean exists;
            try (ResultSet rs = selectStatement.executeQuery()) {
                exists = rs.next();
            }

            boolean insertionFailed = false;
            if (!exists) {
                PreparedStatement insertStatement = connection.prepareStatement(
                        insert
                );
                insertStatementFiller.fillParams(insertStatement);
                try {
                    insertionFailed = (insertStatement.executeUpdate() != 1);
                } catch (SQLException e) {
                    insertionFailed = true;
                }
            }

            if (exists || insertionFailed) {
                PreparedStatement updateStatement = connection.prepareStatement(
                        update
                );
                updateStatementFiller.fillParams(updateStatement);
                if (updateStatement.executeUpdate() != 1) {
                    throw new IllegalStateException(failureMessageSupplier.get());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public interface StatementFiller {

        void fillParams(PreparedStatement ps) throws SQLException;
    }

    public interface ResultSetMapper<T> {

        T map(ResultSet resultSet) throws SQLException;
    }
}

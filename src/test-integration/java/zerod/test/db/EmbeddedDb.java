package zerod.test.db;

import com.mchange.v2.c3p0.PooledDataSource;
import org.h2.tools.RunScript;
import org.h2.tools.Server;
import org.slf4j.Logger;

import java.beans.PropertyVetoException;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.ClassLoader.getSystemResource;
import static org.slf4j.LoggerFactory.getLogger;
import static zerod.test.common.JdbcUtil.createPooledDataSource;
import static zerod.test.common.TcpUtil.getFreeServerPort;

public class EmbeddedDb implements Closeable {

    private static final Logger LOG = getLogger(EmbeddedDb.class);

    private final File sqlSciptsDir;
    private final int dbPort;
    private final String dbName;

    private Server tcpServer;
    private PooledDataSource dataSource;

    private EmbeddedDb(File sqlSciptsDir, int dbPort, String dbName) {
        this.sqlSciptsDir = sqlSciptsDir;
        this.dbPort = dbPort;
        this.dbName = dbName;
    }

    public static EmbeddedDb createDb(File sqlSciptsDir, int dbPort, String dbName) {
        EmbeddedDb embeddedDb = new EmbeddedDb(sqlSciptsDir, dbPort, dbName);
        try {
            return embeddedDb.initAndStart();
        } catch (Exception e) {
            embeddedDb.close();
            throw e;
        }
    }

    public static EmbeddedDb createDb(File sqlSciptsDir) {
        return createDb(sqlSciptsDir, getFreeServerPort(), "tempDb-" + System.currentTimeMillis());
    }

    public static EmbeddedDb createDb() {
        return createDb(new File(getSystemResource("sql-scripts").getFile()));
    }

    public PooledDataSource getDataSource() {
        assertIsRunning();

        return dataSource;
    }

    public String jdbcDriver() {
        return "org.h2.Driver";
    }

    public String jdbcUrl() {
        return "jdbc:h2:tcp://localhost:" + dbPort + "/" + dbName;
    }

    public Optional<String> jdbcUser() {
        return Optional.empty();
    }

    public Optional<String> jdbcPassword() {
        return Optional.empty();
    }

    public EmbeddedDb reinitializeDb() {
        assertIsRunning();

        try (Connection connection = dataSource.getConnection()) {

            truncateTables(connection);
            runDataScripts(sqlSciptsDir, connection);

        } catch (FileNotFoundException | SQLException e) {
            LOG.error("Failed to reinitialize db", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public void close() {
        if (isRunning()) {
            try {
                tcpServer.stop();
            } finally {
                dataSource = null;
                tcpServer = null;
            }
        }
    }

    private EmbeddedDb initAndStart() {
        assertIsNotRunning();

        try {
            tcpServer = Server.createTcpServer(
                    "-tcpPort", Integer.toString(dbPort),
                    "-baseDir", "./db"
            ).start();

            dataSource = createPooledDataSource(jdbcDriver(), jdbcUrl(), jdbcUser(), jdbcPassword(), 5);

            try (Connection connection = dataSource.getConnection()) {
                runSchemaScripts(sqlSciptsDir, connection);
                runDataScripts(sqlSciptsDir, connection);
            }

            return this;

        } catch (PropertyVetoException | FileNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void runSchemaScripts(File sqlSciptsDir, Connection connection) throws FileNotFoundException, SQLException {
        File schemaDir = new File(sqlSciptsDir, "schema");
        if (!schemaDir.exists()) {
            LOG.warn("Sql schema directory doesn't exist: " + schemaDir.getAbsolutePath());
            return;
        }

        List<File> scriptFiles = newArrayList();
        scriptFiles.addAll(newArrayList(schemaDir.listFiles((dir, filename) -> filename.endsWith(".sql"))));
        if (scriptFiles.size() == 0) {
            LOG.warn("No sql schema script found in dir: " + schemaDir.getAbsolutePath());
        }

        for (File scriptFile : scriptFiles) {
            RunScript.execute(connection, new FileReader(scriptFile));
        }
    }

    private static void runDataScripts(File sqlSciptsDir, Connection connection) throws FileNotFoundException, SQLException {
        File dataDir = new File(sqlSciptsDir, "data");
        if (!dataDir.exists()) {
            return;
        }

        List<File> scriptFiles = newArrayList();
        scriptFiles.addAll(newArrayList(dataDir.listFiles((dir, filename) -> filename.endsWith(".sql"))));

        for (File scriptFile : scriptFiles) {
            RunScript.execute(connection, new FileReader(scriptFile));
        }
    }

    private static void truncateTables(Connection connection) throws SQLException {
        List<String> tableNames = newArrayList();

        try (ResultSet rs = connection.getMetaData().getTables(null, "PUBLIC", "%", null)) {
            while (rs.next()) {
                tableNames.add(rs.getString(3));
            }
        }

        connection.prepareCall("SET REFERENTIAL_INTEGRITY FALSE").execute();
        for (String tableName : tableNames) {
            connection.prepareCall("TRUNCATE TABLE " + tableName).execute();
        }
        connection.prepareCall("SET REFERENTIAL_INTEGRITY TRUE").execute();
    }

    private boolean isRunning() {
        return tcpServer != null && tcpServer.isRunning(true);
    }

    private void assertIsNotRunning() {
//        if (dataSource != null) {
        if (isRunning()) {
            throw new IllegalStateException("This operation can't be executed while EmbeddedDb is running");
        }
    }

    private void assertIsRunning() {
//        if (dataSource == null) {
        if (!isRunning()) {
            throw new IllegalStateException("This operation can't be executed while EmbeddedDb is NOT running");
        }
    }
}

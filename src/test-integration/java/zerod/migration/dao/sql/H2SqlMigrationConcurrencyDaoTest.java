package zerod.migration.dao.sql;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import zerod.beta.migration.dao.MigrationDao;
import zerod.beta.migration.dao.sql.SqlMigrationDao;
import zerod.migration.dao.MigrationDaoConcurrencyTestBase;
import zerod.test.db.EmbeddedDb;

import javax.sql.DataSource;

import static zerod.beta.domain.TableName.tableName;
import static zerod.test.db.EmbeddedDb.createDb;

public class H2SqlMigrationConcurrencyDaoTest extends MigrationDaoConcurrencyTestBase {

    private static EmbeddedDb db;
    private static DataSource dataSource;

    @BeforeClass
    public static void setUp() throws Exception {
        db = createDb();
        dataSource = db.getDataSource();
    }

    @Before
    public void resetDb() {
        db.reinitializeDb();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        db.close();
    }

    @Override
    protected MigrationDao getDao() {
        return new SqlMigrationDao(tableName("zd_migration"), dataSource);
    }
}

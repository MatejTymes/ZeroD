package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.test.db.EmbeddedDb;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.sql.DataSource;

import static co.uk.zerod.domain.TableName.tableName;
import static co.uk.zerod.test.db.EmbeddedDb.createDb;

public class H2SqlAgentDaoTest extends AgentDaoTestBase {

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
    protected AgentDao getDao() {
        return new SqlAgentDao(tableName("zd_agent"), dataSource, new Clock());
    }
}

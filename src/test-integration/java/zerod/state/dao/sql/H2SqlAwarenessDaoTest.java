package zerod.state.dao.sql;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import zerod.beta.agent.dao.AgentDao;
import zerod.beta.agent.dao.sql.SqlAgentDao;
import zerod.beta.awareness.dao.AwarenessDao;
import zerod.beta.awareness.dao.sql.SqlAwarenessDao;
import zerod.beta.common.Clock;
import zerod.state.dao.AwarenessDaoTestBase;
import zerod.test.db.EmbeddedDb;

import javax.sql.DataSource;

import static zerod.beta.domain.TableName.tableName;
import static zerod.test.db.EmbeddedDb.createDb;

public class H2SqlAwarenessDaoTest extends AwarenessDaoTestBase {

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
    protected AgentDao getAgentDao() {
        return new SqlAgentDao(tableName("zd_agent"), dataSource, new Clock());
    }

    @Override
    protected AwarenessDao getDao() {
        return new SqlAwarenessDao(tableName("zd_agent"), tableName("zd_awareness"), dataSource);
    }
}

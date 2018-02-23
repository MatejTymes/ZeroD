package zerod.state.dao;

import javafixes.concurrency.Runner;
import org.junit.Test;
import zerod.beta.migration.dao.MigrationDao;
import zerod.migration.domain.MigrationId;

import java.util.concurrent.CyclicBarrier;

import static javafixes.concurrency.Runner.runner;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static zerod.test.Random.randomMigrationId;

public abstract class MigrationDaoConcurrencyTestBase {

    private MigrationDao dao = getDao();

    @Test
    public void shouldNotFailIfTheSameMigrationIsRegisteredConcurrently() throws Exception {
        int attemptsCount = 10;
        int concurrentThreadCount = 30;

        Runner runner = runner(concurrentThreadCount);

        try {
            for (int attempt = 1; attempt <= attemptsCount; attempt++) {
                MigrationId migrationId = randomMigrationId();

                // When
                CyclicBarrier synchronizedStartBarrier = new CyclicBarrier(concurrentThreadCount);
                for (int i = 0; i < concurrentThreadCount; i++) {
                    runner.runTask(() -> {
                        synchronizedStartBarrier.await();

                        dao.registerMigration(migrationId);
                    });
                }
                runner.waitTillDone();

                // Then
                assertThat(
                        attempt + ". attempt - there should be no failures",

                        runner.failedCount(), is(0)
                );
                assertThat(dao.findAllMigrations(), hasItem(migrationId));
            }
        } finally {
            runner.shutdownNow();
        }
    }


    protected abstract MigrationDao getDao();
}
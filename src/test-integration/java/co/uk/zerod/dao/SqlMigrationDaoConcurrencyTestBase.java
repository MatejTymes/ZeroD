package co.uk.zerod.dao;

import co.uk.zerod.domain.MigrationId;
import external.mtymes.javafixes.concurrency.Runner;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.concurrent.CyclicBarrier;

import static co.uk.zerod.domain.TableName.tableName;
import static co.uk.zerod.test.Random.randomMigrationId;
import static external.mtymes.javafixes.concurrency.Runner.runner;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class SqlMigrationDaoConcurrencyTestBase {

    private SqlMigrationDao dao = new SqlMigrationDao(tableName("zd_migration"), getDataSource());

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
            }
        } finally {
            runner.shutdownNow();
        }
    }


    protected abstract DataSource getDataSource();
}
package co.uk.zerod.dao;

import co.uk.zerod.domain.MigrationId;
import mtymes.javafixes.concurrency.Runner;
import org.junit.Test;

import java.util.concurrent.CyclicBarrier;

import static co.uk.zerod.test.Random.randomMigrationId;
import static mtymes.javafixes.concurrency.Runner.runner;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
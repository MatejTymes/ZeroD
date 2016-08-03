package co.uk.zerod.dao;

import co.uk.zerod.wip.MigrationId;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static co.uk.zerod.domain.TableName.tableName;
import static co.uk.zerod.wip.MigrationId.migrationId;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class SqlMigrationDaoTestBase {

    private SqlMigrationDao dao = new SqlMigrationDao(tableName("migration"), getDataSource());

    @Test
    public void shouldNotFindAnyMigrationsInEmptyDb() {
        assertThat(dao.findAllMigrations(), is(empty()));
    }

    @Test
    public void shouldFailIfUnableToRegisterMigration() {
        MigrationId migrationId = migrationId("really long migration id that definitely won't fit into the database");

        try {
            // When
            dao.registerMigration(migrationId);

            // Then
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // this is expected
        }
    }

    @Test
    public void shouldBeAbleToRegisterValidMigration() {
        MigrationId migrationId = randomMigrationId();

        // When
        dao.registerMigration(migrationId);

        // Then
        assertThat(dao.findAllMigrations(), equalTo(newHashSet(migrationId)));
    }

    @Test
    public void shouldIngoreWhenRegisteringExistingMigration() {
        MigrationId migrationId = randomMigrationId();
        dao.registerMigration(migrationId);

        // When
        dao.registerMigration(migrationId);

        // Then
        assertThat(dao.findAllMigrations(), equalTo(newHashSet(migrationId)));
    }

    @Test
    public void shouldFindAllRegisteredMigrations() {
        MigrationId migrationId1 = randomMigrationId();
        MigrationId migrationId2 = randomMigrationId();
        MigrationId migrationId3 = randomMigrationId();
        dao.registerMigration(migrationId1);
        dao.registerMigration(migrationId2);
        dao.registerMigration(migrationId3);

        assertThat(dao.findAllMigrations(), equalTo(newHashSet(migrationId1, migrationId2, migrationId3)));
    }

    @Test
    public void shouldNotFailIfTheSameMigrationIsRegisteredConcurrently() throws Exception {
        // todo: use Runner from JavaFixes to simplify this test
        int concurrentThreadCount = 30;
        int retryCount = 50;
        ScheduledExecutorService executor = newScheduledThreadPool(concurrentThreadCount);

        for (int attempt = 0; attempt < retryCount; attempt++) {
            MigrationId migrationId = randomMigrationId();

            CyclicBarrier startBarrier = new CyclicBarrier(concurrentThreadCount);
            CountDownLatch finishedCounter = new CountDownLatch(concurrentThreadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < concurrentThreadCount; i++) {
                executor.submit(() -> {
                    try {
                        startBarrier.await();

                        dao.registerMigration(migrationId);

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        e.printStackTrace();
                    } finally {
                        finishedCounter.countDown();
                    }
                    return null;
                });
            }

            finishedCounter.await();

            assertThat(errorCount.get(), is(0));
        }
    }

    // todo: move into some util class
    private static MigrationId randomMigrationId() {
        return migrationId(randomUUID().toString());
    }


    protected abstract DataSource getDataSource();
}
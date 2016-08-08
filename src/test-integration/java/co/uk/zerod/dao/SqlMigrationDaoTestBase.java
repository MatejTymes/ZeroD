package co.uk.zerod.dao;

import co.uk.zerod.wip.MigrationId;
import external.mtymes.javafixes.concurrency.Runner;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.concurrent.CyclicBarrier;

import static co.uk.zerod.domain.TableName.tableName;
import static co.uk.zerod.test.Random.randomMigrationId;
import static co.uk.zerod.wip.MigrationId.migrationId;
import static com.google.common.collect.Sets.newHashSet;
import static external.mtymes.javafixes.concurrency.Runner.runner;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class SqlMigrationDaoTestBase {

    private SqlMigrationDao dao = new SqlMigrationDao(tableName("zd_migration"), getDataSource());

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
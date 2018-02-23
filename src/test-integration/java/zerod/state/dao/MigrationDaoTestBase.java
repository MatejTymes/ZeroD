package zerod.state.dao;

import org.junit.Test;
import zerod.beta.migration.dao.MigrationDao;
import zerod.migration.domain.MigrationId;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.internal.util.collections.Sets.newSet;
import static zerod.migration.domain.MigrationId.migrationId;
import static zerod.test.Condition.otherThan;
import static zerod.test.Random.randomMigrationId;

public abstract class MigrationDaoTestBase {

    private MigrationDao dao = getDao();

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
        MigrationId migrationId2 = randomMigrationId(otherThan(migrationId1));
        MigrationId migrationId3 = randomMigrationId(otherThan(migrationId1, migrationId2));
        dao.registerMigration(migrationId1);
        dao.registerMigration(migrationId2);
        dao.registerMigration(migrationId3);

        assertThat(dao.findAllMigrations(), equalTo(newSet(migrationId1, migrationId2, migrationId3)));
    }


    protected abstract MigrationDao getDao();
}
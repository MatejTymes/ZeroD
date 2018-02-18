package zerod.sample;

import zerod.beta.MigrationCoordinator;
import zerod.sample.dao.UserDao;
import zerod.sample.dao.VersionedStore;
import zerod.sample.domain.UserId;

import static zerod.beta.MigrationStepsBuilder.migrationStepsBuilder;
import static zerod.sample.MigrationHelper.*;

public class Node {

    public final UserDao userDao;

    private Node(UserDao userDao) {
        this.userDao = userDao;
    }

    public static Node startNode(VersionedStore<UserId> storage) {
        MigrationCoordinator coordinator = new MigrationCoordinator();

        coordinator.registerMigration(
                FULL_NAME_MIGRATION,
                migrationStepsBuilder()
                        .toBeAbleToReadNew(() -> addMissingFirstAndLastNameValues(storage))
                        .onceWeReadAndWriteOnlyNew(() -> removeFullNameField(storage))
                        .build()
        );

        return new Node(
                new UserDao(storage, coordinator.getAccessGuide()));
    }

}

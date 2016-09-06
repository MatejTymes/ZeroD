package zerod.sample;

import zerod.sample.dao.UserDao;
import zerod.sample.dao.VersionedStore;
import zerod.sample.domain.UserId;
import zerod.wip.MigrationCoordinator;

import static zerod.sample.MigrationHelper.*;
import static zerod.wip.MigrationStepsBuilder.migrationStepsBuilder;

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

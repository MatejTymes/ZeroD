package co.uk.zerod.sample;

import co.uk.zerod.sample.dao.UserDao;
import co.uk.zerod.sample.dao.VersionedStore;
import co.uk.zerod.sample.domain.UserId;
import co.uk.zerod.wip.MigrationCoordinator;

import static co.uk.zerod.sample.MigrationHelper.*;
import static co.uk.zerod.wip.MigrationStepsBuilder.migrationStepsBuilder;

public class Node {

    public final UserDao userDao;

    private Node(UserDao userDao) {
        this.userDao = userDao;
    }

    public static Node startNode(VersionedStore<UserId> storage) {
        MigrationCoordinator coordinator = new MigrationCoordinator();

        coordinator.registerMigrationStepsFor(
                FULL_NAME_MIGRATION,
                migrationStepsBuilder()
                        .toBeAbleToReadNew(() -> addMissingFirstAndLastNameValues(storage))
                        .onceWeReadAndWriteOnlyNew(() -> removeFullNameField(storage))
                        .build()
        );

        UserDao userDao = new UserDao(storage, coordinator.getAccessGuide());

        return new Node(userDao);
    }

}

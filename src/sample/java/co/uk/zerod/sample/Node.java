package co.uk.zerod.sample;

import co.uk.zerod.sample.dao.UserDao;
import co.uk.zerod.sample.dao.VersionedStore;
import co.uk.zerod.sample.domain.UserId;
import co.uk.zerod.wip.MigrationCoordinator;

import java.util.Optional;

import static co.uk.zerod.ReadWriteState.ReadNew_WriteNew;
import static co.uk.zerod.ReadWriteState.ReadOld_WriteBoth;
import static co.uk.zerod.sample.MigrationIds.FULL_NAME_MIGRATION;
import static co.uk.zerod.wip.MigrationConfigBuilder.afterPhaseRun;

public class Node {

    public final UserDao userDao;

    private Node(UserDao userDao) {
        this.userDao = userDao;
    }

    public static Node startNode(VersionedStore<UserId> storage) {
        MigrationCoordinator coordinator = new MigrationCoordinator();

        coordinator.registerMigrationHandler(
                FULL_NAME_MIGRATION,

                afterPhaseRun(ReadOld_WriteBoth, () -> {

                    // back-populate missing firstName and lastName

                    for (UserId userId : storage.keySet()) {
                        storage.conditionalUpdate(userId, userValues -> {
                            if (userValues.containsKey("firstName") || userValues.containsKey("lastName")) {
                                return Optional.empty();
                            } else {
                                String fullName = (String) userValues.get("fullName");

                                String nameParts[] = fullName.split(" ");
                                userValues.put("firstName", nameParts[0]);
                                userValues.put("lastName", nameParts[1]);


                                return Optional.of(userValues);
                            }
                        });
                    }
                }).andAfterPhaseRun(ReadNew_WriteNew, () -> {

                    // remove fullName fields

                    for (UserId userId : storage.keySet()) {

                        storage.conditionalUpdate(userId, userValues -> {
                            if (!userValues.containsKey("fullName")) {
                                return Optional.empty();
                            } else {
                                userValues.remove("fullName");
                                return Optional.of(userValues);
                            }
                        });

                    }
                }).build()
        );

        UserDao userDao = new UserDao(storage, coordinator.getAccessGuide());

        return new Node(userDao);
    }
}

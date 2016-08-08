package co.uk.zerod.sample;

import co.uk.zerod.domain.MigrationId;
import co.uk.zerod.sample.dao.VersionedStore;
import co.uk.zerod.sample.domain.UserId;

import java.util.Optional;

import static co.uk.zerod.domain.MigrationId.migrationId;

public class MigrationHelper {

    public static final MigrationId FULL_NAME_MIGRATION = migrationId("fullNameMigration");

    public static void addMissingFirstAndLastNameValues(VersionedStore<UserId> storage) {
        for (UserId userId : storage.keySet()) {

            storage.conditionalUpdate(userId, userValues -> {

                if (userValues.containsKey("firstName") || userValues.containsKey("lastName")) {
                    return noChangeNeeded();
                } else {
                    String fullName = (String) userValues.get("fullName");

                    String nameParts[] = fullName.split(" ");
                    userValues.put("firstName", nameParts[0]);
                    userValues.put("lastName", nameParts[1]);


                    return changeTo(userValues);
                }
            });
        }
    }

    public static void removeFullNameField(VersionedStore<UserId> storage) {
        for (UserId userId : storage.keySet()) {

            storage.conditionalUpdate(userId, userValues -> {

                if (!userValues.containsKey("fullName")) {
                    return noChangeNeeded();
                } else {
                    userValues.remove("fullName");
                    return changeTo(userValues);
                }
            });
        }
    }

    private static Optional noChangeNeeded() {
        return Optional.empty();
    }

    private static <T> Optional<T> changeTo(T newValue) {
        return Optional.of(newValue);
    }
}

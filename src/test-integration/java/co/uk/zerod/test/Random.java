package co.uk.zerod.test;

import co.uk.zerod.wip.MigrationId;

import static co.uk.zerod.wip.MigrationId.migrationId;
import static java.util.UUID.randomUUID;

// todo: merge both Randoms into one test code
public class Random {

    public static String randomUUIDString() {
        return randomUUID().toString();
    }

    public static MigrationId randomMigrationId() {
        return migrationId(randomUUIDString());
    }
}
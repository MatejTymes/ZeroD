package co.uk.zerod.wip;

import external.mtymes.javafixes.object.Microtype;

public class MigrationId extends Microtype<String> {

    public MigrationId(String value) {
        super(value);
    }

    public static MigrationId migrationId(String value) {
        return new MigrationId(value);
    }
}

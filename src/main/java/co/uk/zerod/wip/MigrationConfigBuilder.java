package co.uk.zerod.wip;

import co.uk.zerod.ReadWriteState;
import external.mtymes.javafixes.concurrency.Task;

import static co.uk.zerod.ReadWriteState.*;

public class MigrationConfigBuilder {

    // todo: should be able to register phase
    // pre-migration code  - all nodes will run it
    // migration code      - only single node will run it
    // post-migration code - all nodes will run it

    public static MigrationConfigBuilder migrationConfigBuilder() {
        return new MigrationConfigBuilder();
    }

    public MigrationConfigBuilder afterPhase(ReadWriteState state, Task task) {
        // todo: implement
        throw new UnsupportedOperationException("implement");
    }

    public MigrationConfigBuilder toBeAbleToWriteOldAndNew(Task task) {
        return afterPhase(ReadOld_WriteOld, task);
    }

    public MigrationConfigBuilder toBeAbleToReadNew(Task task) {
        return afterPhase(ReadOld_WriteBoth, task);
    }

    public MigrationConfigBuilder toBeAbleToNotWriteNew(Task task) {
        return afterPhase(ReadNew_WriteBoth, task);
    }

    public MigrationConfigBuilder onceWeReadAndWriteOnlyNew(Task task) {
        return afterPhase(ReadNew_WriteNew, task);
    }

    public MigrationConfig build() {
        // todo: implement
        throw new UnsupportedOperationException("implement");
    }
}

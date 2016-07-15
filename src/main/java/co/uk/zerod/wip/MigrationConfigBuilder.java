package co.uk.zerod.wip;

import co.uk.zerod.ReadWriteState;
import external.mtymes.javafixes.concurrency.Task;

public class MigrationConfigBuilder {

    // todo: should be able to register phase
    // pre-migration code  - all nodes will run it
    // migration code      - only single node will run it
    // post-migration code - all nodes will run it

    public static MigrationConfigBuilder afterPhaseRun(ReadWriteState state, Task task) {
        // todo: implement
        throw new UnsupportedOperationException("implement");
    }

    public MigrationConfigBuilder andAfterPhaseRun(ReadWriteState state, Task task) {
        // todo: implement
        throw new UnsupportedOperationException("implement");
    }

    public MigrationConfig build() {
        // todo: implement
        throw new UnsupportedOperationException("implement");
    }
}

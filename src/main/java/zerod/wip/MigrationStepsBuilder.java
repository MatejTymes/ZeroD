package zerod.wip;

import javafixes.concurrency.Task;
import zerod.state.ReadWriteState;

import static zerod.state.ReadWriteState.*;

public class MigrationStepsBuilder {

    // todo: should be able to register phase
    // pre-migration code  - all agents will run it
    // migration code      - only single agent will run it
    // post-migration code - all agents will run it

    public static MigrationStepsBuilder migrationStepsBuilder() {
        return new MigrationStepsBuilder();
    }

    public MigrationStepsBuilder afterPhase(ReadWriteState state, Task task) {
        // todo: implement
        throw new UnsupportedOperationException("implement");
    }

    public MigrationStepsBuilder toBeAbleToWriteOldAndNew(Task task) {
        return afterPhase(ReadOld_WriteOld, task);
    }

    public MigrationStepsBuilder toBeAbleToReadNew(Task task) {
        return afterPhase(ReadOld_WriteBoth, task);
    }

    public MigrationStepsBuilder toBeAbleToNotWriteNew(Task task) {
        return afterPhase(ReadNew_WriteBoth, task);
    }

    public MigrationStepsBuilder onceWeReadAndWriteOnlyNew(Task task) {
        return afterPhase(ReadNew_WriteNew, task);
    }

    public MigrationSteps build() {
        // todo: implement
        throw new UnsupportedOperationException("implement");
    }
}

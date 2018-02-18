package zerod.domain;

import javafixes.object.DataObject;
import zerod.state.ReadWriteState;
import zerod.state.StateTransitioner;

public class MigrationConfig extends DataObject {

    public final ReadWriteState initialState;
    public final StateTransitioner stateTransitioner;

    public MigrationConfig(ReadWriteState initialState, StateTransitioner stateTransitioner) {
        this.initialState = initialState;
        this.stateTransitioner = stateTransitioner;
    }

    public static final MigrationConfig migrationConfig(ReadWriteState initialState, StateTransitioner stateTransitioner) {
        return new MigrationConfig(initialState, stateTransitioner);
    }
}

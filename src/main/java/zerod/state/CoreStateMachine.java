package zerod.state;

public class CoreStateMachine implements StateMachine<ReadWriteState> {

    // todo: add possibility to transition to previous states (state revert)
    // todo: test
    @Override
    public boolean canTransitionFromTo(ReadWriteState fromState, ReadWriteState toState) {
        return fromState.ordinal() == toState.ordinal() || fromState.ordinal() + 1 == toState.ordinal();
    }
}

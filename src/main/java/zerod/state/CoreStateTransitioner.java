package zerod.state;

public class CoreStateTransitioner implements StateTransitioner {

    private volatile boolean isRevertMode = false;

    // todo: add possibility to transition to previous states (state revert)
    // todo: test
    @Override
    public boolean canTransitionFromTo(ReadWriteState fromState, ReadWriteState toState) {
        return fromState.ordinal() == toState.ordinal()
                || (!isRevertMode && fromState.ordinal() + 1 == toState.ordinal())
                || (isRevertMode && fromState.ordinal() - 1 == toState.ordinal());
    }
}

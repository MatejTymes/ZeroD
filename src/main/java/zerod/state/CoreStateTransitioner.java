package zerod.state;

import zerod.state.domain.ReadWriteState;

public class CoreStateTransitioner implements StateTransitioner {

    @Override
    public boolean canTransitionFromTo(ReadWriteState fromState, ReadWriteState toState) {
        return fromState.ordinal() == toState.ordinal()
                || fromState.ordinal() + 1 == toState.ordinal()
                || fromState.ordinal() - 1 == toState.ordinal();
    }
}

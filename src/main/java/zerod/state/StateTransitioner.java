package zerod.state;

import zerod.state.domain.ReadWriteState;

public interface StateTransitioner {

    boolean canTransitionFromTo(ReadWriteState fromState, ReadWriteState toState);
}

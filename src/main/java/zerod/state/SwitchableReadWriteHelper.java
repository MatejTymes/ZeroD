package zerod.state;

import zerod.state.domain.ReadWriteState;

public interface SwitchableReadWriteHelper extends ReadWriteHelper {

    void switchState(ReadWriteState toState) throws IllegalStateException;

    ReadWriteState getCurrentState();

    ReadWriteState getTransitionToState();
}

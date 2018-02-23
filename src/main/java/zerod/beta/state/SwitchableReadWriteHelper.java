package zerod.beta.state;

import zerod.state.ReadWriteState;

public interface SwitchableReadWriteHelper extends ReadWriteHelper {

    void switchState(ReadWriteState toState) throws IllegalStateException;

    ReadWriteState getCurrentState();

    ReadWriteState getTransitionToState();
}

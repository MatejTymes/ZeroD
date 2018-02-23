package zerod.beta.guide;

import zerod.state.domain.ReadWriteState;

// todo: add javadoc
public interface SwitchableReadWriteGuide extends ReadWriteGuide {

    void switchState(ReadWriteState toState) throws IllegalStateException;

    ReadWriteState getCurrentState();

    ReadWriteState getTransitionToState();
}

package zerod.guide;

import zerod.state.ReadWriteState;

public interface SwitchableReadWriteGuide extends ReadWriteGuide {

    // todo: add javadoc
    void switchState(ReadWriteState toState) throws IllegalStateException;
}

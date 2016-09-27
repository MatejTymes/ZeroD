package zerod.guide;

import zerod.state.ReadWriteState;

public interface SwitchableReadWriteGuide extends ReadWriteGuide {

    void switchState(ReadWriteState toState);
}

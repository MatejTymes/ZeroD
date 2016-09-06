package zerod;

import static zerod.ReadState.ReadNew;
import static zerod.ReadState.ReadOld;
import static zerod.WriteState.*;

public enum ReadWriteState {

    ReadOld_WriteOld(ReadOld, WriteOld),
    ReadOld_WriteBoth(ReadOld, WriteBoth),
    ReadNew_WriteBoth(ReadNew, WriteBoth),
    ReadNew_WriteNew(ReadNew, WriteNew);

    public final ReadState readState;
    public final WriteState writeState;

    ReadWriteState(ReadState readState, WriteState writeState) {
        this.readState = readState;
        this.writeState = writeState;
    }
}

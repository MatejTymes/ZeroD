package co.uk.zerod;

import static co.uk.zerod.ReadState.ReadNew;
import static co.uk.zerod.ReadState.ReadOld;
import static co.uk.zerod.WriteState.WriteBoth;
import static co.uk.zerod.WriteState.WriteNew;
import static co.uk.zerod.WriteState.WriteOld;

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

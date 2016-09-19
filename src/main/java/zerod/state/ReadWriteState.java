package zerod.state;

public enum ReadWriteState {

    ReadOld_WriteOld(ReadState.ReadOld, WriteState.WriteOld),
    ReadOld_WriteBoth(ReadState.ReadOld, WriteState.WriteBoth),
    ReadNew_WriteBoth(ReadState.ReadNew, WriteState.WriteBoth),
    ReadNew_WriteNew(ReadState.ReadNew, WriteState.WriteNew);

    public final ReadState readState;
    public final WriteState writeState;

    ReadWriteState(ReadState readState, WriteState writeState) {
        this.readState = readState;
        this.writeState = writeState;
    }
}

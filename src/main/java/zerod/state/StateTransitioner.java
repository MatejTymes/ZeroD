package zerod.state;

public interface StateTransitioner {

    boolean canTransitionFromTo(ReadWriteState fromState, ReadWriteState toState);
}

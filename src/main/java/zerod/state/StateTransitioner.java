package zerod.state;

public interface StateTransitioner<T> {

    boolean canTransitionFromTo(T fromState, T toState);
}

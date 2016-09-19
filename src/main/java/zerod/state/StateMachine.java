package zerod.state;

public interface StateMachine<T> {

    boolean canTransitionFromTo(T fromState, T toState);
}

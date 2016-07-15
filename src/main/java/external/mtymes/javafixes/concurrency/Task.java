package external.mtymes.javafixes.concurrency;

/**
 * @author mtymes
 * @since 10/22/14 11:07 PM
 */
// todo: taken from JavaFixes - add dependency instead
public interface Task {

    // same as runnable, but can throw exception
    void run() throws Exception;
}

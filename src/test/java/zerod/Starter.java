package zerod;

import java.util.concurrent.CountDownLatch;

public class Starter {
    private final CountDownLatch startCountDown = new CountDownLatch(1);

    private Starter() {
    }

    public static Starter starter() {
        return new Starter();
    }

    public void start() {
        startCountDown.countDown();
    }

    public void waitForStartSignal() throws InterruptedException {
        startCountDown.await();
    }

}

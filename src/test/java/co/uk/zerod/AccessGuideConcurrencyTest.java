package co.uk.zerod;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static co.uk.zerod.ReadWriteState.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

public class AccessGuideConcurrencyTest {

    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(1, SECONDS);
    }

    @Test
    public void shouldBlockStateSwitchToIfActiveReaderHasDifferentReadState() throws Exception {
        AccessGuide guide = new AccessGuide(ReadOld_WriteBoth);

        long timeoutDurationMs = 300;
        CountDownLatch startReadTimeout = new CountDownLatch(1);

        readOnNewThread(guide, readState -> {

            startReadTimeout.await();
            Thread.sleep(timeoutDurationMs);
        });

        // When
        long startTime = System.currentTimeMillis();
        startReadTimeout.countDown();

        guide.switchState(ReadNew_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(timeoutDurationMs));
    }

    @Test
    public void shouldBlockStateSwitchToIfActiveWriterHasDifferentWriteState1() {
        AccessGuide guide = new AccessGuide(ReadOld_WriteOld);

        long timeoutDurationMs = 300;
        CountDownLatch startWriteTimeout = new CountDownLatch(1);
        writeOnNewThread(guide, writeState -> {

            startWriteTimeout.await();
            Thread.sleep(timeoutDurationMs);
        });

        // When
        long startTime = System.currentTimeMillis();
        startWriteTimeout.countDown();

        guide.switchState(ReadOld_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(timeoutDurationMs));
    }

    @Test
    public void shouldBlockStateSwitchToIfActiveWriterHasDifferentWriteState2() {
        AccessGuide guide = new AccessGuide(ReadNew_WriteBoth);


        long timeoutDurationMs = 300;
        CountDownLatch startWriteTimeout = new CountDownLatch(1);
        writeOnNewThread(guide, writeState -> {

            startWriteTimeout.await();
            Thread.sleep(timeoutDurationMs);
        });

        // When
        long startTime = System.currentTimeMillis();
        startWriteTimeout.countDown();

        guide.switchState(ReadNew_WriteNew);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(timeoutDurationMs));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveReadersHaveTheSameReadState1() {
        AccessGuide guide = new AccessGuide(ReadOld_WriteOld);

        long timeoutDurationMs = 300;
        CountDownLatch startReadTimeout = new CountDownLatch(1);

        readOnNewThread(guide, readState -> {

            startReadTimeout.await();
            Thread.sleep(timeoutDurationMs);
        });

        // When
        long startTime = System.currentTimeMillis();
        startReadTimeout.countDown();

        guide.switchState(ReadOld_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, Matchers.lessThan(20L));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveReaderHaveTheSameReadState2() {
        AccessGuide guide = new AccessGuide(ReadNew_WriteBoth);

        long timeoutDurationMs = 300;
        CountDownLatch startReadTimeout = new CountDownLatch(1);

        readOnNewThread(guide, readState -> {

            startReadTimeout.await();
            Thread.sleep(timeoutDurationMs);
        });

        // When
        long startTime = System.currentTimeMillis();
        startReadTimeout.countDown();

        guide.switchState(ReadNew_WriteNew);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, Matchers.lessThan(20L));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveWritersHaveTheSameWriteState() {
        AccessGuide guide = new AccessGuide(ReadOld_WriteBoth);

        long timeoutDurationMs = 300;
        CountDownLatch startWriteTimeout = new CountDownLatch(1);
        writeOnNewThread(guide, writeState -> {

            startWriteTimeout.await();
            Thread.sleep(timeoutDurationMs);
        });

        // When
        long startTime = System.currentTimeMillis();
        startWriteTimeout.countDown();

        guide.switchState(ReadNew_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, Matchers.lessThan(20L));
    }

    @Test
    public void shouldUseNewReadStatusWhileSwitching() {
        // todo: implement
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitching1() {
        // todo: implement
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitching2() {
        // todo: implement
    }


    private void readOnNewThread(AccessGuide guide, DummyMethod<ReadState> reader) {
        CountDownLatch enteredReadMethod = new CountDownLatch(1);
        executor.submit(() -> guide.read(readState -> {
            enteredReadMethod.countDown();
            try {
                reader.apply(readState);
            } catch (Exception e) {
                // do nothing
            }
            return null;
        }));
        try {
            enteredReadMethod.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeOnNewThread(AccessGuide guide, DummyMethod<WriteState> writer) {
        CountDownLatch enteredWriteMethod = new CountDownLatch(1);
        executor.submit(() -> guide.write(writeState -> {
            enteredWriteMethod.countDown();
            try {
                writer.apply(writeState);
            } catch (Exception e) {
                // do nothing
            }
            return null;
        }));
        try {
            enteredWriteMethod.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private interface DummyMethod<T> {

        void apply(T param) throws Exception;
    }
}
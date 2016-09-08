package zerod.guide;

import mtymes.javafixes.object.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import zerod.ReadState;
import zerod.ReadWriteState;
import zerod.Starter;
import zerod.WriteState;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static mtymes.javafixes.object.Tuple.tuple;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static zerod.ReadState.ReadNew;
import static zerod.ReadWriteState.*;
import static zerod.WriteState.WriteBoth;
import static zerod.WriteState.WriteNew;

public class TransitionalReadWriteGuideConcurrencyTest {

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
    public void shouldBlockStateSwitchIfActiveReadOpHasDifferentReadState() throws Exception {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteBoth);

        long durationInMS = 300;
        Starter readStarter = submitStartableReadOp(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        readStarter.start();

        guide.switchState(ReadNew_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(durationInMS));
    }

    @Test
    public void shouldBlockStateSwitchIfActiveWritOpHasDifferentWriteState1() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 300;
        Starter writeStarter = submitStartableWriteOp(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        writeStarter.start();

        guide.switchState(ReadOld_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(durationInMS));
    }

    @Test
    public void shouldBlockStateSwitchIfActiveWriteOpHasDifferentWriteState2() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadNew_WriteBoth);


        long durationInMS = 300;
        Starter writeStarter = submitStartableWriteOp(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        writeStarter.start();

        guide.switchState(ReadNew_WriteNew);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(durationInMS));
    }

    @Test
    public void shouldBlockStateSwitchIfActiveReadWriteOpHasDifferentState() throws Exception {
        for (Tuple<ReadWriteState, ReadWriteState> fromToState : asList(
                tuple(ReadOld_WriteOld, ReadOld_WriteBoth), tuple(ReadOld_WriteBoth, ReadNew_WriteBoth), tuple(ReadNew_WriteBoth, ReadNew_WriteNew)
        )) {
            TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(fromToState.a);

            long durationInMS = 300;
            Starter readStarter = submitStartableReadWriteOp(guide, durationInMS);

            // When
            long startTime = System.currentTimeMillis();
            readStarter.start();

            guide.switchState(fromToState.b);

            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertThat(duration, greaterThanOrEqualTo(durationInMS));
        }
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveReadOpHaveTheSameReadState1() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 300;
        Starter readStarter = submitStartableReadOp(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        readStarter.start();

        guide.switchState(ReadOld_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveReadOpHaveTheSameReadState2() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadNew_WriteBoth);

        long durationInMS = 300;
        Starter readStarter = submitStartableReadOp(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        readStarter.start();

        guide.switchState(ReadNew_WriteNew);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveWritOpHaveTheSameWriteState() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteBoth);

        long durationInMS = 300;
        Starter writeStarter = submitStartableWriteOp(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        writeStarter.start();

        guide.switchState(ReadNew_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewReadStatusWhileSwitchingAndNotBlock1() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteBoth);

        long durationInMS = 600;
        submitStartableReadOp(guide, durationInMS)
                .start();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteBoth);

        long startTime = System.currentTimeMillis();
        ReadState usedReadStateA = guide.runReadOp(readState -> readState);
        ReadState usedReadStateB = guide.runReadWriteOp((readState, writeState) -> readState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(usedReadStateA, equalTo(ReadNew));
        assertThat(usedReadStateB, equalTo(ReadNew));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitchingAndNotBlock1() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 600;
        submitStartableWriteOp(guide, durationInMS)
                .start();

        // When
        switchStateOnNewThread(guide, ReadOld_WriteBoth);

        AtomicReference<WriteState> usedStateHolderA = new AtomicReference<>();
        long startTime = System.currentTimeMillis();
        guide.runWriteOp(writeState -> usedStateHolderA.set(writeState));
        WriteState usedWriteStateB = guide.runReadWriteOp((readState, writeState) -> writeState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(usedStateHolderA.get(), equalTo(WriteBoth));
        assertThat(usedWriteStateB, equalTo(WriteBoth));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitchingAndNotBlock2() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadNew_WriteBoth);

        long durationInMS = 600;
        submitStartableWriteOp(guide, durationInMS)
                .start();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteNew);

        AtomicReference<WriteState> usedStateHolderA = new AtomicReference<>();
        long startTime = System.currentTimeMillis();
        guide.runWriteOp(writeState -> usedStateHolderA.set(writeState));
        WriteState usedWriteStateB = guide.runReadWriteOp((readState, writeState) -> writeState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(usedStateHolderA.get(), equalTo(WriteNew));
        assertThat(usedWriteStateB, equalTo(WriteNew));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldHaveDifferentCurrentAndTransitionToStateIfSwitchHasNotCompleted() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteBoth);

        // When
        long durationInMS = 600;
        submitStartableReadOp(guide, durationInMS)
                .start();
        switchStateOnNewThread(guide, ReadNew_WriteBoth);

        // Then
        assertThat(guide.getCurrentState(), equalTo(ReadOld_WriteBoth));
        assertThat(guide.getTransitionToState(), equalTo(ReadNew_WriteBoth));
    }

    @Test
    public void shouldNotBeAbleToSwitchToNextStateBeforeThePreviousSwitchHasEnded() {
        TransitionalReadWriteGuide guide = new TransitionalReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 600;
        submitStartableWriteOp(guide, durationInMS)
                .start();
        switchStateOnNewThread(guide, ReadOld_WriteBoth);

        // When

        long startTime = System.currentTimeMillis();
        guide.switchState(ReadNew_WriteBoth);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(400L));
        assertThat(guide.getCurrentState(), equalTo(ReadNew_WriteBoth));
        assertThat(guide.getTransitionToState(), equalTo(ReadNew_WriteBoth));
    }


    private Starter submitStartableReadOp(ReadWriteGuide guide, long readDurationInMS) {
        Starter readStarter = Starter.starter();
        CountDownLatch enteredGuidedMethod = new CountDownLatch(1);
        executor.submit(() -> guide.runReadOp(readState -> {
            enteredGuidedMethod.countDown();
            try {
                readStarter.waitForStartSignal();
                Thread.sleep(readDurationInMS);
            } catch (Exception e) {
                // do nothing
            }
            return null;
        }));
        try {
            enteredGuidedMethod.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return readStarter;
    }

    private Starter submitStartableWriteOp(ReadWriteGuide guide, long writeDurationInMS) {
        Starter writeStarter = Starter.starter();
        CountDownLatch enteredGuidedMethod = new CountDownLatch(1);
        executor.submit(() -> guide.runWriteOp(writeState -> {
            enteredGuidedMethod.countDown();
            try {
                writeStarter.waitForStartSignal();
                Thread.sleep(writeDurationInMS);
            } catch (Exception e) {
                // do nothing
            }
        }));
        try {
            enteredGuidedMethod.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return writeStarter;
    }

    private Starter submitStartableReadWriteOp(ReadWriteGuide guide, long readDurationInMS) {
        Starter readWriteStarter = Starter.starter();
        CountDownLatch enteredGuidedMethod = new CountDownLatch(1);
        executor.submit(() -> guide.runReadWriteOp((readState, writeState) -> {
            enteredGuidedMethod.countDown();
            try {
                readWriteStarter.waitForStartSignal();
                Thread.sleep(readDurationInMS);
            } catch (Exception e) {
                // do nothing
            }
            return null;
        }));
        try {
            enteredGuidedMethod.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return readWriteStarter;
    }

    private void switchStateOnNewThread(TransitionalReadWriteGuide guide, ReadWriteState newState) {
        executor.submit(() -> guide.switchState(newState));
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
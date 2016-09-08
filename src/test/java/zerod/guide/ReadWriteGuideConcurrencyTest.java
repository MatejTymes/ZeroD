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

public class ReadWriteGuideConcurrencyTest {

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadNew_WriteBoth);


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
            ReadWriteGuide guide = new ReadWriteGuide(fromToState.a);

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadNew_WriteBoth);

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

        long durationInMS = 600;
        submitStartableReadOp(guide, durationInMS)
                .start();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteBoth);

        long startTime = System.currentTimeMillis();
        ReadState usedReadStateA = guide.read(readState -> readState);
        ReadState usedReadStateB = guide.readWrite((readState, writeState) -> readState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(usedReadStateA, equalTo(ReadNew));
        assertThat(usedReadStateB, equalTo(ReadNew));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitchingAndNotBlock1() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 600;
        submitStartableWriteOp(guide, durationInMS)
                .start();

        // When
        switchStateOnNewThread(guide, ReadOld_WriteBoth);

        AtomicReference<WriteState> usedStateHolderA = new AtomicReference<>();
        long startTime = System.currentTimeMillis();
        guide.write(writeState -> usedStateHolderA.set(writeState));
        WriteState usedWriteStateB = guide.readWrite((readState, writeState) -> writeState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(usedStateHolderA.get(), equalTo(WriteBoth));
        assertThat(usedWriteStateB, equalTo(WriteBoth));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitchingAndNotBlock2() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadNew_WriteBoth);

        long durationInMS = 600;
        submitStartableWriteOp(guide, durationInMS)
                .start();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteNew);

        AtomicReference<WriteState> usedStateHolderA = new AtomicReference<>();
        long startTime = System.currentTimeMillis();
        guide.write(writeState -> usedStateHolderA.set(writeState));
        WriteState usedWriteStateB = guide.readWrite((readState, writeState) -> writeState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(usedStateHolderA.get(), equalTo(WriteNew));
        assertThat(usedWriteStateB, equalTo(WriteNew));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldHaveDifferentCurrentAndTransitionToStateIfSwitchHasNotCompleted() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

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
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

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
        executor.submit(() -> guide.read(readState -> {
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
        executor.submit(() -> guide.write(writeState -> {
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
        executor.submit(() -> guide.readWrite((readState, writeState) -> {
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

    private void switchStateOnNewThread(ReadWriteGuide guide, ReadWriteState newState) {
        executor.submit(() -> guide.switchState(newState));
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
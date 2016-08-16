package co.uk.zerod;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static co.uk.zerod.ReadState.ReadNew;
import static co.uk.zerod.ReadWriteState.*;
import static co.uk.zerod.WriteState.WriteBoth;
import static co.uk.zerod.WriteState.WriteNew;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

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
    public void shouldBlockStateSwitchToIfActiveReaderHasDifferentReadState() throws Exception {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

        long durationInMS = 300;
        CountDownLatch readCountDown = submitStartableRead(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        readCountDown.countDown();

        guide.switchState(ReadNew_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(durationInMS));
    }

    @Test
    public void shouldBlockStateSwitchToIfActiveWriterHasDifferentWriteState1() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 300;
        CountDownLatch writeCountDown = submitStartableWrite(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        writeCountDown.countDown();

        guide.switchState(ReadOld_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(durationInMS));
    }

    @Test
    public void shouldBlockStateSwitchToIfActiveWriterHasDifferentWriteState2() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadNew_WriteBoth);


        long durationInMS = 300;
        CountDownLatch writeCountDown = submitStartableWrite(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        writeCountDown.countDown();

        guide.switchState(ReadNew_WriteNew);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, greaterThanOrEqualTo(durationInMS));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveReadersHaveTheSameReadState1() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 300;
        CountDownLatch readCountDown = submitStartableRead(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        readCountDown.countDown();

        guide.switchState(ReadOld_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveReaderHaveTheSameReadState2() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadNew_WriteBoth);

        long durationInMS = 300;
        CountDownLatch readCountDown = submitStartableRead(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        readCountDown.countDown();

        guide.switchState(ReadNew_WriteNew);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldNotBlockStateSwitchIfActiveWritersHaveTheSameWriteState() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

        long durationInMS = 300;
        CountDownLatch writeCountDown = submitStartableWrite(guide, durationInMS);

        // When
        long startTime = System.currentTimeMillis();
        writeCountDown.countDown();

        guide.switchState(ReadNew_WriteBoth);

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewReadStatusWhileSwitchingAndNotBlock() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

        long durationInMS = 600;
        submitStartableRead(guide, durationInMS)
                .countDown();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteBoth);

        long startTime = System.currentTimeMillis();
        ReadState newlyUsedReadState = guide.read(readState -> readState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(newlyUsedReadState, equalTo(ReadNew));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitchingAndNotBlock1() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 600;
        submitStartableWrite(guide, durationInMS)
                .countDown();

        // When
        switchStateOnNewThread(guide, ReadOld_WriteBoth);

        long startTime = System.currentTimeMillis();
        WriteState newlyUsedWriteState = guide.write(writeState -> writeState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(newlyUsedWriteState, equalTo(WriteBoth));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitchingAndNotBlock2() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadNew_WriteBoth);

        long durationInMS = 600;
        submitStartableWrite(guide, durationInMS)
                .countDown();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteNew);

        long startTime = System.currentTimeMillis();
        WriteState newlyUsedWriteState = guide.write(writeState -> writeState);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(newlyUsedWriteState, equalTo(WriteNew));
        assertThat(duration, lessThan(20L));
    }

    @Test
    public void shouldHaveDifferentCurrentAndTransitionToStateIfSwitchHasNotCompleted() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteBoth);

        // When
        long durationInMS = 600;
        submitStartableRead(guide, durationInMS)
                .countDown();
        switchStateOnNewThread(guide, ReadNew_WriteBoth);

        // Then
        assertThat(guide.getCurrentState(), equalTo(ReadOld_WriteBoth));
        assertThat(guide.getTransitionToState(), equalTo(ReadNew_WriteBoth));
    }

    @Test
    public void shouldNotBeAbleToSwitchToNextStateBeforeThePreviousSwitchHasEnded() {
        ReadWriteGuide guide = new ReadWriteGuide(ReadOld_WriteOld);

        long durationInMS = 600;
        submitStartableWrite(guide, durationInMS)
                .countDown();
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


    private CountDownLatch submitStartableRead(ReadWriteGuide guide, long readDurationInMS) {
        CountDownLatch readCounDown = new CountDownLatch(1);
        CountDownLatch enteredGuidedMethod = new CountDownLatch(1);
        executor.submit(() -> guide.read(readState -> {
            enteredGuidedMethod.countDown();
            try {
                readCounDown.await();
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

        return readCounDown;
    }

    private CountDownLatch submitStartableWrite(ReadWriteGuide guide, long writeDurationInMS) {
        CountDownLatch writeCounDown = new CountDownLatch(1);
        CountDownLatch enteredGuidedMethod = new CountDownLatch(1);
        executor.submit(() -> guide.write(writeState -> {
            enteredGuidedMethod.countDown();
            try {
                writeCounDown.await();
                Thread.sleep(writeDurationInMS);
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
        return writeCounDown;
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
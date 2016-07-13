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
        AccessGuide guide = new AccessGuide(ReadOld_WriteOld);

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
        AccessGuide guide = new AccessGuide(ReadNew_WriteBoth);


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
        AccessGuide guide = new AccessGuide(ReadOld_WriteOld);

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
        AccessGuide guide = new AccessGuide(ReadNew_WriteBoth);

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
        AccessGuide guide = new AccessGuide(ReadOld_WriteBoth);

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
        AccessGuide guide = new AccessGuide(ReadOld_WriteBoth);

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
        AccessGuide guide = new AccessGuide(ReadOld_WriteOld);

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
        AccessGuide guide = new AccessGuide(ReadNew_WriteBoth);

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
    public void shouldNotBeAbleToSwitchToNextStateBeforeThePreviousSwitchHasEnded() {
        AccessGuide guide = new AccessGuide(ReadOld_WriteOld);

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
        assertThat(guide.getState(), equalTo(ReadNew_WriteBoth));
    }


    private CountDownLatch submitStartableRead(AccessGuide guide, long readDurationInMS) {
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

    private CountDownLatch submitStartableWrite(AccessGuide guide, long writeDurationInMS) {
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

    private void switchStateOnNewThread(AccessGuide guide, ReadWriteState newState) {
        executor.submit(() -> guide.switchState(newState));
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
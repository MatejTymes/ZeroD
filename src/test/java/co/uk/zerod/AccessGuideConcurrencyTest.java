package co.uk.zerod;

import org.hamcrest.Matchers;
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
import static org.hamcrest.Matchers.equalTo;
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
        assertThat(duration, Matchers.lessThan(20L));
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
        assertThat(duration, Matchers.lessThan(20L));
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
        assertThat(duration, Matchers.lessThan(20L));
    }

    @Test
    public void shouldUseNewReadStatusWhileSwitching() {
        AccessGuide guide = new AccessGuide(ReadOld_WriteBoth);

        long durationInMS = 600;
        submitStartableRead(guide, durationInMS)
                .countDown();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteBoth);
        ReadState newlyUsedReadState = guide.read(readState -> readState);

        // Then
        assertThat(newlyUsedReadState, equalTo(ReadNew));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitching1() {
        AccessGuide guide = new AccessGuide(ReadOld_WriteOld);

        long durationInMS = 600;
        submitStartableWrite(guide, durationInMS)
                .countDown();

        // When
        switchStateOnNewThread(guide, ReadOld_WriteBoth);
        WriteState newlyUsedWriteState = guide.write(writeState -> writeState);

        // Then
        assertThat(newlyUsedWriteState, equalTo(WriteBoth));
    }

    @Test
    public void shouldUseNewWriteStatusWhileSwitching2() {
        AccessGuide guide = new AccessGuide(ReadNew_WriteBoth);

        long durationInMS = 600;
        submitStartableWrite(guide, durationInMS)
                .countDown();

        // When
        switchStateOnNewThread(guide, ReadNew_WriteNew);
        WriteState newlyUsedWriteState = guide.write(writeState -> writeState);

        // Then
        assertThat(newlyUsedWriteState, equalTo(WriteNew));
    }

    @Test
    public void shouldNotBeAbleToSwitchToNextStateBeforeThePreviousSwitchHasEnded() {
        // todo: this is hard to test - but the switchState should be synchronized
    }


    private CountDownLatch submitStartableRead(AccessGuide guide, long readDurationInMS) {
        CountDownLatch readCoundown = new CountDownLatch(1);
        CountDownLatch enteredGuidedMethod = new CountDownLatch(1);
        executor.submit(() -> guide.read(readState -> {
            enteredGuidedMethod.countDown();
            try {
                readCoundown.await();
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

        return readCoundown;
    }

    private CountDownLatch submitStartableWrite(AccessGuide guide, long writeDurationInMS) {
        CountDownLatch writeCoundown = new CountDownLatch(1);
        CountDownLatch enteredGuidedMethod = new CountDownLatch(1);
        executor.submit(() -> guide.write(writeState -> {
            enteredGuidedMethod.countDown();
            try {
                writeCoundown.await();
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
        return writeCoundown;
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
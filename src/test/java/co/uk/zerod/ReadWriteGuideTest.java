package co.uk.zerod;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static co.uk.zerod.ReadState.ReadNew;
import static co.uk.zerod.ReadState.ReadOld;
import static co.uk.zerod.ReadWriteState.*;
import static co.uk.zerod.WriteState.WriteBoth;
import static co.uk.zerod.WriteState.WriteNew;
import static co.uk.zerod.test.Random.randomReadWriteState;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ReadWriteGuideTest {

    @Test
    public void shouldProvideProperReadAndWriteState() {
        for (ReadWriteState readWriteState : ReadWriteState.values()) {

            ReadWriteGuide readWriteGuide = new ReadWriteGuide(readWriteState);

            // When
            ReadState usedReadState = actualReadState(readWriteGuide);
            WriteState usedWriteState = actualWriteState(readWriteGuide);

            // Then
            assertThat(readWriteGuide.getCurrentState(), equalTo(readWriteState));
            assertThat(readWriteGuide.getTransitionToState(), equalTo(readWriteState));
            assertThat(usedReadState, equalTo(readWriteState.readState));
            assertThat(usedWriteState, equalTo(readWriteState.writeState));
        }
    }

    @Test
    public void shouldReturnValueReadValue() {
        ReadWriteGuide readWriteGuide = new ReadWriteGuide(randomReadWriteState());
        UUID expectedResponse = randomUUID();

        // When
        UUID readResponse = readWriteGuide.read(readState -> expectedResponse);

        // Then
        assertThat(readResponse, equalTo(expectedResponse));
    }

    @Test
    public void shouldProvideProperWriteState() {
        ReadWriteGuide readWriteGuide = new ReadWriteGuide(randomReadWriteState());
        UUID expectedResponse = randomUUID();

        // When
        UUID writeResponse = readWriteGuide.write(writeState -> expectedResponse);

        // Then
        assertThat(writeResponse, equalTo(expectedResponse));
    }

    @Test
    public void shouldBeAbleToSwitchToNextState() {
        ReadWriteGuide readWriteGuide = new ReadWriteGuide(ReadOld_WriteOld);

        readWriteGuide.switchState(ReadOld_WriteBoth);
        assertThat(readWriteGuide.getCurrentState(), equalTo(ReadOld_WriteBoth));
        assertThat(readWriteGuide.getTransitionToState(), equalTo(ReadOld_WriteBoth));
        assertThat(actualReadState(readWriteGuide), equalTo(ReadOld));
        assertThat(actualWriteState(readWriteGuide), equalTo(WriteBoth));

        readWriteGuide.switchState(ReadNew_WriteBoth);
        assertThat(readWriteGuide.getCurrentState(), equalTo(ReadNew_WriteBoth));
        assertThat(readWriteGuide.getTransitionToState(), equalTo(ReadNew_WriteBoth));
        assertThat(actualReadState(readWriteGuide), equalTo(ReadNew));
        assertThat(actualWriteState(readWriteGuide), equalTo(WriteBoth));

        readWriteGuide.switchState(ReadNew_WriteNew);
        assertThat(readWriteGuide.getCurrentState(), equalTo(ReadNew_WriteNew));
        assertThat(readWriteGuide.getTransitionToState(), equalTo(ReadNew_WriteNew));
        assertThat(actualReadState(readWriteGuide), equalTo(ReadNew));
        assertThat(actualWriteState(readWriteGuide), equalTo(WriteNew));
    }

    @Test
    public void shouldBeAbleToSwitchToTheSameState() {
        for (ReadWriteState readWriteState : ReadWriteState.values()) {

            ReadWriteGuide readWriteGuide = new ReadWriteGuide(readWriteState);

            // When
            readWriteGuide.switchState(readWriteState);

            // Then
            assertThat(readWriteGuide.getCurrentState(), equalTo(readWriteState));
            assertThat(readWriteGuide.getTransitionToState(), equalTo(readWriteState));
        }
    }

    @Test
    public void shouldFailOnInvalidStateTransition() {
        ReadWriteGuide readWriteGuide;

        readWriteGuide = new ReadWriteGuide(ReadOld_WriteOld);
        for (ReadWriteState invalidTransitionalStates : asList(ReadNew_WriteBoth, ReadNew_WriteNew)) {
            try {
                readWriteGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }

        readWriteGuide = new ReadWriteGuide(ReadOld_WriteBoth);
        for (ReadWriteState invalidTransitionalStates : asList(ReadOld_WriteOld, ReadNew_WriteNew)) {
            try {
                readWriteGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }

        readWriteGuide = new ReadWriteGuide(ReadNew_WriteNew);
        for (ReadWriteState invalidTransitionalStates : asList(ReadOld_WriteOld, ReadOld_WriteBoth)) {
            try {
                readWriteGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }
    }


    private ReadState actualReadState(ReadWriteGuide readWriteGuide) {
        AtomicReference<ReadState> readStateToUse = new AtomicReference<>(null);
        readWriteGuide.read(readState -> {
            readStateToUse.set(readState);
            return null;
        });
        return readStateToUse.get();
    }


    private WriteState actualWriteState(ReadWriteGuide readWriteGuide) {
        AtomicReference<WriteState> writeStateToUse = new AtomicReference<>(null);
        readWriteGuide.write(writeState -> {
            writeStateToUse.set(writeState);
            return null;
        });
        return writeStateToUse.get();
    }
}
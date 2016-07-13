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

public class AccessGuideTest {

    @Test
    public void shouldProvideProperReadAndWriteState() {
        for (ReadWriteState readWriteState : ReadWriteState.values()) {

            AccessGuide accessGuide = new AccessGuide(readWriteState);

            // When
            ReadState usedReadState = actualReadState(accessGuide);
            WriteState usedWriteState = actualWriteState(accessGuide);

            // Then
            assertThat(accessGuide.getState(), equalTo(readWriteState));
            assertThat(usedReadState, equalTo(readWriteState.readState));
            assertThat(usedWriteState, equalTo(readWriteState.writeState));
        }
    }


    @Test
    public void shouldReturnValueReadValue() {
        AccessGuide accessGuide = new AccessGuide(randomReadWriteState());
        UUID expectedResponse = randomUUID();

        // When
        UUID readResponse = accessGuide.read(readState -> expectedResponse);

        // Then
        assertThat(readResponse, equalTo(expectedResponse));
    }


    @Test
    public void shouldProvideProperWriteState() {
        AccessGuide accessGuide = new AccessGuide(randomReadWriteState());
        UUID expectedResponse = randomUUID();

        // When
        UUID writeResponse = accessGuide.write(writeState -> expectedResponse);

        // Then
        assertThat(writeResponse, equalTo(expectedResponse));
    }


    @Test
    public void shouldBeAbleToSwitchToNextState() {
        AccessGuide accessGuide = new AccessGuide(ReadOld_WriteOld);

        accessGuide.switchState(ReadOld_WriteBoth);
        assertThat(accessGuide.getState(), equalTo(ReadOld_WriteBoth));
        assertThat(actualReadState(accessGuide), equalTo(ReadOld));
        assertThat(actualWriteState(accessGuide), equalTo(WriteBoth));

        accessGuide.switchState(ReadNew_WriteBoth);
        assertThat(accessGuide.getState(), equalTo(ReadNew_WriteBoth));
        assertThat(actualReadState(accessGuide), equalTo(ReadNew));
        assertThat(actualWriteState(accessGuide), equalTo(WriteBoth));

        accessGuide.switchState(ReadNew_WriteNew);
        assertThat(accessGuide.getState(), equalTo(ReadNew_WriteNew));
        assertThat(actualReadState(accessGuide), equalTo(ReadNew));
        assertThat(actualWriteState(accessGuide), equalTo(WriteNew));
    }

    @Test
    public void shouldBeAbleToSwitchToTheSameState() {
        for (ReadWriteState readWriteState : ReadWriteState.values()) {

            AccessGuide accessGuide = new AccessGuide(readWriteState);

            // When
            accessGuide.switchState(readWriteState);

            // Then
            assertThat(accessGuide.getState(), equalTo(readWriteState));
        }
    }


    @Test
    public void shouldFailOnInvalidStateTransition() {
        AccessGuide accessGuide;

        accessGuide = new AccessGuide(ReadOld_WriteOld);
        for (ReadWriteState invalidTransitionalStates : asList(ReadNew_WriteBoth, ReadNew_WriteNew)) {
            try {
                accessGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }

        accessGuide = new AccessGuide(ReadOld_WriteBoth);
        for (ReadWriteState invalidTransitionalStates : asList(ReadOld_WriteOld, ReadNew_WriteNew)) {
            try {
                accessGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }

        accessGuide = new AccessGuide(ReadNew_WriteNew);
        for (ReadWriteState invalidTransitionalStates : asList(ReadOld_WriteOld, ReadOld_WriteBoth)) {
            try {
                accessGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }
    }


    private ReadState actualReadState(AccessGuide accessGuide) {
        AtomicReference<ReadState> readStateToUse = new AtomicReference<>(null);
        accessGuide.read(readState -> {
            readStateToUse.set(readState);
            return null;
        });
        return readStateToUse.get();
    }


    private WriteState actualWriteState(AccessGuide accessGuide) {
        AtomicReference<WriteState> writeStateToUse = new AtomicReference<>(null);
        accessGuide.write(writeState -> {
            writeStateToUse.set(writeState);
            return null;
        });
        return writeStateToUse.get();
    }
}
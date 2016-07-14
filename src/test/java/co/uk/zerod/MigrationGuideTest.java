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

public class MigrationGuideTest {

    @Test
    public void shouldProvideProperReadAndWriteState() {
        for (ReadWriteState readWriteState : ReadWriteState.values()) {

            MigrationGuide migrationGuide = new MigrationGuide(readWriteState);

            // When
            ReadState usedReadState = actualReadState(migrationGuide);
            WriteState usedWriteState = actualWriteState(migrationGuide);

            // Then
            assertThat(migrationGuide.getCurrentState(), equalTo(readWriteState));
            assertThat(migrationGuide.getTransitionToState(), equalTo(readWriteState));
            assertThat(usedReadState, equalTo(readWriteState.readState));
            assertThat(usedWriteState, equalTo(readWriteState.writeState));
        }
    }

    @Test
    public void shouldReturnValueReadValue() {
        MigrationGuide migrationGuide = new MigrationGuide(randomReadWriteState());
        UUID expectedResponse = randomUUID();

        // When
        UUID readResponse = migrationGuide.read(readState -> expectedResponse);

        // Then
        assertThat(readResponse, equalTo(expectedResponse));
    }

    @Test
    public void shouldProvideProperWriteState() {
        MigrationGuide migrationGuide = new MigrationGuide(randomReadWriteState());
        UUID expectedResponse = randomUUID();

        // When
        UUID writeResponse = migrationGuide.write(writeState -> expectedResponse);

        // Then
        assertThat(writeResponse, equalTo(expectedResponse));
    }

    @Test
    public void shouldBeAbleToSwitchToNextState() {
        MigrationGuide migrationGuide = new MigrationGuide(ReadOld_WriteOld);

        migrationGuide.switchState(ReadOld_WriteBoth);
        assertThat(migrationGuide.getCurrentState(), equalTo(ReadOld_WriteBoth));
        assertThat(migrationGuide.getTransitionToState(), equalTo(ReadOld_WriteBoth));
        assertThat(actualReadState(migrationGuide), equalTo(ReadOld));
        assertThat(actualWriteState(migrationGuide), equalTo(WriteBoth));

        migrationGuide.switchState(ReadNew_WriteBoth);
        assertThat(migrationGuide.getCurrentState(), equalTo(ReadNew_WriteBoth));
        assertThat(migrationGuide.getTransitionToState(), equalTo(ReadNew_WriteBoth));
        assertThat(actualReadState(migrationGuide), equalTo(ReadNew));
        assertThat(actualWriteState(migrationGuide), equalTo(WriteBoth));

        migrationGuide.switchState(ReadNew_WriteNew);
        assertThat(migrationGuide.getCurrentState(), equalTo(ReadNew_WriteNew));
        assertThat(migrationGuide.getTransitionToState(), equalTo(ReadNew_WriteNew));
        assertThat(actualReadState(migrationGuide), equalTo(ReadNew));
        assertThat(actualWriteState(migrationGuide), equalTo(WriteNew));
    }

    @Test
    public void shouldBeAbleToSwitchToTheSameState() {
        for (ReadWriteState readWriteState : ReadWriteState.values()) {

            MigrationGuide migrationGuide = new MigrationGuide(readWriteState);

            // When
            migrationGuide.switchState(readWriteState);

            // Then
            assertThat(migrationGuide.getCurrentState(), equalTo(readWriteState));
            assertThat(migrationGuide.getTransitionToState(), equalTo(readWriteState));
        }
    }

    @Test
    public void shouldFailOnInvalidStateTransition() {
        MigrationGuide migrationGuide;

        migrationGuide = new MigrationGuide(ReadOld_WriteOld);
        for (ReadWriteState invalidTransitionalStates : asList(ReadNew_WriteBoth, ReadNew_WriteNew)) {
            try {
                migrationGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }

        migrationGuide = new MigrationGuide(ReadOld_WriteBoth);
        for (ReadWriteState invalidTransitionalStates : asList(ReadOld_WriteOld, ReadNew_WriteNew)) {
            try {
                migrationGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }

        migrationGuide = new MigrationGuide(ReadNew_WriteNew);
        for (ReadWriteState invalidTransitionalStates : asList(ReadOld_WriteOld, ReadOld_WriteBoth)) {
            try {
                migrationGuide.switchState(invalidTransitionalStates);
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // do nothing
            }
        }
    }


    private ReadState actualReadState(MigrationGuide migrationGuide) {
        AtomicReference<ReadState> readStateToUse = new AtomicReference<>(null);
        migrationGuide.read(readState -> {
            readStateToUse.set(readState);
            return null;
        });
        return readStateToUse.get();
    }


    private WriteState actualWriteState(MigrationGuide migrationGuide) {
        AtomicReference<WriteState> writeStateToUse = new AtomicReference<>(null);
        migrationGuide.write(writeState -> {
            writeStateToUse.set(writeState);
            return null;
        });
        return writeStateToUse.get();
    }
}
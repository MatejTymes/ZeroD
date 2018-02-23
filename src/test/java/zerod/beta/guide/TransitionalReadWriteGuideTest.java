package zerod.beta.guide;

import javafixes.object.Tuple;
import org.junit.After;
import org.junit.Test;
import zerod.state.StateTransitioner;
import zerod.state.domain.ReadState;
import zerod.state.domain.ReadWriteState;
import zerod.state.domain.WriteState;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.UUID.randomUUID;
import static javafixes.object.Tuple.tuple;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static zerod.test.Condition.otherThan;
import static zerod.test.Random.randomReadWriteState;

public class TransitionalReadWriteGuideTest {

    private StateTransitioner stateTransitioner = mock(StateTransitioner.class);

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(stateTransitioner);
    }

    @Test
    public void shouldProvideProperReadAndWriteState() {
        for (ReadWriteState readWriteState : ReadWriteState.values()) {

            TransitionalReadWriteGuide readWriteGuide = new TransitionalReadWriteGuide(stateTransitioner, readWriteState);

            // When
            ReadState usedReadState = actualReadState(readWriteGuide);
            WriteState usedWriteState = actualWriteState(readWriteGuide);
            Tuple<ReadState, WriteState> usedReadWriteState = actualReadWriteState(readWriteGuide);

            // Then
            assertThat(readWriteGuide.getCurrentState(), equalTo(readWriteState));
            assertThat(readWriteGuide.getTransitionToState(), equalTo(readWriteState));
            assertThat(usedReadState, equalTo(readWriteState.readState));
            assertThat(usedWriteState, equalTo(readWriteState.writeState));
            assertThat(usedReadWriteState, equalTo(tuple(readWriteState.readState, readWriteState.writeState)));
        }
    }

    @Test
    public void shouldReturnProperValueOnReadOperation() {
        ReadWriteState expectedState = randomReadWriteState();
        ReadWriteGuide readWriteGuide = new TransitionalReadWriteGuide(stateTransitioner, expectedState);
        UUID expectedResponse = randomUUID();

        // When
        UUID actualResponse = readWriteGuide.runReadOp(readState -> expectedResponse);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }

    @Test
    public void shouldUseProperValueOnWriteOperation() {
        ReadWriteState expectedState = randomReadWriteState();
        ReadWriteGuide readWriteGuide = new TransitionalReadWriteGuide(stateTransitioner, expectedState);

        AtomicReference<WriteState> usedWriteState = new AtomicReference<>();

        // When
        readWriteGuide.runWriteOp(writeState -> usedWriteState.set(writeState));

        // Then
        assertThat(usedWriteState.get(), equalTo(expectedState.writeState));
    }

    @Test
    public void shouldReturnProperValueOnReadWriteOperation() {
        ReadWriteState expectedState = randomReadWriteState();
        ReadWriteGuide readWriteGuide = new TransitionalReadWriteGuide(stateTransitioner, expectedState);
        UUID expectedResponse = randomUUID();

        // When
        UUID actualResponse = readWriteGuide.runReadWriteOp((readState, writeState) -> expectedResponse);

        // Then
        assertThat(actualResponse, equalTo(expectedResponse));
    }

    @Test
    public void shouldBeAbleToSwitchToNextValidState() {
        ReadWriteState oldState = randomReadWriteState();
        ReadWriteState newState = randomReadWriteState(otherThan(oldState));
        TransitionalReadWriteGuide readWriteGuide = new TransitionalReadWriteGuide(stateTransitioner, oldState);

        when(stateTransitioner.canTransitionFromTo(oldState, newState)).thenReturn(true);

        // When
        readWriteGuide.switchState(newState);

        // Then
        verify(stateTransitioner).canTransitionFromTo(oldState, newState);
        assertThat(readWriteGuide.getCurrentState(), equalTo(newState));
        assertThat(readWriteGuide.getTransitionToState(), equalTo(newState));
        assertThat(actualReadState(readWriteGuide), equalTo(newState.readState));
        assertThat(actualWriteState(readWriteGuide), equalTo(newState.writeState));
        assertThat(actualReadWriteState(readWriteGuide), equalTo(tuple(newState.readState, newState.writeState)));
    }

    @Test
    public void shouldFailOnInvalidStateTransition() {
        ReadWriteState oldState = randomReadWriteState();
        ReadWriteState invalidNewState = randomReadWriteState(otherThan(oldState));
        TransitionalReadWriteGuide readWriteGuide = new TransitionalReadWriteGuide(stateTransitioner, oldState);

        when(stateTransitioner.canTransitionFromTo(oldState, invalidNewState)).thenReturn(false);

        try {
            // When
            readWriteGuide.switchState(invalidNewState);

            // Then
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {

            verify(stateTransitioner).canTransitionFromTo(oldState, invalidNewState);
            assertThat(readWriteGuide.getCurrentState(), equalTo(oldState));
            assertThat(readWriteGuide.getTransitionToState(), equalTo(oldState));
            assertThat(actualReadState(readWriteGuide), equalTo(oldState.readState));
            assertThat(actualWriteState(readWriteGuide), equalTo(oldState.writeState));
            assertThat(actualReadWriteState(readWriteGuide), equalTo(tuple(oldState.readState, oldState.writeState)));
        }
    }


    private ReadState actualReadState(ReadWriteGuide readWriteGuide) {
        AtomicReference<ReadState> readStateToUse = new AtomicReference<>();
        readWriteGuide.runReadOp(readState -> {
            readStateToUse.set(readState);
            return null;
        });
        return readStateToUse.get();
    }

    private WriteState actualWriteState(ReadWriteGuide readWriteGuide) {
        AtomicReference<WriteState> writeStateToUse = new AtomicReference<>();
        readWriteGuide.runWriteOp(writeState -> {
            writeStateToUse.set(writeState);
        });
        return writeStateToUse.get();
    }

    private Tuple<ReadState, WriteState> actualReadWriteState(ReadWriteGuide readWriteGuide) {
        AtomicReference<Tuple<ReadState, WriteState>> readStateToUse = new AtomicReference<>();
        readWriteGuide.runReadWriteOp((readState, writeState) -> {
            readStateToUse.set(tuple(readState, writeState));
            return null;
        });
        return readStateToUse.get();
    }
}
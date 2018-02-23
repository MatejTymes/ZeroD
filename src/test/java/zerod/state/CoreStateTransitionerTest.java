package zerod.state;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static zerod.state.domain.ReadWriteState.*;

public class CoreStateTransitionerTest {

    private StateTransitioner transitioner = new CoreStateTransitioner();

    @Test
    public void shouldAllowTransitionToNextState() {
        assertThat(transitioner.canTransitionFromTo(ReadOld_WriteOld, ReadOld_WriteBoth), is(true));
        assertThat(transitioner.canTransitionFromTo(ReadOld_WriteBoth, ReadNew_WriteBoth), is(true));
        assertThat(transitioner.canTransitionFromTo(ReadNew_WriteBoth, ReadNew_WriteNew), is(true));
    }

    @Test
    public void shouldAllowTransitionToPreviousState() {
        assertThat(transitioner.canTransitionFromTo(ReadNew_WriteNew, ReadNew_WriteBoth), is(true));
        assertThat(transitioner.canTransitionFromTo(ReadNew_WriteBoth, ReadOld_WriteBoth), is(true));
        assertThat(transitioner.canTransitionFromTo(ReadOld_WriteBoth, ReadOld_WriteOld), is(true));
    }

    @Test
    public void shouldNotAllowTransitionToOtherThanNextOrPreviousState() {
        assertThat(transitioner.canTransitionFromTo(ReadOld_WriteOld, ReadNew_WriteBoth), is(false));
        assertThat(transitioner.canTransitionFromTo(ReadOld_WriteOld, ReadNew_WriteNew), is(false));

        assertThat(transitioner.canTransitionFromTo(ReadOld_WriteBoth, ReadNew_WriteNew), is(false));

        assertThat(transitioner.canTransitionFromTo(ReadNew_WriteBoth, ReadOld_WriteOld), is(false));

        assertThat(transitioner.canTransitionFromTo(ReadNew_WriteNew, ReadOld_WriteBoth), is(false));
        assertThat(transitioner.canTransitionFromTo(ReadNew_WriteNew, ReadOld_WriteOld), is(false));
    }
}
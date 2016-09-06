package zerod.domain;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static zerod.test.Random.randomInt;

public class HealthTest {

    @Test
    public void shouldNotCreateHealthWithNegativeValue() {
        byte negativeByte = (byte) randomInt(-128, -1);

        try {
            // When
            Health.health(negativeByte);

            // Then
            fail("expected IllegalArgumentException as can't create Health using negative value");
        } catch (IllegalArgumentException expected) {
            // this is expected
        }
    }

    @Test
    public void shouldBeConsideredDeadOnHealthWithZeroValue() {
        byte zeroByte = 0;

        Health health = Health.health(zeroByte);

        assertThat(health.isAlive(), is(false));
    }

    @Test
    public void shouldBeConsideredAliveOnHealthWithPositiveValue() {
        byte positiveByte = (byte) randomInt(1, 127);

        Health health = Health.health(positiveByte);

        assertThat(health.isAlive(), is(true));
    }
}
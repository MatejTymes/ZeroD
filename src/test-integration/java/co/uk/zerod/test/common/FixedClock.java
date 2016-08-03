package co.uk.zerod.test.common;

import co.uk.zerod.common.Clock;

import java.time.Duration;
import java.time.ZonedDateTime;

public class FixedClock extends Clock {

    private ZonedDateTime now = ZonedDateTime.now(UTC_ZONE);

    @Override
    public ZonedDateTime now() {
        return now;
    }

    public ZonedDateTime setNow(ZonedDateTime newNow) {
        this.now = newNow;
        return newNow;
    }

    public ZonedDateTime increaseBy(Duration duration) {
        return setNow(now.plus(duration));
    }

    public ZonedDateTime increaseBySeconds(int seconds) {
        return increaseBy(Duration.ofSeconds(seconds));
    }
}

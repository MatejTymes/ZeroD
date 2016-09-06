package zerod.common;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Clock {

    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    public ZonedDateTime now() {
        return ZonedDateTime.now(UTC_ZONE);
    }
}

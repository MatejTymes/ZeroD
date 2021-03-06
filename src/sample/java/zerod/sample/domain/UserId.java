package zerod.sample.domain;

import javafixes.object.Microtype;

import java.util.UUID;

public class UserId extends Microtype<UUID> {

    public UserId(UUID value) {
        super(value);
    }

    public static UserId userId(UUID value) {
        return (value == null) ? null : new UserId(value);
    }
}

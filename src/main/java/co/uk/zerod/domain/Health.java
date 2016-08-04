package co.uk.zerod.domain;

import external.mtymes.javafixes.object.Microtype;

public class Health extends Microtype<Byte> {

    private Health(Byte value) {
        super(value);
        if (value < 0) {
            throw new IllegalArgumentException("Health can't be lower than 0");
        }
    }

    public static Health health(byte value) {
        return new Health(value);
    }

    public boolean isAlive() {
        return value() > 0;
    }
}

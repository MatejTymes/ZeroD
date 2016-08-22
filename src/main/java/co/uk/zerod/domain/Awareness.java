package co.uk.zerod.domain;

public enum Awareness {

    Aware(true),
    NotAware(false);

    public final boolean isAware;

    Awareness(boolean isAware) {
        this.isAware = isAware;
    }
}

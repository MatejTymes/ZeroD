package co.uk.zerod.domain;

import external.mtymes.javafixes.object.Microtype;

public class NodeName extends Microtype<String> {

    private NodeName(String value) {
        super(value);
    }

    public static NodeName nodeName(String value) {
        return new NodeName(value);
    }

}

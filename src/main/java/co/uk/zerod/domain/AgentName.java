package co.uk.zerod.domain;

import external.mtymes.javafixes.object.Microtype;

public class AgentName extends Microtype<String> {

    private AgentName(String value) {
        super(value);
    }

    public static AgentName agentName(String value) {
        return new AgentName(value);
    }
}

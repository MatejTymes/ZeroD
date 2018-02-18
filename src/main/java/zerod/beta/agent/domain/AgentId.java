package zerod.beta.agent.domain;

import javafixes.object.Microtype;

public class AgentId extends Microtype<String> {

    private AgentId(String value) {
        super(value);
    }

    public static AgentId agentId(String value) {
        return new AgentId(value);
    }
}

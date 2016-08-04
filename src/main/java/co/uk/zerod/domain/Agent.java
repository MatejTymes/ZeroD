package co.uk.zerod.domain;

import external.mtymes.javafixes.object.DataObject;

import java.time.ZonedDateTime;

public class Agent extends DataObject {

    public final AgentName name;
    public final Health health;
    public final ZonedDateTime lastUpdatedAt;

    public Agent(AgentName name, Health health, ZonedDateTime lastUpdatedAt) {
        this.name = name;
        this.health = health;
        this.lastUpdatedAt = lastUpdatedAt;
    }
}

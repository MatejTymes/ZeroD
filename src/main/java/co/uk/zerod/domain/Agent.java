package co.uk.zerod.domain;

import mtymes.javafixes.object.DataObject;

import java.time.ZonedDateTime;

public class Agent extends DataObject {

    public final AgentId id;
    public final Health health;
    public final ZonedDateTime lastUpdatedAt;

    public Agent(AgentId id, Health health, ZonedDateTime lastUpdatedAt) {
        this.id = id;
        this.health = health;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public AgentId id() {
        return id;
    }
}

package co.uk.zerod.dao;

import co.uk.zerod.domain.Agent;
import co.uk.zerod.domain.AgentName;
import co.uk.zerod.domain.Health;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

public interface AgentDao {

    // todo: made concurrent safe - add concurrency test
    void registerAgentHealth(AgentName agentName, Health health);

    // todo: made concurrent safe - add concurrency test
    boolean updateAgentsHealth(AgentName agentName, Health from, Health to, ZonedDateTime ifNotUpdatedSince);

    Optional<Agent> findAgent(AgentName agentName);

    Set<Agent> findStaleAgents(ZonedDateTime notUpdatedSince);

    Set<Agent> findLiveAgents();

    Set<Agent> findDeadAgents();
}

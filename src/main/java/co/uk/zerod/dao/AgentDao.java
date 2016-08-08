package co.uk.zerod.dao;

import co.uk.zerod.domain.Agent;
import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Health;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

public interface AgentDao {

    // todo: made concurrent safe - add concurrency test
    void registerAgentHealth(AgentId agentId, Health health);

    // todo: made concurrent safe - add concurrency test
    boolean updateAgentsHealth(AgentId agentId, Health from, Health to, ZonedDateTime ifNotUpdatedSince);

    Optional<Agent> findAgent(AgentId agentId);

    // todo: replace with findLiveStaleAgents
    Set<Agent> findStaleAgents(ZonedDateTime notUpdatedSince);

    Set<Agent> findLiveAgents();

    Set<Agent> findDeadAgents();
}

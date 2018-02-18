package zerod.beta.agent.dao;

import zerod.beta.agent.domain.Agent;
import zerod.beta.agent.domain.AgentId;
import zerod.beta.agent.domain.Health;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

public interface AgentDao {

    void registerAgentHealth(AgentId agentId, Health health);

    boolean updateAgentsHealth(AgentId agentId, Health from, Health to, ZonedDateTime ifNotUpdatedSince);

    Optional<Agent> findAgent(AgentId agentId);

    Set<Agent> findStaleLiveAgents(ZonedDateTime notUpdatedSince);

    Set<Agent> findLiveAgents();

    Set<Agent> findDeadAgents();
}

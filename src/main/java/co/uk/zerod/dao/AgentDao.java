package co.uk.zerod.dao;

import co.uk.zerod.domain.AgentName;

import java.time.ZonedDateTime;
import java.util.Set;

public interface AgentDao {

    // todo: add concurrency test
    void registerHeartBeatFor(AgentName agentName);

    Set<AgentName> findAgentsAliveSince(ZonedDateTime activeSince);
}

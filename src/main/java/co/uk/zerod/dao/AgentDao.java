package co.uk.zerod.dao;

import co.uk.zerod.domain.AgentName;

import java.time.ZonedDateTime;
import java.util.Set;

// todo: move to health based system
// todo: rename to AgentHealthDao
public interface AgentDao {

    // todo: made concurrent safe - add concurrency test
    void registerHeartBeatFor(AgentName agentName);

    Set<AgentName> findAgentsAliveSince(ZonedDateTime activeSince);
}

package zerod.dao;

import zerod.domain.AgentId;
import zerod.domain.Awareness;
import zerod.domain.MigrationId;

import java.util.Map;
import java.util.Optional;

public interface AwarenessDao {

    void registerAwareness(MigrationId migrationId, AgentId agentId, Awareness awareness);

    Map<AgentId, Optional<Awareness>> findLiveAgentsAwareness(MigrationId migrationId);
}

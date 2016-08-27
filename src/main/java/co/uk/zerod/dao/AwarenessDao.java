package co.uk.zerod.dao;

import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Awareness;
import co.uk.zerod.domain.MigrationId;

import java.util.Map;
import java.util.Optional;

public interface AwarenessDao {

    void registerAwareness(MigrationId migrationId, AgentId agentId, Awareness awareness);

    Map<AgentId, Optional<Awareness>> findLiveAgentsAwareness(MigrationId migrationId);
}

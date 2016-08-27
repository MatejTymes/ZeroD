package co.uk.zerod.dao;

import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Awareness;
import co.uk.zerod.domain.MigrationId;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static co.uk.zerod.domain.Health.noHealth;
import static co.uk.zerod.test.Condition.otherThan;
import static co.uk.zerod.test.Random.*;
import static co.uk.zerod.test.common.MapFiller.fill;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public abstract class AwarenessDaoTestBase {

    private AgentDao agentDao = getAgentDao();
    private AwarenessDao dao = getDao();

    @Test
    public void shouldFindNoAwarenessInEmptyDb() {
        assertThat(dao.findLiveAgentsAwareness(randomMigrationId()).keySet(), is(empty()));
    }

    @Test
    public void shouldFindAwarenessOfLiveAgents() {
        MigrationId migrationId = randomMigrationId();

        AgentId agentId1 = randomAgentId();
        AgentId agentId2 = randomAgentId(otherThan(agentId1));
        AgentId agentId3 = randomAgentId(otherThan(agentId1));

        agentDao.registerAgentHealth(agentId1, randomLiveHealth());
        agentDao.registerAgentHealth(agentId2, randomLiveHealth());
        agentDao.registerAgentHealth(agentId3, randomLiveHealth());

        Awareness awareness1 = randomAwareness();
        Awareness awareness3 = randomAwareness(otherThan(awareness1));
        dao.registerAwareness(migrationId, agentId1, awareness1);
        dao.registerAwareness(migrationId, agentId3, awareness3);

        // When
        Map<AgentId, Optional<Awareness>> awareness = dao.findLiveAgentsAwareness(migrationId);

        // Then
        assertThat(awareness, equalTo(fill(newHashMap())
                .put(agentId1, Optional.of(awareness1))
                .put(agentId2, Optional.empty())
                .put(agentId3, Optional.of(awareness3))
                .map));
    }

    @Test
    public void shouldNotFindAwarenessOfDeadAgents() {
        MigrationId migrationId = randomMigrationId();

        AgentId agentId1 = randomAgentId();
        AgentId agentId2 = randomAgentId(otherThan(agentId1));
        AgentId agentId3 = randomAgentId(otherThan(agentId1));

        agentDao.registerAgentHealth(agentId1, noHealth());
        agentDao.registerAgentHealth(agentId2, noHealth());
        agentDao.registerAgentHealth(agentId3, noHealth());

        Awareness awareness1 = randomAwareness();
        Awareness awareness3 = randomAwareness(otherThan(awareness1));
        dao.registerAwareness(migrationId, agentId1, awareness1);
        dao.registerAwareness(migrationId, agentId3, awareness3);

        // When
        Map<AgentId, Optional<Awareness>> awareness = dao.findLiveAgentsAwareness(migrationId);

        // Then
        assertThat(awareness, equalTo(emptyMap()));
    }

    @Test
    public void shouldNotFindAwarenessOfDifferentMigration() {
        MigrationId migrationId = randomMigrationId();
        MigrationId otherMigrationId = randomMigrationId(otherThan(migrationId));

        AgentId agentId = randomAgentId();

        agentDao.registerAgentHealth(agentId, randomLiveHealth());

        dao.registerAwareness(otherMigrationId, agentId, randomAwareness());

        // When
        Map<AgentId, Optional<Awareness>> awareness = dao.findLiveAgentsAwareness(migrationId);

        // Then
        assertThat(awareness, equalTo(fill(newHashMap())
                .put(agentId, Optional.empty())
                .map));
    }


    protected abstract AgentDao getAgentDao();

    protected abstract AwarenessDao getDao();
}

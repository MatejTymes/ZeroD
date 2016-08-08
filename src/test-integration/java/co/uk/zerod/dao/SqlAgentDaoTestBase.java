package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.domain.Agent;
import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Health;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static co.uk.zerod.domain.AgentId.agentId;
import static co.uk.zerod.domain.Health.noHealth;
import static co.uk.zerod.domain.TableName.tableName;
import static co.uk.zerod.test.Condition.otherThan;
import static co.uk.zerod.test.Random.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public abstract class SqlAgentDaoTestBase {

    private Clock clock = new Clock();

    private SqlAgentDao dao = new SqlAgentDao(tableName("zd_agent"), getDataSource(), clock);

    @Test
    public void shouldFindNoAgentsInEmptyDb() {
        assertThat(dao.findAgent(agentId("agent1")).isPresent(), equalTo(false)); // todo: add Optional matcher
        assertThat(dao.findLiveAgents(), is(empty()));
        assertThat(dao.findDeadAgents(), is(empty()));
        assertThat(dao.findStaleAgents(clock.now()), is(empty()));
    }

    @Test
    public void shouldBeAbleToRegisterAnAgent() {
        AgentId agentId = agentId("agent1");
        Health health = randomHealth();

        // When
        ZonedDateTime timeBeforeRegistration = clock.now();
        dao.registerAgentHealth(agentId, health);
        ZonedDateTime timeAfterRegistration = clock.now();

        // Then
        Optional<Agent> foundAgent = dao.findAgent(agentId);
        assertThat(foundAgent.isPresent(), equalTo(true)); // todo: add Optional matcher
        assertThat(foundAgent.get(), equalTo(new Agent(agentId, health, foundAgent.get().lastUpdatedAt)));

        // todo: add ZonedDateTime matcher
        ZonedDateTime storedUpdateTime = foundAgent.get().lastUpdatedAt;
        assertThat(storedUpdateTime.isEqual(timeBeforeRegistration) || storedUpdateTime.isAfter(timeBeforeRegistration), equalTo(true));
        assertThat(storedUpdateTime.isEqual(timeAfterRegistration) || storedUpdateTime.isBefore(timeAfterRegistration), equalTo(true));
    }

    @Test
    public void shouldBeAbleToFindLiveAgent() {
        AgentId agentId = agentId("agent1");

        // When
        Health health = randomLiveHealth();
        dao.registerAgentHealth(agentId, health);

        // Then
        List<Agent> liveAgents = newArrayList(dao.findLiveAgents());
        assertThat(liveAgents, hasSize(1));
        Agent foundAgent = liveAgents.get(0);
        assertThat(foundAgent, equalTo(new Agent(agentId, health, foundAgent.lastUpdatedAt)));

        Set<Agent> deadAgents = dao.findDeadAgents();
        assertThat(deadAgents, is(empty()));
    }

    @Test
    public void shouldBeAbleToFindDeadAgent() {
        AgentId agentId = agentId("agent1");

        // When
        Health health = noHealth();
        dao.registerAgentHealth(agentId, health);

        // Then
        Set<Agent> liveAgents = dao.findLiveAgents();
        assertThat(liveAgents, is(empty()));

        List<Agent> deadAgents = newArrayList(dao.findDeadAgents());
        assertThat(deadAgents, hasSize(1));
        Agent foundAgent = deadAgents.get(0);
        assertThat(foundAgent, equalTo(new Agent(agentId, health, foundAgent.lastUpdatedAt)));
    }

    @Test
    public void shouldUpdateHealthIfNotUpdatedSinceDate() {
        AgentId agentId = agentId("agent1");
        dao.registerAgentHealth(agentId, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentId).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentId, originalAgent.health, newHealth, originalAgent.lastUpdatedAt);

        // Then
        Agent updatedAgent = dao.findAgent(agentId).get();
        assertThat(updatedAgent.lastUpdatedAt.isAfter(originalAgent.lastUpdatedAt), is(true));
        assertThat(updatedAgent.health, equalTo(newHealth));

        assertThat(updatedAgent.id, equalTo(agentId));
    }

    @Test
    public void shouldUpdateHealthIfNotUpdatedSinceDate2() {
        AgentId agentId = agentId("agent1");
        dao.registerAgentHealth(agentId, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentId).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentId, originalAgent.health, newHealth, originalAgent.lastUpdatedAt.plusSeconds(randomInt(1, 100)));

        // Then
        Agent updatedAgent = dao.findAgent(agentId).get();
        assertThat(updatedAgent.health, equalTo(newHealth));
        assertThat(updatedAgent.lastUpdatedAt.isAfter(originalAgent.lastUpdatedAt), is(true));

        assertThat(updatedAgent.id, equalTo(agentId));
    }

    @Test
    public void shouldNotUpdateHealthIfUpdatedSinceDate() {
        AgentId agentId = agentId("agent1");
        dao.registerAgentHealth(agentId, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentId).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentId, originalAgent.health, newHealth, originalAgent.lastUpdatedAt.minusSeconds(randomInt(1, 100)));

        // Then
        Agent updatedAgent = dao.findAgent(agentId).get();
        assertThat(updatedAgent.health, equalTo(originalAgent.health));
        assertThat(updatedAgent.lastUpdatedAt.isEqual(originalAgent.lastUpdatedAt), is(true));

        assertThat(updatedAgent.id, equalTo(agentId));
    }

    @Test
    public void shouldNotUpdateHealthIfExpetedHealthDoesntMatch() {
        AgentId agentId = agentId("agent1");
        dao.registerAgentHealth(agentId, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentId).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentId, randomHealth(otherThan(originalAgent.health)), newHealth, originalAgent.lastUpdatedAt);

        // Then
        Agent updatedAgent = dao.findAgent(agentId).get();
        assertThat(updatedAgent.health, equalTo(originalAgent.health));
        assertThat(updatedAgent.lastUpdatedAt.isEqual(originalAgent.lastUpdatedAt), is(true));

        assertThat(updatedAgent.id, equalTo(agentId));
    }

    @Test
    public void shouldConsiderAgentToBeStale() {
        AgentId agentId = agentId("agent1");
        dao.registerAgentHealth(agentId, randomHealth());
        Agent storedAgent = dao.findAgent(agentId).get();

        // When & Then
        assertThat(dao.findStaleAgents(storedAgent.lastUpdatedAt.minusSeconds(1)).stream().map(Agent::id).collect(toSet()), is(empty()));
        assertThat(dao.findStaleAgents(storedAgent.lastUpdatedAt).stream().map(Agent::id).collect(toSet()), equalTo(newHashSet(agentId)));
        assertThat(dao.findStaleAgents(storedAgent.lastUpdatedAt.plusSeconds(1)).stream().map(Agent::id).collect(toSet()), equalTo(newHashSet(agentId)));
    }

    @Test
    public void shouldFindStaleAgents() {
        AgentId agentId1 = agentId("agent1");
        AgentId agentId2 = agentId("agent2");
        AgentId agentId3 = agentId("agent3");
        AgentId agentId4 = agentId("agent4");

        Health deadlyHealth = noHealth();
        Health liveHealth = randomLiveHealth();

        // todo: add updated and not updated agents as well
        dao.registerAgentHealth(agentId1, deadlyHealth);
        waitForMs(10);
        dao.registerAgentHealth(agentId2, liveHealth);
        waitForMs(10);
        ZonedDateTime cutoutTime = clock.now();
        waitForMs(10);
        dao.registerAgentHealth(agentId3, deadlyHealth);
        waitForMs(10);
        dao.registerAgentHealth(agentId4, liveHealth);

        // When
        Set<Agent> staleAgents = dao.findStaleAgents(cutoutTime);

        // Then
        assertThat(staleAgents.stream().map(Agent::id).collect(toSet()), equalTo(newHashSet(agentId1, agentId2)));
    }


    private void waitForMs(long durationInMs) {
        try {
            Thread.sleep(durationInMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    protected abstract DataSource getDataSource();
}
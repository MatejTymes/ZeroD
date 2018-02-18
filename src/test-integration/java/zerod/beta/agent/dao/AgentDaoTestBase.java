package zerod.beta.agent.dao;

import org.junit.Test;
import zerod.beta.agent.domain.Agent;
import zerod.beta.agent.domain.AgentId;
import zerod.beta.agent.domain.Health;
import zerod.common.Clock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static zerod.beta.agent.domain.Health.noHealth;
import static zerod.test.Condition.otherThan;
import static zerod.test.Random.*;
import static zerod.test.matcher.OptionalMatcher.isNotPresent;
import static zerod.test.matcher.OptionalMatcher.isPresent;

public abstract class AgentDaoTestBase {

    private Clock clock = new Clock();

    private AgentDao dao = getDao();

    @Test
    public void shouldFindNoAgentsInEmptyDb() {
        assertThat(dao.findAgent(randomAgentId()), isNotPresent());
        assertThat(dao.findLiveAgents(), is(empty()));
        assertThat(dao.findDeadAgents(), is(empty()));
        assertThat(dao.findStaleLiveAgents(clock.now()), is(empty()));
    }

    @Test
    public void shouldBeAbleToRegisterAnAgent() {
        AgentId agentId = randomAgentId();
        Health health = randomHealth();

        // When
        ZonedDateTime timeBeforeRegistration = clock.now();
        dao.registerAgentHealth(agentId, health);
        ZonedDateTime timeAfterRegistration = clock.now();

        // Then
        Optional<Agent> foundAgent = dao.findAgent(agentId);
        assertThat(foundAgent, isPresent());
        assertThat(foundAgent.get(), equalTo(new Agent(agentId, health, foundAgent.get().lastUpdatedAt)));

        // todo: add ZonedDateTime matcher
        ZonedDateTime storedUpdateTime = foundAgent.get().lastUpdatedAt;
        assertThat(storedUpdateTime.isEqual(timeBeforeRegistration) || storedUpdateTime.isAfter(timeBeforeRegistration), equalTo(true));
        assertThat(storedUpdateTime.isEqual(timeAfterRegistration) || storedUpdateTime.isBefore(timeAfterRegistration), equalTo(true));
    }

    @Test
    public void shouldBeAbleToFindLiveAgent() {
        AgentId agentId = randomAgentId();

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
        AgentId agentId = randomAgentId();

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
        AgentId agentId = randomAgentId();
        Health initialHealth = randomLiveHealth();
        dao.registerAgentHealth(agentId, initialHealth);
        Agent originalAgent = dao.findAgent(agentId).get();

        Health newHealth = randomHealth(otherThan(initialHealth));
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
        AgentId agentId = randomAgentId();
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
        AgentId agentId = randomAgentId();
        Health initialHealth = randomLiveHealth();
        dao.registerAgentHealth(agentId, initialHealth);
        Agent originalAgent = dao.findAgent(agentId).get();

        Health newHealth = randomHealth(otherThan(initialHealth));
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
        AgentId agentId = randomAgentId();
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
    public void shouldEvaluateAgentsStalenessIfItAlive() {
        AgentId agentId = randomAgentId();
        dao.registerAgentHealth(agentId, randomHealth());
        Agent storedAgent = dao.findAgent(agentId).get();

        // When & Then
        assertThat(dao.findStaleLiveAgents(storedAgent.lastUpdatedAt.minusSeconds(1)), is(empty()));
        assertThat(dao.findStaleLiveAgents(storedAgent.lastUpdatedAt).stream().map(Agent::id).collect(toSet()), equalTo(newHashSet(agentId)));
        assertThat(dao.findStaleLiveAgents(storedAgent.lastUpdatedAt.plusSeconds(1)).stream().map(Agent::id).collect(toSet()), equalTo(newHashSet(agentId)));
    }

    @Test
    public void shouldNotEvaluateAgentsStalenessIfItDead() {
        AgentId agentId = randomAgentId();
        dao.registerAgentHealth(agentId, noHealth());
        Agent storedAgent = dao.findAgent(agentId).get();

        // When & Then
        assertThat(dao.findStaleLiveAgents(storedAgent.lastUpdatedAt.minusSeconds(1)), is(empty()));
        assertThat(dao.findStaleLiveAgents(storedAgent.lastUpdatedAt), is(empty()));
        assertThat(dao.findStaleLiveAgents(storedAgent.lastUpdatedAt.plusSeconds(1)), is(empty()));
    }

    @Test
    public void shouldFindStaleAgents() {
        AgentId agentId1 = randomAgentId();
        AgentId agentId2 = randomAgentId(otherThan(agentId1));
        AgentId agentId3 = randomAgentId(otherThan(agentId1, agentId2));
        AgentId agentId4 = randomAgentId(otherThan(agentId1, agentId2, agentId3));
        AgentId agentId5 = randomAgentId(otherThan(agentId1, agentId2, agentId3, agentId4));
        AgentId agentId6 = randomAgentId(otherThan(agentId1, agentId2, agentId3, agentId4, agentId5));

        Health deadlyHealth = noHealth();
        Health liveHealth = randomLiveHealth();
        Health otherLiveHealth = randomLiveHealth(otherThan(liveHealth));

        dao.registerAgentHealth(agentId1, liveHealth);
        dao.registerAgentHealth(agentId3, liveHealth);
        dao.registerAgentHealth(agentId6, liveHealth);
        waitForMs(10);
        dao.registerAgentHealth(agentId2, deadlyHealth);
        waitForMs(10);
        dao.updateAgentsHealth(agentId3, liveHealth, otherLiveHealth, clock.now());
        waitForMs(10);
        ZonedDateTime cutoutTime = clock.now();
        waitForMs(10);
        dao.registerAgentHealth(agentId4, liveHealth);
        waitForMs(10);
        dao.registerAgentHealth(agentId5, deadlyHealth);
        waitForMs(10);
        dao.updateAgentsHealth(agentId6, liveHealth, otherLiveHealth, clock.now());

        // When
        Set<Agent> staleAgents = dao.findStaleLiveAgents(cutoutTime);

        // Then
        assertThat(staleAgents.stream().map(Agent::id).collect(toSet()), equalTo(newHashSet(agentId1, agentId3)));
    }


    private void waitForMs(long durationInMs) {
        try {
            Thread.sleep(durationInMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    protected abstract AgentDao getDao();
}
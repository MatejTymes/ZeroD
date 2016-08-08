package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.domain.Agent;
import co.uk.zerod.domain.AgentName;
import co.uk.zerod.domain.Health;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static co.uk.zerod.domain.AgentName.agentName;
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

    private SqlAgentDao dao = new SqlAgentDao(tableName("agent"), getDataSource(), clock);

    @Test
    public void shouldFindNoAgentsInEmptyDb() {
        assertThat(dao.findAgent(agentName("agent1")).isPresent(), equalTo(false)); // todo: add Optional matcher
        assertThat(dao.findLiveAgents(), is(empty()));
        assertThat(dao.findDeadAgents(), is(empty()));
        assertThat(dao.findStaleAgents(clock.now()), is(empty()));
    }

    @Test
    public void shouldBeAbleToRegisterAnAgent() {
        AgentName agentName = agentName("agent1");
        Health health = randomHealth();

        // When
        ZonedDateTime timeBeforeRegistration = clock.now();
        dao.registerAgentHealth(agentName, health);
        ZonedDateTime timeAfterRegistration = clock.now();

        // Then
        Optional<Agent> foundAgent = dao.findAgent(agentName);
        assertThat(foundAgent.isPresent(), equalTo(true)); // todo: add Optional matcher
        assertThat(foundAgent.get(), equalTo(new Agent(agentName, health, foundAgent.get().lastUpdatedAt)));

        // todo: add ZonedDateTime matcher
        ZonedDateTime storedUpdateTime = foundAgent.get().lastUpdatedAt;
        assertThat(storedUpdateTime.isEqual(timeBeforeRegistration) || storedUpdateTime.isAfter(timeBeforeRegistration), equalTo(true));
        assertThat(storedUpdateTime.isEqual(timeAfterRegistration) || storedUpdateTime.isBefore(timeAfterRegistration), equalTo(true));
    }

    @Test
    public void shouldBeAbleToFindLiveAgent() {
        AgentName agentName = agentName("agent1");

        // When
        Health health = randomLiveHealth();
        dao.registerAgentHealth(agentName, health);

        // Then
        List<Agent> liveAgents = newArrayList(dao.findLiveAgents());
        assertThat(liveAgents, hasSize(1));
        Agent foundAgent = liveAgents.get(0);
        assertThat(foundAgent, equalTo(new Agent(agentName, health, foundAgent.lastUpdatedAt)));

        Set<Agent> deadAgents = dao.findDeadAgents();
        assertThat(deadAgents, is(empty()));
    }

    @Test
    public void shouldBeAbleToFindDeadAgent() {
        AgentName agentName = agentName("agent1");

        // When
        Health health = noHealth();
        dao.registerAgentHealth(agentName, health);

        // Then
        Set<Agent> liveAgents = dao.findLiveAgents();
        assertThat(liveAgents, is(empty()));

        List<Agent> deadAgents = newArrayList(dao.findDeadAgents());
        assertThat(deadAgents, hasSize(1));
        Agent foundAgent = deadAgents.get(0);
        assertThat(foundAgent, equalTo(new Agent(agentName, health, foundAgent.lastUpdatedAt)));
    }

    @Test
    public void shouldUpdateHealthIfNotUpdatedSinceDate() {
        AgentName agentName = agentName("agent1");
        dao.registerAgentHealth(agentName, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentName).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentName, originalAgent.health, newHealth, originalAgent.lastUpdatedAt);

        // Then
        Agent updatedAgent = dao.findAgent(agentName).get();
        assertThat(updatedAgent.lastUpdatedAt.isAfter(originalAgent.lastUpdatedAt), is(true));
        assertThat(updatedAgent.health, equalTo(newHealth));

        assertThat(updatedAgent.name, equalTo(agentName));
    }

    @Test
    public void shouldUpdateHealthIfNotUpdatedSinceDate2() {
        AgentName agentName = agentName("agent1");
        dao.registerAgentHealth(agentName, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentName).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentName, originalAgent.health, newHealth, originalAgent.lastUpdatedAt.plusSeconds(randomInt(1, 100)));

        // Then
        Agent updatedAgent = dao.findAgent(agentName).get();
        assertThat(updatedAgent.health, equalTo(newHealth));
        assertThat(updatedAgent.lastUpdatedAt.isAfter(originalAgent.lastUpdatedAt), is(true));

        assertThat(updatedAgent.name, equalTo(agentName));
    }

    @Test
    public void shouldNotUpdateHealthIfUpdatedSinceDate() {
        AgentName agentName = agentName("agent1");
        dao.registerAgentHealth(agentName, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentName).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentName, originalAgent.health, newHealth, originalAgent.lastUpdatedAt.minusSeconds(randomInt(1, 100)));

        // Then
        Agent updatedAgent = dao.findAgent(agentName).get();
        assertThat(updatedAgent.health, equalTo(originalAgent.health));
        assertThat(updatedAgent.lastUpdatedAt.isEqual(originalAgent.lastUpdatedAt), is(true));

        assertThat(updatedAgent.name, equalTo(agentName));
    }

    @Test
    public void shouldNotUpdateHealthIfExpetedHealthDoesntMatch() {
        AgentName agentName = agentName("agent1");
        dao.registerAgentHealth(agentName, randomLiveHealth());
        Agent originalAgent = dao.findAgent(agentName).get();

        Health newHealth = randomHealth(otherThan(randomLiveHealth()));
        waitForMs(10);

        // When
        dao.updateAgentsHealth(agentName, randomHealth(otherThan(originalAgent.health)), newHealth, originalAgent.lastUpdatedAt);

        // Then
        Agent updatedAgent = dao.findAgent(agentName).get();
        assertThat(updatedAgent.health, equalTo(originalAgent.health));
        assertThat(updatedAgent.lastUpdatedAt.isEqual(originalAgent.lastUpdatedAt), is(true));

        assertThat(updatedAgent.name, equalTo(agentName));
    }

    @Test
    public void shouldConsiderAgentToBeStale() {
        AgentName agentName = agentName("agent1");
        dao.registerAgentHealth(agentName, randomHealth());
        Agent storedAgent = dao.findAgent(agentName).get();

        // When & Then
        assertThat(dao.findStaleAgents(storedAgent.lastUpdatedAt.minusSeconds(1)).stream().map(Agent::name).collect(toSet()), is(empty()));
        assertThat(dao.findStaleAgents(storedAgent.lastUpdatedAt).stream().map(Agent::name).collect(toSet()), equalTo(newHashSet(agentName)));
        assertThat(dao.findStaleAgents(storedAgent.lastUpdatedAt.plusSeconds(1)).stream().map(Agent::name).collect(toSet()), equalTo(newHashSet(agentName)));
    }

    @Test
    public void shouldFindStaleAgents() {
        AgentName agentName1 = agentName("agent1");
        AgentName agentName2 = agentName("agent2");
        AgentName agentName3 = agentName("agent3");
        AgentName agentName4 = agentName("agent4");

        Health deadlyHealth = noHealth();
        Health liveHealth = randomLiveHealth();

        // todo: add updated and not updated agents as well
        dao.registerAgentHealth(agentName1, deadlyHealth);
        waitForMs(10);
        dao.registerAgentHealth(agentName2, liveHealth);
        waitForMs(10);
        ZonedDateTime cutoutTime = clock.now();
        waitForMs(10);
        dao.registerAgentHealth(agentName3, deadlyHealth);
        waitForMs(10);
        dao.registerAgentHealth(agentName4, liveHealth);

        // When
        Set<Agent> staleAgents = dao.findStaleAgents(cutoutTime);

        // Then
        assertThat(staleAgents.stream().map(Agent::name).collect(toSet()), equalTo(newHashSet(agentName1, agentName2)));
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
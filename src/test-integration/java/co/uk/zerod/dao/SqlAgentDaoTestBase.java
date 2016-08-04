package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.domain.AgentName;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.Set;

import static co.uk.zerod.domain.AgentName.agentName;
import static co.uk.zerod.domain.TableName.tableName;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public abstract class SqlAgentDaoTestBase {

    private Clock clock = new Clock();

    private SqlAgentDao dao = new SqlAgentDao(tableName("agent"), getDataSource(), clock);

    @Test
    public void shouldNotFindLiveAgentsInEmptyDb() {
        assertThat(dao.findAgentsAliveSince(clock.now()), is(empty()));
    }

    @Test
    public void shouldFindLiveAgents() {
        AgentName agentName = agentName("agent1");

        ZonedDateTime timeBeforeHeartBeat = clock.now();
        waitForMs(10);
        dao.registerHeartBeatFor(agentName);
        waitForMs(10);

        // When
        Set<AgentName> liveAgents = dao.findAgentsAliveSince(timeBeforeHeartBeat);

        // Then
        assertThat(liveAgents, equalTo(newHashSet(agentName)));
    }

    @Test
    public void shouldNotFindDeadAgents() {
        AgentName agentName = agentName("agent1");

        dao.registerHeartBeatFor(agentName);
        waitForMs(10);
        ZonedDateTime timeAfterHeartBeat = clock.now();

        // When
        Set<AgentName> liveAgents = dao.findAgentsAliveSince(timeAfterHeartBeat);

        // Then
        assertThat(liveAgents, is(empty()));

    }

    @Test
    public void shouldBeAbleToRefreshHeartBeatTime() {
        AgentName agentName = agentName("agent1");

        dao.registerHeartBeatFor(agentName);
        waitForMs(10);
        ZonedDateTime timeBeforeLastHeartBeat = clock.now();
        waitForMs(10);
        dao.registerHeartBeatFor(agentName);
        waitForMs(10);

        // When
        Set<AgentName> liveAgents = dao.findAgentsAliveSince(timeBeforeLastHeartBeat);

        // Then
        assertThat(liveAgents, equalTo(newHashSet(agentName)));
    }

    @Test
    public void shouldFindAgentsAliveSince() {
        dao.registerHeartBeatFor(agentName("oldAgent"));
        waitForMs(10);

        AgentName agentName1 = agentName("agent1");
        AgentName agentName2 = agentName("agent2");

        ZonedDateTime cutoutTime = clock.now();
        dao.registerHeartBeatFor(agentName1);
        waitForMs(10);
        dao.registerHeartBeatFor(agentName2);
        waitForMs(10);

        Set<AgentName> liveAgents = dao.findAgentsAliveSince(cutoutTime);

        assertThat(liveAgents, equalTo(newHashSet(agentName1, agentName2)));
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
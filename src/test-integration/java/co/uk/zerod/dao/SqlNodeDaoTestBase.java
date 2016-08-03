package co.uk.zerod.dao;

import co.uk.zerod.common.Clock;
import co.uk.zerod.domain.NodeName;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.Set;

import static co.uk.zerod.domain.NodeName.nodeName;
import static co.uk.zerod.domain.TableName.tableName;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public abstract class SqlNodeDaoTestBase {

    private Clock clock = new Clock();

    private SqlNodeDao dao = new SqlNodeDao(tableName("node"), getDataSource(), clock);

    @Test
    public void shouldNotFindLiveNodesInEmptyDb() {
        assertThat(dao.findNodesAliveSince(clock.now()), is(empty()));
    }

    @Test
    public void shouldFindLiveNode() {
        NodeName nodeName = nodeName("node1");

        ZonedDateTime timeBeforeHeartBeat = clock.now();
        waitForMs(10);
        dao.registerHeartBeatFor(nodeName);
        waitForMs(10);

        // When
        Set<NodeName> liveNodes = dao.findNodesAliveSince(timeBeforeHeartBeat);

        // Then
        assertThat(liveNodes, equalTo(newHashSet(nodeName)));
    }

    @Test
    public void shouldNotFindDeadNode() {
        NodeName nodeName = nodeName("node1");

        dao.registerHeartBeatFor(nodeName);
        waitForMs(10);
        ZonedDateTime timeAfterHeartBeat = clock.now();

        // When
        Set<NodeName> liveNodes = dao.findNodesAliveSince(timeAfterHeartBeat);

        // Then
        assertThat(liveNodes, is(empty()));

    }

    @Test
    public void shouldBeAbleToRefreshHeartBeatTime() {
        NodeName nodeName = nodeName("node1");

        dao.registerHeartBeatFor(nodeName);
        waitForMs(10);
        ZonedDateTime timeBeforeLastHeartBeat = clock.now();
        waitForMs(10);
        dao.registerHeartBeatFor(nodeName);
        waitForMs(10);

        // When
        Set<NodeName> liveNodes = dao.findNodesAliveSince(timeBeforeLastHeartBeat);

        // Then
        assertThat(liveNodes, equalTo(newHashSet(nodeName)));
    }

    @Test
    public void shouldFindAliveNodesSince() {
        dao.registerHeartBeatFor(nodeName("oldNode"));
        waitForMs(10);

        ZonedDateTime cutoutTime = clock.now();
        dao.registerHeartBeatFor(nodeName("node1"));
        waitForMs(10);
        dao.registerHeartBeatFor(nodeName("node2"));
        waitForMs(10);

        Set<NodeName> liveNodes = dao.findNodesAliveSince(cutoutTime);

        assertThat(liveNodes, equalTo(newHashSet(nodeName("node1"), nodeName("node2"))));
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
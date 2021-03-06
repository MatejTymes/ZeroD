package zerod.beta.agent.dao;

import javafixes.concurrency.Runner;
import org.junit.Test;
import zerod.beta.agent.domain.Agent;
import zerod.beta.agent.domain.AgentId;
import zerod.beta.agent.domain.Health;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;

import static javafixes.concurrency.Runner.runner;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static zerod.test.Condition.otherThan;
import static zerod.test.Random.randomAgentId;
import static zerod.test.Random.randomHealth;
import static zerod.test.matcher.OptionalMatcher.isPresent;

public abstract class AgentDaoConcurrentTestBase {

    private AgentDao dao = getDao();

    @Test
    public void shouldNotFailIfHealthForTheSameAgentIsRegisteredConcurrently() {
        int attemptsCount = 10;
        int concurrentThreadCount = 30;

        Runner runner = runner(concurrentThreadCount);

        try {
            for (int attempt = 1; attempt <= attemptsCount; attempt++) {
                AgentId agentId = randomAgentId();
                Health health = randomHealth();

                // When
                CyclicBarrier synchronizedStartBarrier = new CyclicBarrier(concurrentThreadCount);
                for (int i = 0; i < concurrentThreadCount; i++) {
                    runner.runTask(() -> {
                        synchronizedStartBarrier.await();

                        dao.registerAgentHealth(agentId, health);
                    });
                }
                runner.waitTillDone();

                // Then
                assertThat(
                        attempt + ". attempt - there should be no failures",

                        runner.failedCount(), is(0)
                );
                Optional<Agent> foundAgent = dao.findAgent(agentId);
                assertThat(foundAgent, isPresent());
                assertThat(foundAgent.get().health, equalTo(health));
            }
        } finally {
            runner.shutdownNow();
        }
    }

    @Test
    public void shouldNotFailIfHealthForTheSameAgentIsUpdatedConcurrently() {
        int attemptsCount = 10;
        int concurrentThreadCount = 30;

        Runner runner = runner(concurrentThreadCount);

        try {
            for (int attempt = 1; attempt <= attemptsCount; attempt++) {
                AgentId agentId = randomAgentId();
                Health oldHealth = randomHealth();
                dao.registerAgentHealth(agentId, oldHealth);

                ZonedDateTime lastUpdatedAt = dao.findAgent(agentId).get().lastUpdatedAt;
                Health newHealth = randomHealth(otherThan(oldHealth));

                // When
                CyclicBarrier synchronizedStartBarrier = new CyclicBarrier(concurrentThreadCount);
                for (int i = 0; i < concurrentThreadCount; i++) {
                    runner.runTask(() -> {
                        synchronizedStartBarrier.await();

                        dao.updateAgentsHealth(agentId, oldHealth, newHealth, lastUpdatedAt);
                    });
                }
                runner.waitTillDone();

                // Then
                assertThat(
                        attempt + ". attempt - there should be no failures",

                        runner.failedCount(), is(0)
                );
                Optional<Agent> foundAgent = dao.findAgent(agentId);
                assertThat(foundAgent, isPresent());
                assertThat(foundAgent.get().health, equalTo(newHealth));
            }
        } finally {
            runner.shutdownNow();
        }
    }

    @Test
    public void shouldNotFailIfHealthForTheSameAgentIsRegisteredAndUpdatedConcurrently() {
        int attemptsCount = 10;

        Runner runner = runner(2);

        try {
            for (int attempt = 1; attempt <= attemptsCount; attempt++) {
                AgentId agentId = randomAgentId();
                Health oldHealth = randomHealth();
                dao.registerAgentHealth(agentId, oldHealth);

                ZonedDateTime lastUpdatedAt = dao.findAgent(agentId).get().lastUpdatedAt;
                Health updateHealth = randomHealth(otherThan(oldHealth));
                Health newRegistrationHealth = randomHealth(otherThan(oldHealth, updateHealth));

                // When
                CyclicBarrier synchronizedStartBarrier = new CyclicBarrier(2);

                runner.runTask(() -> {
                    synchronizedStartBarrier.await();
                    dao.registerAgentHealth(agentId, newRegistrationHealth);
                });
                runner.runTask(() -> {
                    synchronizedStartBarrier.await();
                    dao.updateAgentsHealth(agentId, oldHealth, updateHealth, lastUpdatedAt);
                });

                runner.waitTillDone();

                // Then
                assertThat(
                        attempt + ". attempt - there should be no failures",

                        runner.failedCount(), is(0)
                );
                Optional<Agent> foundAgent = dao.findAgent(agentId);
                assertThat(foundAgent, isPresent());
                assertThat(foundAgent.get().health, equalTo(newRegistrationHealth));
            }
        } finally {
            runner.shutdownNow();
        }
    }

    protected abstract AgentDao getDao();
}
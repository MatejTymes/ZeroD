package co.uk.zerod.dao;

import co.uk.zerod.domain.Agent;
import co.uk.zerod.domain.AgentId;
import co.uk.zerod.domain.Health;
import mtymes.javafixes.concurrency.Runner;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;

import static co.uk.zerod.test.Condition.otherThan;
import static co.uk.zerod.test.Random.randomAgentId;
import static co.uk.zerod.test.Random.randomHealth;
import static co.uk.zerod.test.matcher.OptionalMatcher.isPresent;
import static mtymes.javafixes.concurrency.Runner.runner;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
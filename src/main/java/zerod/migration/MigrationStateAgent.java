package zerod.migration;

import org.slf4j.Logger;
import zerod.migration.dao.MigrationStateDao;
import zerod.migration.domain.MigrationConfig;
import zerod.migration.domain.MigrationId;
import zerod.migration.exception.UnknownMigrationIdException;
import zerod.state.CoreReadWriteHelper;
import zerod.state.ReadWriteHelper;
import zerod.state.StateTransitioner;
import zerod.state.SwitchableReadWriteHelper;
import zerod.state.domain.ReadWriteState;

import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

// todo: mtymes - test this
public class MigrationStateAgent {

    private static final Logger LOG = getLogger(MigrationStateAgent.class);

    private final MigrationStateDao stateDao;
    private final Map<MigrationId, SwitchableReadWriteHelper> helpers;

    public MigrationStateAgent(
            MigrationStateDao stateDao,
            Map<MigrationId, MigrationConfig> configs
    ) {
        this.stateDao = stateDao;
        configs.forEach(
                (migrationId, config) -> stateDao.registerInitialState(migrationId, config.initialState)
        );
        helpers = configs
                .entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> {
                            StateTransitioner stateTransitioner = entry.getValue().stateTransitioner;
                            ReadWriteState currentState = stateDao.getState(entry.getKey());
                            return new CoreReadWriteHelper(stateTransitioner, currentState);
                        }));
    }

    public void refreshStates() {
        helpers.forEach((migrationId, helper) -> {
            ReadWriteState newState = null;
            try {
                newState = stateDao.getState(migrationId);
                if (helper.getCurrentState() != newState && helper.getTransitionToState() != newState) {
                    helper.switchState(newState);
                }
            } catch (Exception e) {
                if (newState == null) {
                    LOG.error(format("unable to load new ReadWriteState for MigrationId '%s'", migrationId), e);
                } else {
                    LOG.error(format("unable to switch ReadWriteState for MigrationId '%s' to '%s'. Current state = '%s', transitional state = '%s'", migrationId, newState, helper.getCurrentState(), helper.getTransitionToState()), e);
                }
            }
        });
    }

    public ReadWriteHelper readWriteHelper(MigrationId migrationId) throws UnknownMigrationIdException {
        ReadWriteHelper helper = helpers.get(migrationId);
        if (helper == null) {
            throw new UnknownMigrationIdException(format("No ReadWriteGuide registered for MigrationId ''%s", migrationId));
        }
        return helper;
    }
}

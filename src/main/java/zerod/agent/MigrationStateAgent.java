package zerod.agent;

import org.slf4j.Logger;
import zerod.dao.MigrationStateDao;
import zerod.domain.MigrationConfig;
import zerod.domain.MigrationId;
import zerod.exception.UnknownMigrationIdException;
import zerod.guide.ReadWriteGuide;
import zerod.guide.ReadWriteHelper;
import zerod.guide.SwitchableReadWriteGuide;
import zerod.guide.TransitionalReadWriteGuide;
import zerod.state.ReadWriteState;
import zerod.state.StateTransitioner;

import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

// todo: mtymes - test this
public class MigrationStateAgent {

    private static final Logger LOG = getLogger(MigrationStateAgent.class);

    private final MigrationStateDao stateDao;
    private final Map<MigrationId, SwitchableReadWriteGuide> guides;

    public MigrationStateAgent(
            MigrationStateDao stateDao,
            Map<MigrationId, MigrationConfig> configs
    ) {
        this.stateDao = stateDao;
        configs.forEach(
                (migrationId, config) -> stateDao.registerInitialState(migrationId, config.initialState)
        );
        guides = configs
                .entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> {
                            StateTransitioner stateTransitioner = entry.getValue().stateTransitioner;
                            ReadWriteState currentState = stateDao.getState(entry.getKey());
                            return new TransitionalReadWriteGuide(stateTransitioner, currentState);
                        }));
    }

    public void refreshStates() {
        guides.forEach((migrationId, guide) -> {
            ReadWriteState newState = null;
            try {
                newState = stateDao.getState(migrationId);
                if (guide.getCurrentState() != newState && guide.getTransitionToState() != newState) {
                    guide.switchState(newState);
                }
            } catch (Exception e) {
                if (newState == null) {
                    LOG.error(format("unable to load new ReadWriteState for MigrationId '%s'", migrationId), e);
                } else {
                    LOG.error(format("unable to switch ReadWriteState for MigrationId '%s' to '%s'. Current state = '%s', transitional state = '%s'", migrationId, newState, guide.getCurrentState(), guide.getTransitionToState()), e);
                }
            }
        });
    }

    public ReadWriteGuide readWriteGuide(MigrationId migrationId) throws UnknownMigrationIdException {
        SwitchableReadWriteGuide guide = guides.get(migrationId);
        if (guide == null) {
            throw new UnknownMigrationIdException(format("No ReadWriteGuide registered for MigrationId ''%s", migrationId));
        }
        return guide;
    }

    public ReadWriteHelper readWriteHelper(MigrationId migrationId) throws UnknownMigrationIdException {
        return new ReadWriteHelper(readWriteGuide(migrationId));
    }
}

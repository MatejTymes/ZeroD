package zerod.copy;

import zerod.copy.dao.CopyProgressDao;
import zerod.copy.dao.CopyProgressMetadataDao;
import zerod.migration.domain.MigrationId;

import java.util.Optional;

// todo: mtymes - test this
public abstract class BaseCopyAgent<Id, SuccessSummary> {

    private final MigrationId migrationId;
    private final CopyProgressMetadataDao progressMetadataDao;
    private final CopyProgressDao<Id, SuccessSummary> progressDao;

    public BaseCopyAgent(MigrationId migrationId, CopyProgressMetadataDao progressMetadataDao, CopyProgressDao<Id, SuccessSummary> progressDao) {
        this.migrationId = migrationId;
        this.progressMetadataDao = progressMetadataDao;
        this.progressDao = progressDao;
    }

    // todo: register this as a rerunable step: in GenericAgent
    // todo: return if work has been done
    public boolean copyNext() {
        if (progressMetadataDao.isProgressPaused(migrationId)) {
            return false;
        }

        Optional<Id> optionalId = progressDao.takeNextProcessableId();
        if (optionalId.isPresent()) {
            Id id = optionalId.get();
            try {
                SuccessSummary successSummary = copy(id);

                progressDao.markAsSucceeded(id, successSummary);
            } catch (Exception e) {
                progressDao.markAsFailed(id, e);
            }
            return true;
        } else {
            return false;
        }
    }

    protected abstract SuccessSummary copy(Id id) throws Exception;
}

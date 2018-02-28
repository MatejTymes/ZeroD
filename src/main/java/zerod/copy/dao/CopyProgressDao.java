package zerod.copy.dao;

import zerod.copy.domain.CopyProgress;
import zerod.copy.domain.CopyState;
import zerod.copy.exception.UnknownIdException;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;


// todo: start using this

// todo: add generic test
// todo: add implementations
public interface CopyProgressDao<Id, SuccessSummary> {

    void registerForCopy(Iterator<Id> ids);

    void retry(Id id) throws UnknownIdException;

    Optional<Id> takeNextProcessableId();

    void markAsSucceeded(Id id, SuccessSummary summary) throws UnknownIdException;

    void markAsFailed(Id id, Exception exception) throws UnknownIdException;

    CopyProgress<Id, SuccessSummary> getProgress(Id id) throws UnknownIdException;

    Map<CopyState, Integer> getObjectsInStateCounts();

    Iterator<Id> idsInState(CopyState status);
}

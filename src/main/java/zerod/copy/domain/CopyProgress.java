package zerod.copy.domain;

import javafixes.object.DataObject;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static javafixes.common.CollectionUtil.newList;

public class CopyProgress<Id, SuccessSummary> extends DataObject {

    public final Id id;
    public final ZonedDateTime created;
    public final ZonedDateTime lastUpdatedAt;
    public final CopyState currentState;
    public final List<CopyAttempt<SuccessSummary>> attempts;

    public CopyProgress(Id id, ZonedDateTime created, ZonedDateTime lastUpdatedAt, CopyState currentState, List<CopyAttempt> attempts) {
        this.id = id;
        this.created = created;
        this.lastUpdatedAt = lastUpdatedAt;
        this.currentState = currentState;
        this.attempts = unmodifiableList(newList(attempts));
    }
}

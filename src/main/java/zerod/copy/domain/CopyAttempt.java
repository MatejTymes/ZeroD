package zerod.copy.domain;

import javafixes.object.DataObject;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public class CopyAttempt<Summary> extends DataObject {

    public final UUID attemptId;
    public final CopyState state;
    public final ZonedDateTime startedAt;
    public final ZonedDateTime lastUpdatedAt;
    public final Optional<Summary> successSummary;
    public final Optional<String> failureMessage;

    public CopyAttempt(UUID attemptId, CopyState state, ZonedDateTime startedAt, ZonedDateTime lastUpdatedAt, Optional<Summary> successSummary, Optional<String> failureMessage) {
        this.attemptId = attemptId;
        this.state = state;
        this.startedAt = startedAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.successSummary = successSummary;
        this.failureMessage = failureMessage;
    }
}

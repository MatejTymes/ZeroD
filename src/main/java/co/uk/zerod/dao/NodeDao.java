package co.uk.zerod.dao;

import co.uk.zerod.domain.NodeName;

import java.time.ZonedDateTime;
import java.util.Set;

public interface NodeDao {

    void registerHeartBeatFor(NodeName nodeName);

    Set<NodeName> findNodesAliveSince(ZonedDateTime activeSince);
}

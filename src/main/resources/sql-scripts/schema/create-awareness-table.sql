CREATE TABLE zd_awareness (

    migration_id VARCHAR(50) NOT NULL,
    agent_id VARCHAR(50) NOT NULL,
    is_aware_of BIT NOT NULL,

    PRIMARY KEY (migration_id, agent_id)
);
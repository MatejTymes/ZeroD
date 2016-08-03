CREATE TABLE node (

    node_name VARCHAR(50) NOT NULL,
    last_heart_beat DATETIME2 NOT NULL,

    PRIMARY KEY (node_name)
);
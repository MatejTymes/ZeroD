CREATE TABLE agent (

    name VARCHAR(50) NOT NULL,
    last_heart_beat DATETIME2 NOT NULL,

    PRIMARY KEY (name)
);
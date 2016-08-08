CREATE TABLE agent (

    name VARCHAR(50) NOT NULL,
    health TINYINT NOT NULL,
    last_updated_at DATETIME2 NOT NULL,

    PRIMARY KEY (name)
);
CREATE TABLE IF NOT EXISTS allocated_ip (
 ip     char PRIMARY KEY,
 name   char NOT NULL
);

CREATE VIEW IF NOT EXISTS most_recent AS
SELECT * FROM allocated_ip ORDER BY ip DESC LIMIT 8192;

CREATE VIEW IF NOT EXISTS latest_ip AS
SELECT ip FROM allocated_ip ORDER BY ip DESC LIMIT 1;

CREATE VIEW IF NOT EXISTS earliest_recent_ip AS
SELECT ip FROM most_recent ORDER BY ip ASC LIMIT 1;


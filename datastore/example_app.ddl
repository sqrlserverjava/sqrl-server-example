CREATE TABLE app_user (id BIGINT NOT NULL AUTO_INCREMENT, username VARCHAR(255), givenName VARCHAR(255), welcomePhrase VARCHAR(255),createdAt DATETIME NOT NULL, PRIMARY KEY (id));
-- ID GENERATOR TABLE
CREATE TABLE app_db_id_gen (name VARCHAR(50) NOT NULL, value DECIMAL(38) NOT NULL, PRIMARY KEY (name));
INSERT INTO app_db_id_gen(name, value) values ('app_user_gen', 0);
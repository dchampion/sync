CREATE SEQUENCE HIBERNATE_SEQUENCE
  START WITH   0
  INCREMENT BY 1
  NOCACHE
  NOCYCLE;

CREATE SCHEMA FRAMEWORK_DEMO AUTHORIZATION sa;

CREATE TABLE FRAMEWORK_DEMO.SHARED_RESPONSE_CACHE
(
  ID      BIGINT NOT NULL,
  UUID    CHAR(36),
  HEADERS VARCHAR(512),
  BODY    CLOB,
  PRIMARY KEY (ID)
);
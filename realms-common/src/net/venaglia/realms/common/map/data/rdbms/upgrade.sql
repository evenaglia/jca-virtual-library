-- 1
CREATE TABLE thing_version (
  version integer NOT NULL,
  instance_id varchar(48) NOT NULL
);

-- 2
INSERT INTO thing_version (version, instance_id) VALUES (0, '{instanceUUID}' );

-- 3
CREATE TABLE thing_props
(
  name varchar(256) NOT NULL,
  value varchar(256) NOT NULL,
  CONSTRAINT thing_props_pkey PRIMARY KEY (name)
);

-- 4
INSERT INTO thing_props DEFAULT VALUES;

-- 5
CREATE TABLE thing_seq
(
  name character varying(48) NOT NULL,
  next bigint NOT NULL DEFAULT 1024,
  CONSTRAINT thing_seq_pkey PRIMARY KEY (name)
);

-- 6
CREATE TABLE thing
(
  thing_id bigint NOT NULL,
  cube_id bigint NULL,
  container_id bigint NULL,
  type varchar(48) NOT NULL,
  x numeric(18,10) NOT NULL,
  y numeric(18,10) NOT NULL,
  z numeric(18,10) NOT NULL,
  properties_length integer NOT NULL,
  properties bytea,
  CONSTRAINT thing_pkey PRIMARY KEY (thing_id)
);

-- 7
CREATE INDEX thing_cube_id_idx ON thing (cube_id);

-- 8
CREATE INDEX thing_container_id_idx ON thing (container_id);

-- 9
CREATE TABLE thing_binary
(
  thing_binary_id bigint NOT NULL,
  reference_count int NOT NULL,
  mimeType varchar(48) NOT NULL,
  metadata varchar(256) NOT NULL,
  sha1 varchar(48) NOT NULL,
  length integer NOT NULL,
  data bytea,
  CONSTRAINT thing_binary_pkey PRIMARY KEY (thing_binary_id),
  CONSTRAINT thing_binary_sha1_length_key UNIQUE (sha1, length, mimetype)
);

-- 10
CREATE TABLE thing_binary_locator
(
  locator_id bigint NOT NULL,
  mimeType varchar(48) NOT NULL,
  thing_binary_id bigint NOT NULL,
  CONSTRAINT thing_binary_locator_pkey PRIMARY KEY (locator_id, mimetype),
  CONSTRAINT thing_binary_locator_fk FOREIGN KEY (thing_binary_id) REFERENCES thing_binary (thing_binary_id)
);

-- 11
CREATE INDEX thing_binary_locator_idx ON thing_binary (thing_binary_id);

-- CREATE DATABASE cft_task_db_replica WITH OWNER=postgres ENCODING='UTF-8' CONNECTION LIMIT=-1;
CREATE ROLE repl_user REPLICATION LOGIN PASSWORD 'repl_password';

GRANT CONNECT ON DATABASE cft_task_db_replica TO repl_user;